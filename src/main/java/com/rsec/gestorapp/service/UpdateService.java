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
 * Servicio encargado de gestionar la comprobación de versiones en GitHub Releases
 * y la actualización del archivo ejecutable hacia "Gestor Android.jar".
 *
 * @author DarkMoon9
 * @version 1.1.0
 */
public class UpdateService {

    // Configuración del repositorio en GitHub
    private static final String GITHUB_USUARIO = "DarkMoon9";
    private static final String GITHUB_REPOSITORIO = "gestor-universal-android";

    // Nombre definitivo del archivo ejecutable que tendrá la aplicación
    private static final String NOMBRE_JAR_DESTINO = "Gestor_Android.jar";

    // =========================================================================
    // IMPORTANTE: Esta versión debe coincidir con el tag publicado en GitHub
    // =========================================================================
    public static final String VERSION_ACTUAL = "1.1.0";

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
                conexion.setConnectTimeout(6000);
                conexion.setReadTimeout(6000);

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

                    SwingUtilities.invokeLater(() -> procesarResultado(ultimaVersion, urlDescargaJar));

                } else if (codigoRespuesta == 404) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                            "No se encontraron versiones publicadas en el repositorio.",
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

    private static void procesarResultado(String ultimaVersion, String urlDescargaJar) {
        if (!VERSION_ACTUAL.equals(ultimaVersion)) {
            int opcion = JOptionPane.showConfirmDialog(null,
                    "¡Hay una nueva versión disponible!\n\n" +
                            "Versión actual: " + VERSION_ACTUAL + "\n" +
                            "Nueva versión: " + ultimaVersion + "\n\n" +
                            "¿Deseas descargar la actualización ahora?",
                    "Actualización Disponible",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (opcion == JOptionPane.YES_OPTION) {
                if (urlDescargaJar != null && !urlDescargaJar.isEmpty()) {
                    descargarYReemplazar(urlDescargaJar);
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

    private static void descargarYReemplazar(String urlDescarga) {
        try {
            // Ubicación del archivo en ejecución
            File jarEnEjecucion = new File(UpdateService.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            // Si se ejecuta desde el IDE, mostrar aviso informativo
            if (!jarEnEjecucion.getName().toLowerCase().endsWith(".jar")) {
                JOptionPane.showMessageDialog(null,
                        "Estás ejecutando desde IntelliJ IDEA.\nPara probar la sustitución física, ejecuta el archivo .jar empaquetado.",
                        "Modo Desarrollo",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            File carpetaDestino = jarEnEjecucion.getParentFile();
            File archivoTemporal = new File(carpetaDestino, "actualizacion.tmp");

            JDialog dialogo = new JDialog();
            dialogo.setTitle("Descargando Actualización...");
            dialogo.setSize(380, 130);
            dialogo.setLocationRelativeTo(null);
            dialogo.setLayout(new BorderLayout(10, 10));
            dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

            JLabel lblEstado = new JLabel("Conectando con el servidor...");
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

                    // Manejo de redirecciones de GitHub (Amazon S3)
                    int estado = conexion.getResponseCode();
                    if (estado == HttpURLConnection.HTTP_MOVED_TEMP || estado == HttpURLConnection.HTTP_MOVED_PERM
                            || estado == 307 || estado == 308) {
                        String redireccion = conexion.getHeaderField("Location");
                        conexion = (HttpURLConnection) new URL(redireccion).openConnection();
                    }

                    int tamanoTotal = conexion.getContentLength();

                    try (InputStream entrada = conexion.getInputStream();
                         FileOutputStream salida = new FileOutputStream(archivoTemporal)) {

                        byte[] bufer = new byte[8192];
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
                                "Descarga finalizada.\nLa aplicación se cerrará para guardar el nuevo archivo '" + NOMBRE_JAR_DESTINO + "'.",
                                "Actualización Lista",
                                JOptionPane.INFORMATION_MESSAGE);

                        ejecutarSustitucionManual(archivoTemporal, jarEnEjecucion);
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        dialogo.dispose();
                        JOptionPane.showMessageDialog(null,
                                "Error al descargar el archivo:\n" + ex.getMessage(),
                                "Error de Descarga",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();

            dialogo.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error con la ruta del ejecutable: " + e.getMessage());
        }
    }

    /**
     * Crea un script que reemplaza el archivo por "Gestor Android.jar",
     * muestra un aviso de confirmación y finaliza sin relanzar la app.
     */
    private static void ejecutarSustitucionManual(File archivoTemporal, File jarEnEjecucion) {
        try {
            File carpetaDestino = jarEnEjecucion.getParentFile();
            File scriptBat = new File(carpetaDestino, "reemplazar.bat");
            File scriptVbs = new File(carpetaDestino, "ocultar.vbs");
            File archivoFinal = new File(carpetaDestino, NOMBRE_JAR_DESTINO);

            StringBuilder script = new StringBuilder();
            script.append("@echo off\r\n");
            // Espera 2 segundos para asegurar que Java liberó el archivo en Windows
            script.append("timeout /t 2 /nobreak > nul\r\n");

            // Si el .jar que se estaba usando tenía otro nombre, se elimina
            if (!jarEnEjecucion.getAbsolutePath().equalsIgnoreCase(archivoFinal.getAbsolutePath())) {
                script.append("if exist \"").append(jarEnEjecucion.getAbsolutePath()).append("\" del /f /q \"").append(jarEnEjecucion.getAbsolutePath()).append("\"\r\n");
            }

            // Mueve el archivo temporal al nombre definitivo con comillas para soportar espacios
            script.append("move /y \"").append(archivoTemporal.getAbsolutePath()).append("\" \"").append(archivoFinal.getAbsolutePath()).append("\"\r\n");

            // Mensaje nativo emergente de Windows informando que ya puede abrirlo
            script.append("msg * \"Actualizacion completada con exito. Ya puedes abrir ").append(NOMBRE_JAR_DESTINO).append("\"\r\n");

            // Elimina los scripts temporales
            script.append("del \"").append(scriptVbs.getAbsolutePath()).append("\"\r\n");
            script.append("del \"%~f0\"\r\n");

            try (FileOutputStream fosBat = new FileOutputStream(scriptBat)) {
                fosBat.write(script.toString().getBytes());
            }

            // VBScript para ejecutar el .bat en segundo plano sin consola negra
            String contenidoVbs = "Set WshShell = CreateObject(\"WScript.Shell\")\r\n" +
                    "WshShell.Run \"cmd /c \"\"" + scriptBat.getAbsolutePath() + "\"\"\", 0, False\r\n";

            try (FileOutputStream fosVbs = new FileOutputStream(scriptVbs)) {
                fosVbs.write(contenidoVbs.getBytes());
            }

            // Ejecuta el VBScript de forma silenciosa
            Runtime.getRuntime().exec("wscript.exe \"" + scriptVbs.getAbsolutePath() + "\"");

            // Cierra la aplicación actual
            System.exit(0);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Error al crear el script de actualización:\n" + e.getMessage(),
                    "Error de Actualización",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}