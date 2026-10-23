package com.rsec.gestorapp.service;

import com.rsec.gestorapp.model.Dispositivo;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Servicio encargado de la comunicación directa con ADB y utilidades del sistema.
 */
public class ADBService {
    private static final String URL_SCRCPY_ZIP = "https://github.com/Genymobile/scrcpy/releases/download/v2.4/scrcpy-win64-v2.4.zip";

    /**
     * Puertos locales estándar evaluados por orden de prioridad:
     * - 16384: Puerto oficial y principal de MuMu Player 12
     * - 5555: Puerto estándar de BlueStacks, LDPlayer y emuladores AVD
     * - 62001: Puerto primario de NoxPlayer
     * - 21503: Puerto de MEmu Player
     * - 7555: Puerto secundario de MuMu Player legacy
     */
    private static final int[] PUERTOS_EMULADORES = {
            16384, 5555, 62001, 21503, 7555
    };

    /**
     * Busca los dispositivos conectados vía USB, Wi-Fi o Emuladores activos,
     * filtrando automáticamente puertos redundantes para que cada emulador aparezca una sola vez.
     */
    public void buscarDispositivos(Consumer<List<Dispositivo>> callback, Consumer<String> logger) {
        new Thread(() -> {
            List<Dispositivo> listaCruda = new ArrayList<>();
            logger.accept("Conectando y buscando dispositivos...");
            try {
                // 1. Auto-conectar emuladores locales abiertos (sin repetir conexiones)
                autoConectarEmuladoresLocales(logger);

                // 2. Consultar la salida de ADB
                Process proceso = new ProcessBuilder("adb", "devices", "-l").start();
                BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
                String linea;

                while ((linea = lector.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("List of devices")) continue;

                    String[] partes = linea.split("\\s+");
                    if (partes.length < 2) continue;

                    String id = partes[0];
                    String estadoAdb = partes[1]; // device, unauthorized, offline

                    if (!"offline".equalsIgnoreCase(estadoAdb)) {
                        Dispositivo dispositivoDetectado = clasificarDispositivo(id, estadoAdb, partes);
                        listaCruda.add(dispositivoDetectado);
                    }
                }
                proceso.waitFor();

                // 3. FILTRO DE DEDUPLICACIÓN: Evita mostrar el mismo emulador varias veces
                List<Dispositivo> listaFinal = new ArrayList<>();
                java.util.Set<String> modelosEmuladoresRegistrados = new java.util.HashSet<>();

                for (Dispositivo disp : listaCruda) {
                    if (disp.isEsEmulador() && disp.getId().startsWith("127.0.0.1:")) {
                        String claveModelo = disp.getNombreModelo();

                        // Si este modelo de emulador ya fue agregado con un puerto preferente
                        if (modelosEmuladoresRegistrados.contains(claveModelo)) {
                            // Desconectamos el puerto espejo de ADB para no saturar conexiones
                            desconectarPuertoDuplicado(disp.getId());
                        } else {
                            modelosEmuladoresRegistrados.add(claveModelo);
                            listaFinal.add(disp);
                        }
                    } else {
                        // Teléfonos físicos reales por USB pasan siempre directamente
                        listaFinal.add(disp);
                    }
                }

                callback.accept(listaFinal);
                if (listaFinal.isEmpty()) {
                    logger.accept("⚠ No se encontraron dispositivos conectados o autorizados.");
                } else {
                    logger.accept("✅ Dispositivos actualizados: " + listaFinal.size() + " encontrado(s).");
                }
            } catch (Exception e) {
                logger.accept("❌ Error al buscar dispositivos: " + e.getMessage());
                callback.accept(listaCruda);
            }
        }).start();
    }

    /**
     * Prueba los puertos locales de emuladores abiertos mediante sockets de respuesta rápida (80ms).
     */
    private void autoConectarEmuladoresLocales(Consumer<String> logger) {
        for (int puerto : PUERTOS_EMULADORES) {
            try (Socket socketPrueba = new Socket()) {
                socketPrueba.connect(new InetSocketAddress("127.0.0.1", puerto), 80);

                // Si el puerto responde, ejecutamos adb connect
                Process pConnect = new ProcessBuilder("adb", "connect", "127.0.0.1:" + puerto).start();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(pConnect.getInputStream()))) {
                    String respuesta = br.readLine();
                    if (respuesta != null && respuesta.contains("connected to")) {
                        logger.accept("🔌 Emulador local enlazado en puerto " + puerto + ".");
                    }
                }
                pConnect.waitFor();
            } catch (Exception ignored) {
                // Puerto cerrado, avanza al siguiente inmediatamente
            }
        }
    }

    /**
     * Desconecta de forma silenciosa un puerto secundario redundante en ADB.
     */
    private void desconectarPuertoDuplicado(String idDispositivo) {
        try {
            new ProcessBuilder("adb", "disconnect", idDispositivo).start();
        } catch (Exception ignored) {}
    }


    /**
     * Analiza el identificador y los atributos de 'adb devices -l' para determinar
     * si corresponde a un emulador conocido o a un dispositivo físico real.
     */
    private Dispositivo clasificarDispositivo(String id, String estadoAdb, String[] partesLinea) {
        String modelo = "Android";
        String idLower = id.toLowerCase();
        StringBuilder metadatos = new StringBuilder();

        // Extraer etiquetas clave devueltas por ADB (model:, product:, device:)
        for (String parte : partesLinea) {
            metadatos.append(parte.toLowerCase()).append(" ");
            if (parte.startsWith("model:")) {
                modelo = parte.replace("model:", "").replace("_", " ").trim();
            }
        }

        String meta = metadatos.toString();
        boolean esEmulador = false;
        String tipoEmulador = null;

        // ==============================================================
        // 1. REGLAS HEURÍSTICAS DE DETECCIÓN DE EMULADORES POPULARES
        // ==============================================================

        // A. Android Studio AVD Oficial
        if (idLower.startsWith("emulator-") || meta.contains("sdk_gphone") || meta.contains("goldfish") || meta.contains("ranchu")) {
            esEmulador = true;
            tipoEmulador = "Android Studio AVD";
            if (modelo.equalsIgnoreCase("Android") || modelo.toLowerCase().contains("sdk")) {
                modelo = "Google Pixel AVD";
            }
        }
        // B. NoxPlayer (Usa habitualmente puertos 62001, 62025 o el nombre nox)
        else if (idLower.contains(":62001") || idLower.contains(":62025") || meta.contains("nox")) {
            esEmulador = true;
            tipoEmulador = "NoxPlayer";
        }
        // C. MEmu Player (Usa habitualmente el puerto 21503 o huella memu)
        else if (idLower.contains(":21503") || meta.contains("memu")) {
            esEmulador = true;
            tipoEmulador = "MEmu";
        }
        // D. MuMu Player de NetEase (Usa puertos 7555, 16384 o huella mumu)
        else if (idLower.contains(":7555") || idLower.contains(":16384") || meta.contains("mumu") || meta.contains("netease")) {
            esEmulador = true;
            tipoEmulador = "MuMu Player";
        }
        // E. Windows Subsystem for Android - WSA (Puerto 58526 o huella wsa)
        else if (idLower.contains(":58526") || meta.contains("subsystem_for_android") || meta.contains("wsa")) {
            esEmulador = true;
            tipoEmulador = "WSA Windows";
        }
        // F. Genymotion (Usa subred 192.168.56.* o huella genymotion / vbox)
        else if (idLower.startsWith("192.168.56.") || meta.contains("genymotion") || meta.contains("vbox86")) {
            esEmulador = true;
            tipoEmulador = "Genymotion";
        }
        // G. BlueStacks / LDPlayer (Ambos usan habitualmente 127.0.0.1:5555 o localhost:5555)
        else if (idLower.startsWith("127.0.0.1:") || idLower.startsWith("localhost:")) {
            esEmulador = true;
            if (meta.contains("bluestacks") || meta.contains("bst")) {
                tipoEmulador = "BlueStacks";
            } else if (meta.contains("ldplayer") || meta.contains("ld")) {
                tipoEmulador = "LDPlayer";
            } else {
                tipoEmulador = "Emulador Virtual";
            }
        }
        // H. Cualquier otro emulador que reporte cadenas de emulación en sus metadatos
        else if (meta.contains("emulator") || meta.contains("virtualbox") || meta.contains("qemu")) {
            esEmulador = true;
            tipoEmulador = "Emulador";
        }

        String estadoFinal = "device".equalsIgnoreCase(estadoAdb) ? "Conectado" : estadoAdb;
        return new Dispositivo(id, modelo, estadoFinal, esEmulador, tipoEmulador);
    }





    /**
     * Obtiene la lista de aplicaciones instaladas por el usuario (terceros).
     */
    public void listarPaquetes(Dispositivo d, Consumer<List<String>> callback, Consumer<String> logger) {
        new Thread(() -> {
            List<String> paquetes = new ArrayList<>();
            logger.accept("Buscando aplicaciones instaladas...");
            try {
                Process proceso = new ProcessBuilder("adb", "-s", d.getId(), "shell", "pm", "list", "packages", "-3").start();
                BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
                String linea;

                while ((linea = lector.readLine()) != null) {
                    if (linea.startsWith("package:")) {
                        paquetes.add(linea.replace("package:", "").trim());
                    }
                }
                proceso.waitFor();
                callback.accept(paquetes);
            } catch (Exception e) {
                logger.accept("❌ Error al listar paquetes: " + e.getMessage());
                callback.accept(paquetes);
            }
        }).start();
    }

    /**
     * Extrae el icono original de una aplicación desde el APK base en el dispositivo
     * con soporte para Split APKs, nombres alternativos y selección por alta resolución.
     *
     * @param d Dispositivo conectado.
     * @param paquete Nombre del paquete (ej. com.google.android.calendar).
     * @return Icono escalado a 32x32 píxeles o null si no se localiza ninguna imagen PNG.
     */
    /**
     * Extrae el icono original de una aplicación inspeccionando tanto el APK base
     * como sus divisiones (splits de densidad), guardándolo en disco local.
     */
    public Icon extraerIconoApp(Dispositivo d, String paquete) {
        try {
            // 1. Verificar si ya existe en la caché local
            File carpetaCache = new File("cache_iconos");
            if (!carpetaCache.exists()) {
                carpetaCache.mkdirs();
            }

            File archivoIcono = new File(carpetaCache, paquete + ".png");
            if (archivoIcono.exists() && archivoIcono.length() > 0) {
                Image img = ImageIO.read(archivoIcono);
                if (img != null) {
                    return new ImageIcon(img);
                }
            }

            // 2. Obtener TODAS las rutas de instalación de la app (Base + Splits de densidad)
            Process pRuta = new ProcessBuilder("adb", "-s", d.getId(), "shell", "pm", "path", paquete).start();
            BufferedReader lectorRuta = new BufferedReader(new InputStreamReader(pRuta.getInputStream()));
            List<String> todasLasRutas = new ArrayList<>();
            String lineaRuta;

            while ((lineaRuta = lectorRuta.readLine()) != null) {
                lineaRuta = lineaRuta.trim();
                if (lineaRuta.startsWith("package:")) {
                    todasLasRutas.add(lineaRuta.replace("package:", "").trim());
                }
            }
            pRuta.waitFor();

            if (todasLasRutas.isEmpty()) {
                return null;
            }

            // Ordenamos para revisar primero los splits de densidad (xxhdpi, xhdpi) y el base.apk
            todasLasRutas.sort((a, b) -> {
                if (a.contains("hdpi") || a.contains("dpi")) return -1;
                if (b.contains("hdpi") || b.contains("dpi")) return 1;
                if (a.endsWith("base.apk")) return -1;
                return 1;
            });

            // Extraemos la última palabra del paquete para coincidencias personalizadas (ej. "vysor", "rootcheck")
            String[] partesPaquete = paquete.toLowerCase().split("\\.");
            String palabraClaveApp = partesPaquete[partesPaquete.length - 1];

            String mejorCandidato = null;
            String apkConIcono = null;
            int mejorPuntuacion = -1;

            // 3. Inspeccionar cada APK hasta encontrar el recurso gráfico más adecuado
            for (String rutaApk : todasLasRutas) {
                ProcessBuilder pbListar = new ProcessBuilder("adb", "-s", d.getId(), "shell", "unzip", "-l", rutaApk);
                pbListar.redirectErrorStream(true);
                Process pListar = pbListar.start();

                BufferedReader lectorListar = new BufferedReader(new InputStreamReader(pListar.getInputStream()));
                String linea;

                while ((linea = lectorListar.readLine()) != null) {
                    linea = linea.trim();
                    if (linea.isEmpty()) continue;

                    String[] partes = linea.split("\\s+");
                    String nombreRecurso = partes[partes.length - 1];
                    String nombreLower = nombreRecurso.toLowerCase();

                    // Debe ser una imagen PNG ubicada dentro de los recursos
                    if (!nombreLower.endsWith(".png") || (!nombreLower.startsWith("res/") && !nombreLower.startsWith("r/"))) {
                        continue;
                    }

                    // Ignorar fondos planos de iconos adaptativos
                    if (nombreLower.contains("background") || nombreLower.contains("_bg") || nombreLower.contains("bg_")) {
                        continue;
                    }

                    // Puntuación por relevancia del nombre
                    int puntuacionNombre = 0;
                    if (nombreLower.contains("ic_launcher_round")) puntuacionNombre = 80;
                    else if (nombreLower.contains("ic_launcher")) puntuacionNombre = 70;
                    else if (nombreLower.contains("app_icon") || nombreLower.contains("ic_app")) puntuacionNombre = 60;
                    else if (nombreLower.contains("icon")) puntuacionNombre = 50;
                    else if (nombreLower.contains(palabraClaveApp)) puntuacionNombre = 40;
                    else if (nombreLower.contains("logo") || nombreLower.contains("launcher")) puntuacionNombre = 30;
                    else if (nombreLower.contains("drawable") || nombreLower.contains("mipmap")) puntuacionNombre = 15;

                    if (puntuacionNombre == 0) continue;

                    // Puntuación por calidad y densidad de imagen
                    int puntuacionDensidad = 0;
                    if (nombreLower.contains("xxxhdpi")) puntuacionDensidad = 6;
                    else if (nombreLower.contains("xxhdpi")) puntuacionDensidad = 5;
                    else if (nombreLower.contains("xhdpi")) puntuacionDensidad = 4;
                    else if (nombreLower.contains("hdpi")) puntuacionDensidad = 3;
                    else if (nombreLower.contains("mdpi")) puntuacionDensidad = 2;

                    int puntuacionTotal = puntuacionNombre + puntuacionDensidad;

                    if (puntuacionTotal > mejorPuntuacion) {
                        mejorPuntuacion = puntuacionTotal;
                        mejorCandidato = nombreRecurso;
                        apkConIcono = rutaApk;
                    }
                }
                pListar.waitFor();

                // Si encontramos un candidato de alta relevancia (ic_launcher o similar), no necesitamos buscar más APKs
                if (mejorPuntuacion >= 50) {
                    break;
                }
            }

            if (mejorCandidato == null || apkConIcono == null) {
                return null;
            }

            // 4. Extraer los bytes de la imagen directamente hacia Java
            Process pExtraer = new ProcessBuilder("adb", "-s", d.getId(), "exec-out", "unzip", "-p", apkConIcono, mejorCandidato).start();
            Image imagenOriginal = ImageIO.read(pExtraer.getInputStream());
            pExtraer.waitFor();

            if (imagenOriginal != null) {
                BufferedImage imgFinal = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = imgFinal.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(imagenOriginal, 0, 0, 32, 32, null);
                g2.dispose();

                // Guardar en la caché local para siguientes lecturas instantáneas
                ImageIO.write(imgFinal, "png", archivoIcono);
                return new ImageIcon(imgFinal);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Instala una lista de archivos APK (soporta archivos únicos o múltiples splits).
     */
    public void instalarApks(List<File> apks, Dispositivo d, Consumer<String> logger) {
        new Thread(() -> {
            try {
                if (apks.size() == 1) {
                    logger.accept("Instalando APK: " + apks.get(0).getName() + "...");
                    Process p = new ProcessBuilder("adb", "-s", d.getId(), "install", "-r", "-d", apks.get(0).getAbsolutePath()).start();
                    reportarSalidaProceso(p, logger);
                } else {
                    logger.accept("Instalando " + apks.size() + " partes divididas (Splits)...");
                    List<String> comando = new ArrayList<>();
                    comando.add("adb");
                    comando.add("-s");
                    comando.add(d.getId());
                    comando.add("install-multiple");
                    comando.add("-r");
                    comando.add("-d");
                    for (File f : apks) comando.add(f.getAbsolutePath());

                    Process p = new ProcessBuilder(comando).start();
                    reportarSalidaProceso(p, logger);
                }
            } catch (Exception e) {
                logger.accept("❌ Error al instalar: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Extrae todos los APKs asociados a un paquete instalado hacia una carpeta en la computadora.
     */
    public void extraerPaquete(String paquete, Dispositivo d, File carpetaDestino, Consumer<String> logger) {
        new Thread(() -> {
            try {
                logger.accept("Localizando rutas de instalación para " + paquete + "...");
                Process pRuta = new ProcessBuilder("adb", "-s", d.getId(), "shell", "pm", "path", paquete).start();
                BufferedReader lector = new BufferedReader(new InputStreamReader(pRuta.getInputStream()));
                List<String> rutas = new ArrayList<>();
                String linea;

                while ((linea = lector.readLine()) != null) {
                    if (linea.startsWith("package:")) {
                        rutas.add(linea.replace("package:", "").trim());
                    }
                }
                pRuta.waitFor();

                if (rutas.isEmpty()) {
                    logger.accept("❌ No se encontró la ruta del paquete indicado.");
                    return;
                }

                File subCarpeta = new File(carpetaDestino, paquete + "_apks");
                if (!subCarpeta.exists()) subCarpeta.mkdirs();

                int descargados = 0;
                for (String ruta : rutas) {
                    String nombreArchivo = ruta.substring(ruta.lastIndexOf("/") + 1);
                    logger.accept("Descargando: " + nombreArchivo + "...");
                    Process pPull = new ProcessBuilder("adb", "-s", d.getId(), "pull", ruta, subCarpeta.getAbsolutePath()).start();
                    pPull.waitFor();
                    descargados++;
                }

                logger.accept("✅ Extracción finalizada con éxito. " + descargados + " archivo(s) guardado(s) en: " + subCarpeta.getName());
            } catch (Exception e) {
                logger.accept("❌ Error al extraer paquete: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Desinstala una aplicación del dispositivo.
     */
    public void desinstalarApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "uninstall", paquete}, "Desinstalando " + paquete + "...", logger);
    }

    /**
     * Inicia la actividad principal de una aplicación en el dispositivo.
     */
    public void ejecutarApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "shell", "monkey", "-p", paquete, "-c", "android.intent.category.LAUNCHER", "1"},
                "Iniciando " + paquete + "...", logger);
    }

    /**
     * Borra la memoria caché y datos de usuario de una aplicación.
     */
    public void limpiarDatosApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "shell", "pm", "clear", paquete},
                "Limpiando datos y caché de " + paquete + "...", logger);
    }

    /**
     * Alinea un APK a bloques de 4 bytes utilizando zipalign.
     */
    public void alinearApk(File entrada, File salida, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"zipalign", "-v", "-p", "4", entrada.getAbsolutePath(), salida.getAbsolutePath()},
                "Alineando APK: " + entrada.getName() + "...", logger);
    }

    /**
     * Firma un APK utilizando certificado de depuración con apksigner.
     */
    public void firmarApkDebug(File apk, Consumer<String> logger) {
        File debugKeystore = new File(System.getProperty("user.home") + File.separator + ".android" + File.separator + "debug.keystore");
        if (!debugKeystore.exists()) {
            logger.accept("⚠ No se encontró el archivo debug.keystore en: " + debugKeystore.getAbsolutePath());
            return;
        }

        ejecutarComandoSimple(new String[]{
                "apksigner", "sign",
                "--ks", debugKeystore.getAbsolutePath(),
                "--ks-pass", "pass:android",
                "--key-pass", "pass:android",
                "--ks-key-alias", "androiddebugkey",
                apk.getAbsolutePath()
        }, "Firmando APK con certificado debug...", logger);
    }


    /**
     * Inicia la transmisión de pantalla con Scrcpy.
     * Si no está instalado, lo descarga y descomprime automáticamente de forma dinámica.
     */
    public void iniciarScrcpyNativo(Dispositivo d, Consumer<String> logger) {
        new Thread(() -> {
            try {
                // 1. Verificar si scrcpy.exe ya existe localmente; si no, descargarlo
                File ejecutable = obtenerOInstalarScrcpy(logger);
                if (ejecutable == null || !ejecutable.exists()) {
                    logger.accept("❌ No se pudo preparar Scrcpy para su ejecución.");
                    return;
                }

                logger.accept("Iniciando transmisión de pantalla con Scrcpy...");

                // 2. Construir el comando apuntando al ejecutable local descargado
                List<String> comando = new ArrayList<>();
                comando.add(ejecutable.getAbsolutePath());
                comando.add("-s");
                comando.add(d.getId());
                comando.add("--max-fps=60");

                ProcessBuilder pb = new ProcessBuilder(comando);
                pb.redirectErrorStream(true);
                Process proceso = pb.start();

                // 3. Monitorear mensajes de salida en vivo
                try (BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = lector.readLine()) != null) {
                        linea = linea.trim();
                        if (linea.toLowerCase().contains("error") || linea.toLowerCase().contains("failed")) {
                            logger.accept("⚠ [Scrcpy]: " + linea);
                        }
                    }
                }

                int codigoSalida = proceso.waitFor();
                if (codigoSalida == 0) {
                    logger.accept("✅ Sesión de Scrcpy finalizada correctamente.");
                } else {
                    logger.accept("ℹ Scrcpy se cerró (código de salida: " + codigoSalida + ").");
                }

            } catch (Exception e) {
                logger.accept("❌ Error al iniciar Scrcpy: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Busca el binario scrcpy.exe localmente. Si no existe, lo descarga desde internet.
     */
    private File obtenerOInstalarScrcpy(Consumer<String> logger) {
        // Carpeta donde residirá Scrcpy dentro de tu proyecto
        File carpetaDestino = new File("scrcpy");
        File ejecutable = new File(carpetaDestino, "scrcpy.exe");

        // Si ya está descargado y listo, lo reutilizamos
        if (ejecutable.exists() && ejecutable.isFile()) {
            return ejecutable;
        }

        // Si no existe, procedemos a la descarga automática
        logger.accept("🌐 Scrcpy no encontrado localmente. Iniciando descarga automática...");
        boolean exitoDescarga = descargarYDescomprimirScrcpy(carpetaDestino, logger);

        if (exitoDescarga && ejecutable.exists()) {
            logger.accept("✅ Scrcpy descargado y configurado exitosamente.");
            return ejecutable;
        }

        return null;
    }

    /**
     * Descarga el archivo comprimido ZIP desde GitHub y lo descomprime en la carpeta local.
     */
    private boolean descargarYDescomprimirScrcpy(File carpetaDestino, Consumer<String> logger) {
        File archivoZipTemporal = new File("scrcpy_temp.zip");
        try {
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            // A. Conectar y descargar el archivo ZIP con seguimiento de bytes
            URL url = URI.create(URL_SCRCPY_ZIP).toURL();
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0");
            conexion.setInstanceFollowRedirects(true); // Maneja redirecciones de GitHub

            int respuesta = conexion.getResponseCode();
            if (respuesta == HttpURLConnection.HTTP_MOVED_PERM || respuesta == HttpURLConnection.HTTP_MOVED_TEMP) {
                String nuevaUrl = conexion.getHeaderField("Location");
                conexion = (HttpURLConnection) URI.create(nuevaUrl).toURL().openConnection();
                conexion.setRequestProperty("User-Agent", "Mozilla/5.0");
            }

            long tamanoTotal = conexion.getContentLengthLong();

            try (InputStream in = new BufferedInputStream(conexion.getInputStream());
                 FileOutputStream out = new FileOutputStream(archivoZipTemporal)) {

                byte[] buffer = new byte[8192];
                long bytesDescargados = 0;
                int leidos;
                int porcentajeAnterior = -1;

                while ((leidos = in.read(buffer)) != -1) {
                    out.write(buffer, 0, leidos);
                    bytesDescargados += leidos;

                    if (tamanoTotal > 0) {
                        int porcentaje = (int) ((bytesDescargados * 100) / tamanoTotal);
                        if (porcentaje % 25 == 0 && porcentaje != porcentajeAnterior) {
                            logger.accept("⬇ Descargando Scrcpy: " + porcentaje + "%...");
                            porcentajeAnterior = porcentaje;
                        }
                    }
                }
            }

            // B. Descomprimir el contenido dentro de la carpeta local scrcpy/
            logger.accept("📦 Descomprimiendo archivos de Scrcpy...");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZipTemporal))) {
                ZipEntry entrada;
                while ((entrada = zis.getNextEntry()) != null) {
                    String nombreEntrada = entrada.getName();

                    // El ZIP de GitHub incluye una carpeta raíz interna (ej: scrcpy-win64-v2.4/scrcpy.exe).
                    // Quitamos esa primera carpeta para que los archivos queden limpios en scrcpy/
                    if (nombreEntrada.contains("/")) {
                        nombreEntrada = nombreEntrada.substring(nombreEntrada.indexOf("/") + 1);
                    }

                    if (nombreEntrada.isEmpty()) {
                        zis.closeEntry();
                        continue;
                    }

                    File archivoSalida = new File(carpetaDestino, nombreEntrada);

                    if (entrada.isDirectory()) {
                        archivoSalida.mkdirs();
                    } else {
                        // Asegurar la existencia de directorios padres
                        if (archivoSalida.getParentFile() != null) {
                            archivoSalida.getParentFile().mkdirs();
                        }

                        try (FileOutputStream fos = new FileOutputStream(archivoSalida)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            }

            // C. Limpiar el archivo temporal descargado
            if (archivoZipTemporal.exists()) {
                archivoZipTemporal.delete();
            }

            return true;

        } catch (Exception e) {
            logger.accept("❌ Error durante la descarga automática de Scrcpy: " + e.getMessage());
            if (archivoZipTemporal.exists()) {
                archivoZipTemporal.delete();
            }
            return false;
        }
    }











    /**
     * Toma una captura de pantalla y la guarda directamente en el archivo destino en la PC.
     */
    public void tomarCapturaPantalla(Dispositivo d, File archivoDestino, Consumer<String> logger, Runnable onFinish) {
        new Thread(() -> {
            try {
                logger.accept("Capturando pantalla del dispositivo...");
                ProcessBuilder pb = new ProcessBuilder("adb", "-s", d.getId(), "exec-out", "screencap", "-p");
                pb.redirectOutput(archivoDestino);
                Process proceso = pb.start();
                proceso.waitFor();

                if (archivoDestino.exists() && archivoDestino.length() > 0) {
                    logger.accept("✅ Captura guardada con éxito en: " + archivoDestino.getName());
                    if (onFinish != null) onFinish.run();
                } else {
                    logger.accept("❌ La captura no se pudo generar correctamente.");
                }
            } catch (Exception e) {
                logger.accept("❌ Error al tomar captura: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Inicia la lectura continua del flujo de Logcat del sistema.
     * @return El proceso en ejecución para poder cerrarlo cuando el usuario cierre la ventana de logs.
     */
    public Process iniciarLogcat(Dispositivo d, Consumer<String> consumidorLinea) {
        try {
            ProcessBuilder pb = new ProcessBuilder("adb", "-s", d.getId(), "logcat", "-v", "time");
            pb.redirectErrorStream(true);
            Process proceso = pb.start();

            new Thread(() -> {
                try (BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()))) {
                    String linea;
                    while ((linea = lector.readLine()) != null) {
                        consumidorLinea.accept(linea);
                    }
                } catch (Exception ignored) {}
            }).start();

            return proceso;
        } catch (Exception e) {
            consumidorLinea.accept("❌ No se pudo iniciar Logcat: " + e.getMessage());
            return null;
        }
    }

    // ==========================================
    // MÉTODOS AUXILIARES REUTILIZABLES
    // ==========================================

    private void ejecutarComandoSimple(String[] comando, String mensajeInicio, Consumer<String> logger) {
        new Thread(() -> {
            try {
                logger.accept(mensajeInicio);
                Process proceso = new ProcessBuilder(comando).start();
                reportarSalidaProceso(proceso, logger);
            } catch (Exception e) {
                logger.accept("❌ Error al ejecutar comando: " + e.getMessage());
            }
        }).start();
    }

    private void reportarSalidaProceso(Process p, Consumer<String> logger) {
        try {
            BufferedReader lector = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String linea;
            boolean reportado = false;
            while ((linea = lector.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isEmpty()) {
                    if (linea.contains("Success")) {
                        logger.accept("✅ " + linea);
                    } else if (linea.contains("Failure") || linea.contains("Error")) {
                        logger.accept("❌ " + linea);
                    } else {
                        logger.accept(linea);
                    }
                    reportado = true;
                }
            }
            p.waitFor();
            if (!reportado && p.exitValue() == 0) {
                logger.accept("✅ Operación completada.");
            }
        } catch (Exception ignored) {}
    }
}