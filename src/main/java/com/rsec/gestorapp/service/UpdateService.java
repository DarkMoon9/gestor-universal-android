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
 * Servicio encargado de comprobar versiones en GitHub Releases
 * y gestionar la auto-actualización y reemplazo del archivo .jar.
 *
 * @author Marco Vences
 * @version 1.1.0
 */
public class UpdateService {

    // Configuración oficial de tu repositorio en GitHub
    private static final String GITHUB_USUARIO = "DarkMoon9";
    private static final String GITHUB_REPOSITORIO = "gestor-universal-android";

    // Versión grabada en el binario local
    public static final String VERSION_ACTUAL = "1.1.0";

    /**
     * Consulta la API de GitHub en segundo plano.
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

    private static void descargarYReemplazar(String urlDescarga, String nombreNuevoJar) {
        try {
            // Verificar si se está ejecutando desde un .jar empaquetado real
            File jarEnEjecucion = new File(UpdateService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            if (!jarEnEjecucion.getName().toLowerCase().endsWith(".jar")) {
                JOptionPane.showMessageDialog(null,
                        "Estás ejecutando el proyecto desde el entorno de desarrollo (IntelliJ IDEA).\n" +
                                "Para probar el auto-reemplazo del archivo, compila con Maven y ejecuta el .jar directamente.",
                        "Aviso de Desarrollo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

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
                    File carpetaDestino = jarEnEjecucion.getParentFile();
                    File archivoTemporal = new File(carpetaDestino, "descarga_actualizacion.tmp");

                    URL url = new URL(urlDescarga);
                    HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
                    conexion.setRequestProperty("User-Agent", "Mozilla/5.0");

                    int estado = conexion.getResponseCode();
                    if (estado == HttpURLConnection.HTTP_MOVED_TEMP || estado == HttpURLConnection.HTTP_MOVED_PERM
                            || estado == 307 || estado == 308) {
                        String redireccion = conexion.getHeaderField("Location");
                        conexion = (HttpURLConnection) new URL(redireccion).openConnection();
                    }

                    int tamanoTotal = conexion.getContentLength();

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
                                "Descarga completada.\nLa aplicación se reiniciará para aplicar los cambios.",
                                "Actualización Lista",
                                JOptionPane.INFORMATION_MESSAGE);

                        ejecutarScriptActualizacion(archivoTemporal, nombreNuevoJar, jarEnEjecucion);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        dialogo.dispose();
                        JOptionPane.showMessageDialog(null,
                                "Error durante la descarga:\n" + ex.getMessage(),
                                "Error de Descarga",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();

            dialogo.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al determinar la ruta del ejecutable: " + e.getMessage());
        }
    }

    private static void ejecutarScriptActualizacion(File archivoTemporal, String nombreNuevoJar, File jarEnEjecucion) {
        try {
            File carpetaDestino = jarEnEjecucion.getParentFile();
            File scriptBat = new File(carpetaDestino, "actualizar.bat");
            File scriptVbs = new File(carpetaDestino, "ocultar.vbs");
            File nuevoJarFinal = new File(carpetaDestino, nombreNuevoJar);

            StringBuilder script = new StringBuilder();
            script.append("@echo off\r\n");
            // Espera 3 segundos a que Java libere completamente el archivo .jar
            script.append("timeout /t 3 /nobreak > nul\r\n");

            // Si el nombre cambió respecto al que se estaba ejecutando, elimina el viejo
            if (!jarEnEjecucion.getAbsolutePath().equalsIgnoreCase(nuevoJarFinal.getAbsolutePath())) {
                script.append("if exist \"").append(jarEnEjecucion.getAbsolutePath()).append("\" del /f /q \"").append(jarEnEjecucion.getAbsolutePath()).append("\"\r\n");
            }

            // Mueve el archivo temporal reemplazando el .jar final
            script.append("move /y \"").append(archivoTemporal.getAbsolutePath()).append("\" \"").append(nuevoJarFinal.getAbsolutePath()).append("\"\r\n");

            // Cambia de directorio e inicia el nuevo .jar
            script.append("cd /d \"").append(carpetaDestino.getAbsolutePath()).append("\"\r\n");
            script.append("start \"\" java -jar \"").append(nuevoJarFinal.getName()).append("\"\r\n");

            // Elimina los archivos temporales de script
            script.append("del \"").append(scriptVbs.getAbsolutePath()).append("\"\r\n");
            script.append("del \"%~f0\"\r\n");

            try (FileOutputStream fosBat = new FileOutputStream(scriptBat)) {
                fosBat.write(script.toString().getBytes());
            }

            // Script en VBScript para ejecutar en modo oculto sin ventanas de CMD
            String contenidoVbs = "Set WshShell = CreateObject(\"WScript.Shell\")\r\n" +
                    "WshShell.Run \"cmd /c \"\"" + scriptBat.getAbsolutePath() + "\"\"\", 0, False\r\n";

            try (FileOutputStream fosVbs = new FileOutputStream(scriptVbs)) {
                fosVbs.write(contenidoVbs.getBytes());
            }

            Runtime.getRuntime().exec("wscript.exe \"" + scriptVbs.getAbsolutePath() + "\"");
            System.exit(0);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo ejecutar el script de actualización:\n" + e.getMessage(),
                    "Error de Actualización",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}