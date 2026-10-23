package com.rsec.gestorapp.service;

import com.rsec.gestorapp.model.Dispositivo;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Servicio encargado de la orquestación y comunicación directa con el servidor ADB
 * (Android Debug Bridge), gestión de subprocesos del sistema operativo, descarga
 * dinámica de utilidades externas (Scrcpy) y extracción de recursos desde paquetes APK.
 *
 * @author Marco Vences
 * @version 1.0
 */
public class ADBService {

    /** URL oficial de descarga de la versión portable de Scrcpy para Windows 64-bits. */
    private static final String URL_SCRCPY_ZIP = "https://github.com/Genymobile/scrcpy/releases/download/v2.4/scrcpy-win64-v2.4.zip";

    /**
     * Puertos locales estándar evaluados por orden de prioridad para la autoconexión de emuladores:
     * <ul>
     *   <li>16384: Puerto oficial y principal de MuMu Player 12</li>
     *   <li>5555: Puerto estándar de BlueStacks, LDPlayer y emuladores AVD</li>
     *   <li>62001: Puerto primario de NoxPlayer</li>
     *   <li>21503: Puerto de MEmu Player</li>
     *   <li>7555: Puerto secundario de MuMu Player legacy</li>
     * </ul>
     */
    private static final int[] PUERTOS_EMULADORES = {
            16384, 5555, 62001, 21503, 7555
    };

    /**
     * Busca asíncronamente los dispositivos conectados vía USB, Wi-Fi o Emuladores activos.
     * Realiza una autoconexión previa por puertos de red y filtra instancias duplicadas en la lista final.
     *
     * @param callback Consumidor que recibe la lista procesada de objetos {@link Dispositivo}.
     * @param logger   Consumidor para el registro de mensajes de estado en la consola de la UI.
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
                Set<String> modelosEmuladoresRegistrados = new HashSet<>();

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
     * Comprueba mediante sockets TCP de respuesta rápida (timeout 80ms) si los puertos
     * conocidos de emuladores están abiertos en localhost y ejecuta 'adb connect' automáticamente.
     *
     * @param logger Consumidor para registrar los puertos enlazados.
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
     * Desconecta de forma silenciosa un puerto secundario redundante en ADB para evitar duplicados.
     *
     * @param idDispositivo Dirección IP y puerto del dispositivo a desconectar (ej. "127.0.0.1:5555").
     */
    private void desconectarPuertoDuplicado(String idDispositivo) {
        try {
            new ProcessBuilder("adb", "disconnect", idDispositivo).start();
        } catch (Exception ignored) {}
    }

    /**
     * Analiza el identificador y los atributos devueltos por 'adb devices -l' para clasificar
     * si corresponde a un emulador conocido o a un dispositivo físico real.
     *
     * @param id          Identificador devuelto por ADB (serie USB o IP:puerto).
     * @param estadoAdb   Estado de conexión reportado ("device", "unauthorized").
     * @param partesLinea Arreglo de cadenas con los metadatos de la línea de ADB.
     * @return Objeto {@link Dispositivo} instanciado con su clasificación correspondiente.
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

        // Reglas heurísticas de detección de emuladores
        if (idLower.startsWith("emulator-") || meta.contains("sdk_gphone") || meta.contains("goldfish") || meta.contains("ranchu")) {
            esEmulador = true;
            tipoEmulador = "Android Studio AVD";
            if (modelo.equalsIgnoreCase("Android") || modelo.toLowerCase().contains("sdk")) {
                modelo = "Google Pixel AVD";
            }
        } else if (idLower.contains(":62001") || idLower.contains(":62025") || meta.contains("nox")) {
            esEmulador = true;
            tipoEmulador = "NoxPlayer";
        } else if (idLower.contains(":21503") || meta.contains("memu")) {
            esEmulador = true;
            tipoEmulador = "MEmu";
        } else if (idLower.contains(":7555") || idLower.contains(":16384") || meta.contains("mumu") || meta.contains("netease")) {
            esEmulador = true;
            tipoEmulador = "MuMu Player";
        } else if (idLower.contains(":58526") || meta.contains("subsystem_for_android") || meta.contains("wsa")) {
            esEmulador = true;
            tipoEmulador = "WSA Windows";
        } else if (idLower.startsWith("192.168.56.") || meta.contains("genymotion") || meta.contains("vbox86")) {
            esEmulador = true;
            tipoEmulador = "Genymotion";
        } else if (idLower.startsWith("127.0.0.1:") || idLower.startsWith("localhost:")) {
            esEmulador = true;
            if (meta.contains("bluestacks") || meta.contains("bst")) {
                tipoEmulador = "BlueStacks";
            } else if (meta.contains("ldplayer") || meta.contains("ld")) {
                tipoEmulador = "LDPlayer";
            } else {
                tipoEmulador = "Emulador Virtual";
            }
        } else if (meta.contains("emulator") || meta.contains("virtualbox") || meta.contains("qemu")) {
            esEmulador = true;
            tipoEmulador = "Emulador";
        }

        String estadoFinal = "device".equalsIgnoreCase(estadoAdb) ? "Conectado" : estadoAdb;
        return new Dispositivo(id, modelo, estadoFinal, esEmulador, tipoEmulador);
    }

    /**
     * Obtiene la lista de nombres de paquetes de aplicaciones de terceros instaladas en el dispositivo.
     *
     * @param d        Dispositivo objetivo.
     * @param callback Consumidor que recibe la lista de identificadores de paquete.
     * @param logger   Consumidor para publicar mensajes de estado.
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
     * Extrae el icono representativo de una aplicación desde sus archivos APK en el dispositivo.
     * Inspecciona la división base y los splits de densidad gráfica, aplicando un escalado suave a 32x32 píxeles.
     * Almacena el resultado en la caché local en disco para lecturas subsiguientes.
     *
     * @param d       Dispositivo donde está instalada la aplicación.
     * @param paquete Nombre del paquete (ej. "com.whatsapp").
     * @return Objeto {@link Icon} escalado o {@code null} si no se localiza ninguna imagen PNG extraíble.
     */
    public Icon extraerIconoApp(Dispositivo d, String paquete) {
        try {
            // 1. Verificar si ya existe en la caché local de disco
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

            // Ordenar para revisar primero los splits de densidad (xxhdpi, xhdpi) y base.apk
            todasLasRutas.sort((a, b) -> {
                if (a.contains("hdpi") || a.contains("dpi")) return -1;
                if (b.contains("hdpi") || b.contains("dpi")) return 1;
                if (a.endsWith("base.apk")) return -1;
                return 1;
            });

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

                // Guardar en la caché local
                ImageIO.write(imgFinal, "png", archivoIcono);
                return new ImageIcon(imgFinal);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Instala una lista de archivos APK en el dispositivo objetivo.
     * Selecciona automáticamente entre instalación simple (`adb install`) o múltiple (`adb install-multiple`).
     *
     * @param apks   Lista de archivos APK a instalar.
     * @param d      Dispositivo objetivo.
     * @param logger Consumidor para registrar el progreso y resultado de la instalación.
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
     * Extrae todos los archivos APK asociados a un paquete instalado en el dispositivo hacia una carpeta local en la PC.
     *
     * @param paquete        Identificador del paquete a extraer (ej. "com.example.app").
     * @param d              Dispositivo objetivo.
     * @param carpetaDestino Directorio local donde se guardará la subcarpeta con los APKs.
     * @param logger         Consumidor para reportar los archivos descargados.
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
     *
     * @param paquete Identificador de paquete a eliminar.
     * @param d       Dispositivo objetivo.
     * @param logger  Consumidor para mensajes de estado.
     */
    public void desinstalarApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "uninstall", paquete}, "Desinstalando " + paquete + "...", logger);
    }

    /**
     * Inicia la actividad principal de una aplicación en el dispositivo mediante la herramienta `monkey`.
     *
     * @param paquete Identificador de paquete a lanzar.
     * @param d       Dispositivo objetivo.
     * @param logger  Consumidor para mensajes de estado.
     */
    public void ejecutarApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "shell", "monkey", "-p", paquete, "-c", "android.intent.category.LAUNCHER", "1"},
                "Iniciando " + paquete + "...", logger);
    }

    /**
     * Elimina los datos de usuario y la memoria caché de una aplicación instalada.
     *
     * @param paquete Identificador de paquete.
     * @param d       Dispositivo objetivo.
     * @param logger  Consumidor para mensajes de estado.
     */
    public void limpiarDatosApp(String paquete, Dispositivo d, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"adb", "-s", d.getId(), "shell", "pm", "clear", paquete},
                "Limpiando datos y caché de " + paquete + "...", logger);
    }

    /**
     * Alinea las fronteras de datos no comprimidos de un archivo APK a bloques de 4 bytes utilizando `zipalign`.
     *
     * @param entrada Archivo APK origen.
     * @param salida  Archivo APK destino alineado.
     * @param logger  Consumidor para mensajes de estado.
     */
    public void alinearApk(File entrada, File salida, Consumer<String> logger) {
        ejecutarComandoSimple(new String[]{"zipalign", "-v", "-p", "4", entrada.getAbsolutePath(), salida.getAbsolutePath()},
                "Alineando APK: " + entrada.getName() + "...", logger);
    }

    /**
     * Firma un APK con la clave oficial de depuración de Android mediante `apksigner`.
     *
     * @param apk    Archivo APK a firmar.
     * @param logger Consumidor para mensajes de estado.
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
     * Inicia la transmisión de pantalla del dispositivo mediante Scrcpy.
     * Si no se encuentra el ejecutable localmente, gestiona su descarga y descompresión automática.
     *
     * @param d      Dispositivo a proyectar.
     * @param logger Consumidor para mensajes de registro.
     */
    public void iniciarScrcpyNativo(Dispositivo d, Consumer<String> logger) {
        new Thread(() -> {
            try {
                File ejecutable = obtenerOInstalarScrcpy(logger);
                if (ejecutable == null || !ejecutable.exists()) {
                    logger.accept("❌ No se pudo preparar Scrcpy para su ejecución.");
                    return;
                }

                logger.accept("Iniciando transmisión de pantalla con Scrcpy...");

                List<String> comando = new ArrayList<>();
                comando.add(ejecutable.getAbsolutePath());
                comando.add("-s");
                comando.add(d.getId());
                comando.add("--max-fps=60");

                ProcessBuilder pb = new ProcessBuilder(comando);
                pb.redirectErrorStream(true);
                Process proceso = pb.start();

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
     * Verifica la existencia local de `scrcpy.exe`. Si no existe, invoca la rutina de descarga.
     *
     * @param logger Consumidor de registros.
     * @return Objeto {@link File} del ejecutable o {@code null} en caso de fallo.
     */
    private File obtenerOInstalarScrcpy(Consumer<String> logger) {
        File carpetaDestino = new File("scrcpy");
        File ejecutable = new File(carpetaDestino, "scrcpy.exe");

        if (ejecutable.exists() && ejecutable.isFile()) {
            return ejecutable;
        }

        logger.accept("🌐 Scrcpy no encontrado localmente. Iniciando descarga automática...");
        boolean exitoDescarga = descargarYDescomprimirScrcpy(carpetaDestino, logger);

        if (exitoDescarga && ejecutable.exists()) {
            logger.accept("✅ Scrcpy descargado y configurado exitosamente.");
            return ejecutable;
        }

        return null;
    }

    /**
     * Descarga la distribución Zip de Scrcpy desde GitHub, procesa redirecciones HTTP
     * y descomprime los archivos dentro del directorio local objetivo.
     *
     * @param carpetaDestino Directorio de destino para la extracción.
     * @param logger         Consumidor para el reporte del porcentaje de descarga.
     * @return {@code true} si la descarga y extracción finalizaron correctamente; {@code false} en caso contrario.
     */
    private boolean descargarYDescomprimirScrcpy(File carpetaDestino, Consumer<String> logger) {
        File archivoZipTemporal = new File("scrcpy_temp.zip");
        try {
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            // Descarga de archivo mediante HttpURLConnection
            URL url = URI.create(URL_SCRCPY_ZIP).toURL();
            HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
            conexion.setRequestProperty("User-Agent", "Mozilla/5.0");
            conexion.setInstanceFollowRedirects(true);

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

            // Descompresión del archivo ZIP
            logger.accept("📦 Descomprimiendo archivos de Scrcpy...");
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archivoZipTemporal))) {
                ZipEntry entrada;
                while ((entrada = zis.getNextEntry()) != null) {
                    String nombreEntrada = entrada.getName();

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
     * Obtiene una captura de pantalla del dispositivo y la almacena en el archivo especificado en la PC.
     *
     * @param d              Dispositivo objetivo.
     * @param archivoDestino Archivo de salida PNG en el sistema local.
     * @param logger         Consumidor para registros.
     * @param onFinish       Tarea ejecutable tras finalizar el guardado.
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
     * Inicia la lectura continua en segundo plano del flujo de Logcat del dispositivo.
     *
     * @param d               Dispositivo objetivo.
     * @param consumidorLinea Consumidor que procesa cada línea de log generada por el sistema.
     * @return El objeto {@link Process} en ejecución para permitir su posterior finalización.
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

    /**
     * Método auxiliar privado que ejecuta un comando externo en un hilo secundario independiente.
     *
     * @param comando       Arreglo con el comando y sus argumentos.
     * @param mensajeInicio Mensaje informativo enviado al logger antes de iniciar.
     * @param logger        Consumidor para el registro de salida.
     */
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

    /**
     * Lee la salida estándar de un proceso e informa los resultados relevantes al logger.
     *
     * @param p      Proceso en ejecución.
     * @param logger Consumidor para registrar la salida.
     */
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