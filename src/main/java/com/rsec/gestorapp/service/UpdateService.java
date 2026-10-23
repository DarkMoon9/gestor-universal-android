package com.rsec.gestorapp.service;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Servicio encargado de verificar si existen nuevas versiones de la aplicación
 * en GitHub Releases y gestionar su descarga y reemplazo automático.
 *
 * @author Marco Vences
 * @version 1.1.0
 */
public class UpdateService {

    // Configuración con los datos exactos de tu cuenta y repositorio en GitHub
    private static final String GITHUB_USUARIO = "DarkMoon9";
    private static final String GITHUB_REPOSITORIO = "gestor-universal-android";

    // Versión instalada actualmente en la máquina local
    public static final String VERSION_ACTUAL = "1.0.0";

    /**
     * Consulta la API de GitHub en segundo plano para verificar si existe una nueva versión.
     */
    public static void comprobarActualizacionesManual() {
        new Thread(() -> {
            try {
                String urlApi = String.format("https://api.github.com/repos/%s/%s/releases/latest",
                        GITHUB_USUARIO, GITHUB_REPOSITORIO);

                URL url = new URL(urlApi);
                HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conexion.setConnectTimeout(5000);
                conexion.setReadTimeout(5000);

                int codigoRespuesta = conexion.getResponseCode();

                if (codigoRespuesta == 200) {
                    BufferedReader lector = new BufferedReader(new InputStreamReader(conexion.getInputStream()));
                    StringBuilder respuesta = new StringBuilder();
                    String linea;

                    while ((linea = lector.readLine()) != null) {
                        respuesta.append(linea);
                    }
                    lector.close();

                    String json = respuesta.toString();
                    String ultimaVersion = extraerTagVersion(json);
                    String urlDescargaJar = extraerUrlDescargaJar(json);
                    String nombreNuevoJar = extraerNombreArchivoRemoto(urlDescargaJar);

                    // Devuelve la respuesta al hilo de interfaz gráfica de Swing
                    SwingUtilities.invokeLater(() -> procesarResultado(ultimaVersion, urlDescargaJar, nombreNuevoJar));

                } else if (codigoRespuesta == 404) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "No se encontraron publicaciones de versiones en el repositorio.",
                            "Verificación de Actualizaciones",
                            JOptionPane.INFORMATION_MESSAGE));
                } else {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "No se pudo consultar GitHub. Código HTTP: " + codigoRespuesta,
                            "Error de Conexión",
                            JOptionPane.ERROR_MESSAGE));
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                        "Error al conectar con el servidor:\n" + e.getMessage(),
                        "Error de Red",
                        JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    /**
     * Extrae el valor de la etiqueta de versión ("tag_name") limpiando la letra 'v'.
     *
     * @param json Cadena JSON devuelta por GitHub.
     * @return Versión numérica limpia (ejemplo: "1.1.0").
     */
    private static String extraerTagVersion(String json) {
        String clave = "\"tag_name\":\"";
        int inicio = json.indexOf(clave);
        if (inicio != -1) {
            inicio += clave.length();
            int fin = json.indexOf("\"", inicio);
            if (fin != -1) {
                return json.substring(inicio, fin).replace("v", "").trim();
            }
        }
        return VERSION_ACTUAL;
    }

    /**
     * Localiza en el JSON la URL de descarga directa que termine en extensión .jar.
     *
     * @param json Cadena JSON devuelta por GitHub.
     * @return Enlace directo al archivo binario ejecutable.
     */
    private static String extraerUrlDescargaJar(String json) {
        String clave = "\"browser_download_url\":\"";
        int index = 0;
        while ((index = json.indexOf(clave, index)) != -1) {
            index += clave.length();
            int fin = json.indexOf("\"", index);
            if (fin != -1) {
                String candidateUrl = json.substring(index, fin);
                if (candidateUrl.toLowerCase().endsWith(".jar")) {
                    return candidateUrl;
                }
            }
        }
        return null;
    }

    /**
     * Obtiene el nombre real del archivo a partir de la URL de descarga.
     *
     * @param urlDescarga Dirección web del archivo adjunto.
     * @return Nombre del archivo (ejemplo: "GestorUniversalAndroid.jar").
     */
    private static String extraerNombreArchivoRemoto(String urlDescarga) {
        if (urlDescarga == null || urlDescarga.isEmpty()) {
            return "GestorUniversalAndroid.jar";
        }
        int ultimoSlash = urlDescarga.lastIndexOf('/');
        if (ultimoSlash != -1 && ultimoSlash < urlDescarga.length() - 1) {
            return urlDescarga.substring(ultimoSlash + 1);
        }
        return "GestorUniversalAndroid.jar";
    }

    /**
     * Notifica al usuario y pregunta si desea proceder con la actualización automática.
     */
    private static void procesarResultado(String ultimaVersion, String urlDescargaJar, String nombreNuevoJar) {
        if (!VERSION_ACTUAL.equals(ultimaVersion)) {
            int opcion = JOptionPane.showConfirmDialog(null,
                    "¡Hay una nueva versión disponible!\n\n" +
                            "Versión actual: " + VERSION_ACTUAL + "\n" +
                            "Nueva versión: " + ultimaVersion + "\n\n" +
                            "¿Deseas descargar e instalar la actualización automáticamente ahora?",
                    "Actualización Disponible",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (opcion == JOptionPane.YES_OPTION) {
                if (urlDescargaJar != null && !urlDescargaJar.isEmpty()) {
                    descargarYReemplazar(urlDescargaJar, nombreNuevoJar);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "No se encontró un archivo .jar ejecutable adjunto en el release de GitHub.",
                            "Archivo no encontrado",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null,
                    "Tu aplicación ya se encuentra actualizada a la última versión (" + VERSION_ACTUAL + ").",
                    "Sistema Actualizado",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Descarga el nuevo binario mostrando una barra de progreso y ejecuta el script de reemplazo.
     */
    private static void descargarYReemplazar(String urlDescarga, String nombreNuevoJar) {
        JDialog dialogo = new JDialog();
        dialogo.setTitle("Descargando Actualización...");
        dialogo.setSize(380, 130);
        dialogo.setLocationRelativeTo(null);
        dialogo.setLayout(new BorderLayout(10, 10));
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JLabel lblEstado = new JLabel("Conectando con GitHub...");
        lblEstado.setBorder(new EmptyBorder(10, 15, 0, 15));
        JProgressBar barra = new JProgressBar(0, 100);
        barra.setStringPainted(true);
        barra.setBorder(new EmptyBorder(0, 15, 10, 15));

        dialogo.add(lblEstado, BorderLayout.NORTH);
        dialogo.add(barra, BorderLayout.CENTER);

        new Thread(() -> {
            try {
                URL url = new URL(urlDescarga);
                HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestProperty("User-Agent", "Mozilla/5.0");

                // Seguir redirecciones HTTP habituales en descargas de GitHub
                int estado = conexion.getResponseCode();
                if (estado == HttpURLConnection.HTTP_MOVED_TEMP || estado == HttpURLConnection.HTTP_MOVED_PERM
                        || estado == 307 || estado == 308) {
                    String redireccion = conexion.getHeaderField("Location");
                    conexion = (HttpURLConnection) new URL(redireccion).openConnection();
                }

                int tamanoTotal = conexion.getContentLength();
                File archivoTemporal = new File("descarga_actualizacion.tmp");

                try (InputStream entrada = conexion.getInputStream();
                     FileOutputStream salida = new FileOutputStream(archivoTemporal)) {

                    byte[] bufer = new byte[4096];
                    int leidos;
                    long acum = 0;

                    while ((leidos = entrada.read(bufer)) != -1) {
                        salida.write(bufer, 0, leidos);
                        acum += leidos;

                        if (tamanoTotal > 0) {
                            int porcentaje = (int) ((acum * 100) / tamanoTotal);
                            SwingUtilities.invokeLater(() -> {
                                barra.setValue(porcentaje);
                                lblEstado.setText("Descargando: " + porcentaje + "% completado");
                            });
                        }
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    dialogo.dispose();
                    JOptionPane.showMessageDialog(null,
                            "Descarga completada con éxito.\nLa aplicación se cerrará para iniciar la nueva versión.",
                            "Actualización Lista",
                            JOptionPane.INFORMATION_MESSAGE);

                    ejecutarScriptActualizacion(archivoTemporal, nombreNuevoJar);
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    dialogo.dispose();
                    JOptionPane.showMessageDialog(null,
                            "Error durante la descarga de la actualización:\n" + ex.getMessage(),
                            "Error de Descarga",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();

        dialogo.setVisible(true);
    }

    /**
     * Construye y lanza un script .bat temporal que se ejecuta de forma completamente
     * invisible utilizando un micro-script VBScript, evitando que aparezcan ventanas negras de CMD.
     *
     * @param archivoTemporal Archivo .tmp descargado desde GitHub.
     * @param nombreNuevoJar  Nombre final del archivo ejecutable de la nueva versión.
     */
    private static void ejecutarScriptActualizacion(File archivoTemporal, String nombreNuevoJar) {
        try {
            // Localiza el archivo ejecutable .jar actual
            File jarEnEjecucion = new File(UpdateService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            String nombreActual = jarEnEjecucion.getName();
            File carpetaDestino = jarEnEjecucion.getParentFile();

            // Si se ejecuta desde el IDE (target/classes/), usar la carpeta del proyecto
            if (!nombreActual.toLowerCase().endsWith(".jar") || carpetaDestino == null) {
                carpetaDestino = new File(".");
                nombreActual = "GestorAndroid-Viejo.jar";
            }

            File scriptBat = new File(carpetaDestino, "actualizar.bat");
            File scriptVbs = new File(carpetaDestino, "ocultar.vbs");

            // 1. Construcción del script por lotes (.bat)
            StringBuilder script = new StringBuilder();
            script.append("@echo off\r\n");
            // Espera 2 segundos en segundo plano a que la JVM de Java se cierre por completo
            script.append("timeout /t 2 /nobreak > nul\r\n");

            // Si el nombre cambió entre versiones, elimina la versión antigua
            if (!nombreActual.equalsIgnoreCase(nombreNuevoJar)) {
                script.append("if exist \"").append(nombreActual).append("\" del /f /q \"").append(nombreActual).append("\"\r\n");
            }

            // Mueve el archivo descargado al nombre del nuevo .jar
            script.append("move /y \"").append(archivoTemporal.getName()).append("\" \"").append(nombreNuevoJar).append("\"\r\n");

            // Ejecuta el nuevo archivo .jar
            script.append("start \"\" java -jar \"").append(nombreNuevoJar).append("\"\r\n");

            // Borra los scripts temporales creados
            script.append("del \"").append(scriptVbs.getName()).append("\"\r\n");
            script.append("del \"%~f0\"\r\n");

            try (FileOutputStream fosBat = new FileOutputStream(scriptBat)) {
                fosBat.write(script.toString().getBytes());
            }

            // 2. Construcción del script VBScript (.vbs) para ocultar la ventana negra de CMD
            // El parámetro '0' al final de Run indica ventana completamente invisible
            String contenidoVbs = "Set WshShell = CreateObject(\"WScript.Shell\")\r\n" +
                    "WshShell.Run \"cmd /c \"\"" + scriptBat.getAbsolutePath() + "\"\"\", 0, False\r\n";

            try (FileOutputStream fosVbs = new FileOutputStream(scriptVbs)) {
                fosVbs.write(contenidoVbs.getBytes());
            }

            // 3. Ejecutar el VBScript de forma silenciosa con wscript.exe
            Runtime.getRuntime().exec("wscript.exe \"" + scriptVbs.getAbsolutePath() + "\"");

            // 4. Cerrar la aplicación Java actual inmediatamente
            System.exit(0);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo ejecutar el reemplazo silencioso:\n" + e.getMessage(),
                    "Error de Actualización",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}