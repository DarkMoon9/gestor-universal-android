package com.rsec.gestorapp.ui;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.rsec.gestorapp.model.Dispositivo;
import com.rsec.gestorapp.service.ADBService;
import com.rsec.gestorapp.ui.components.BotonTarjeta;
import com.rsec.gestorapp.ui.components.DispositivoRenderer;
import com.rsec.gestorapp.ui.components.UIFactory;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.Icon;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador principal y ventana de la aplicación GestorApp.
 */
public class GestorApp extends JFrame {

    private JTextPane consolaMensajes;
    private JComboBox<Dispositivo> menuDispositivos;
    private JTextField entradaPaquete;
    private JCheckBox chkMostrarComandos; // Casilla para activar los comandos didácticos
    private ADBService adbService;
    private SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss");
    private Map<String, Icon> cacheIconos = new ConcurrentHashMap<>();

    public GestorApp() {
        this.adbService = new ADBService();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.put("Button.arc", 10);
                UIManager.put("Component.arc", 10);
                UIManager.put("TextComponent.arc", 10);
                FlatMacDarkLaf.setup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new GestorApp().construirVentana();
        });
    }

    private void construirVentana() {
        setTitle("Gestor Universal Android");
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(18, 22, 32));

        // ==========================================
        // PANEL SUPERIOR: Configuración
        // ==========================================
        JPanel panelNorte = new JPanel(new GridLayout(1, 2, 15, 0));
        panelNorte.setOpaque(false);
        panelNorte.setBorder(new EmptyBorder(15, 15, 5, 15));

        // Sección Dispositivo
        JPanel panelDisp = UIFactory.crearContenedorSeccion();
        panelDisp.add(UIFactory.crearEncabezado("📱", "Dispositivo ADB", "Selecciona el equipo a gestionar"), BorderLayout.NORTH);

        JPanel controlesDisp = new JPanel(new BorderLayout(10, 0));
        controlesDisp.setOpaque(false);

        menuDispositivos = new JComboBox<>();
        menuDispositivos.setRenderer(new DispositivoRenderer());

        JButton btnRefrescar = UIFactory.crearBotonSecundario("🔄 Refrescar", new Dimension(115, 35));
        btnRefrescar.addActionListener(e -> actualizarDispositivos());

        controlesDisp.add(menuDispositivos, BorderLayout.CENTER);
        controlesDisp.add(btnRefrescar, BorderLayout.EAST);
        panelDisp.add(controlesDisp, BorderLayout.CENTER);

        // Sección Aplicación
        JPanel panelApp = UIFactory.crearContenedorSeccion();
        panelApp.add(UIFactory.crearEncabezado("📦", "Aplicación", "Escribe el nombre del paquete"), BorderLayout.NORTH);

        JPanel controlesApp = new JPanel(new BorderLayout(10, 0));
        controlesApp.setOpaque(false);

        entradaPaquete = UIFactory.crearCampoTexto("com.ejemplo.app");

        JButton btnBuscar = UIFactory.crearBotonSecundario("🔍 Buscar Apps", new Dimension(125, 35));
        btnBuscar.addActionListener(e -> iniciarBuscadorApps());

        controlesApp.add(entradaPaquete, BorderLayout.CENTER);
        controlesApp.add(btnBuscar, BorderLayout.EAST);
        panelApp.add(controlesApp, BorderLayout.CENTER);

        panelNorte.add(panelDisp);
        panelNorte.add(panelApp);
        add(panelNorte, BorderLayout.NORTH);

        // ==========================================
        // PANEL CENTRAL: Acciones en Pestañas
        // ==========================================
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setOpaque(false);
        panelCentral.setBorder(new EmptyBorder(5, 15, 5, 15));

        JTabbedPane panelPestanas = new JTabbedPane();
        panelPestanas.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        panelPestanas.setFocusable(false);

        // Pestaña 1: Aplicaciones
        JPanel gridApps = new JPanel(new GridLayout(4, 2, 15, 15));
        gridApps.setOpaque(false);
        gridApps.setBorder(new EmptyBorder(15, 15, 15, 15));

        gridApps.add(new BotonTarjeta("⬇️", "Extraer a PC", "Copia la app del dispositivo a tu PC.", new Color(33, 150, 243), e -> iniciarExtraccion()));
        gridApps.add(new BotonTarjeta("⬆️", "Instalar desde PC", "Instala una app desde tu PC al dispositivo.", new Color(33, 150, 243), e -> iniciarInstalacion()));
        gridApps.add(new BotonTarjeta("▶", "Ejecutar App", "Inicia la aplicación en el dispositivo.", new Color(156, 39, 176), e -> iniciarEjecucion()));
        gridApps.add(new BotonTarjeta("⚙️", "Alinear APK", "Compara y alinea el APK.", new Color(156, 39, 176), e -> iniciarAlineacion()));
        gridApps.add(new BotonTarjeta("🗑️", "Desinstalar App", "Desinstala la aplicación del dispositivo.", new Color(244, 67, 54), e -> iniciarDesinstalacion()));
        gridApps.add(new BotonTarjeta("🧹", "Limpiar Datos", "Elimina datos y caché de la aplicación.", new Color(244, 67, 54), e -> iniciarLimpiezaDatos()));
        gridApps.add(new BotonTarjeta("🔑", "Firmar (Debug)", "Firma el APK con certificado de depuración.", new Color(255, 193, 7), e -> iniciarFirma()));

        panelPestanas.addTab("📦  Gestión de Apps", gridApps);

        // Pestaña 2: Herramientas de Sistema (Fila simétrica de 3 tarjetas)
        JPanel panelContenedorSistema = new JPanel(new BorderLayout());
        panelContenedorSistema.setOpaque(false);

        JPanel gridSistema = new JPanel(new GridLayout(1, 3, 15, 15));
        gridSistema.setOpaque(false);
        gridSistema.setBorder(new EmptyBorder(15, 15, 15, 15));

        gridSistema.add(new BotonTarjeta("📱", "Espejo (Scrcpy)", "Transmite la pantalla a 60 FPS nativo.", new Color(0, 188, 212), e -> iniciarScrcpy()));
        gridSistema.add(new BotonTarjeta("📸", "Capturar Pantalla", "Guarda imagen PNG en tu PC.", new Color(233, 30, 99), e -> iniciarCaptura()));
        gridSistema.add(new BotonTarjeta("📝", "Visor Logcat", "Registros del sistema en vivo.", new Color(76, 175, 80), e -> abrirVentanaLogcat()));

        panelContenedorSistema.add(gridSistema, BorderLayout.NORTH);
        panelPestanas.addTab("⚙️  Herramientas de Sistema", panelContenedorSistema);

        panelCentral.add(panelPestanas, BorderLayout.CENTER);
        add(panelCentral, BorderLayout.CENTER);

        // ==========================================
        // PANEL INFERIOR: Consola de Registro
        // ==========================================
        JPanel panelSur = new JPanel(new BorderLayout(0, 10));
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(5, 15, 15, 15));

        JPanel encabezadoSur = new JPanel(new BorderLayout());
        encabezadoSur.setOpaque(false);
        encabezadoSur.add(UIFactory.crearEncabezado("📄", "Registro del Sistema", "Aquí se muestran los mensajes y el estado."), BorderLayout.WEST);

        JPanel botonesSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        botonesSur.setOpaque(false);

        // Configuración visual de la casilla de verificación
        chkMostrarComandos = new JCheckBox("Ver comandos ADB");
        chkMostrarComandos.setOpaque(false);
        chkMostrarComandos.setForeground(new Color(180, 205, 235));
        chkMostrarComandos.setFont(new Font("Segoe UI", Font.BOLD, 12));
        chkMostrarComandos.setFocusPainted(false);
        chkMostrarComandos.setToolTipText("Muestra en la consola el comando nativo equivalente");

        JButton btnCopiar = UIFactory.crearBotonSecundario("📋 Copiar", new Dimension(100, 30));
        btnCopiar.addActionListener(e -> copiarConsola());

        JButton btnLimpiar = UIFactory.crearBotonSecundario("🗑️ Limpiar", new Dimension(100, 30));
        btnLimpiar.addActionListener(e -> consolaMensajes.setText(""));

        botonesSur.add(chkMostrarComandos);
        botonesSur.add(btnCopiar);
        botonesSur.add(btnLimpiar);
        encabezadoSur.add(botonesSur, BorderLayout.EAST);

        consolaMensajes = new JTextPane();
        consolaMensajes.setEditable(false);
        consolaMensajes.setBackground(new Color(16, 20, 30));
        consolaMensajes.setFont(new Font("Consolas", Font.PLAIN, 13));
        consolaMensajes.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane scrollConsola = new JScrollPane(consolaMensajes);
        scrollConsola.setPreferredSize(new Dimension(0, 150));
        scrollConsola.setBorder(BorderFactory.createLineBorder(new Color(45, 55, 75), 1));

        panelSur.add(encabezadoSur, BorderLayout.NORTH);
        panelSur.add(scrollConsola, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        actualizarDispositivos();
        setVisible(true);
    }

    private void registrarMensaje(String texto) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = consolaMensajes.getStyledDocument();
                Style estilo = consolaMensajes.addStyle("ColorStyle", null);

                String timestamp = "[" + formatoHora.format(new Date()) + "] ";

                if (texto.startsWith("✅") || texto.startsWith("Descargando:") || texto.startsWith("📦")) {
                    StyleConstants.setForeground(estilo, new Color(76, 175, 80));
                } else if (texto.startsWith("❌") || texto.startsWith("⚠")) {
                    StyleConstants.setForeground(estilo, new Color(244, 67, 54));
                } else if (texto.startsWith("💻")) {
                    // Color cian destacado para comandos de consola
                    StyleConstants.setForeground(estilo, new Color(77, 208, 225));
                } else {
                    StyleConstants.setForeground(estilo, new Color(220, 220, 220));
                }

                doc.insertString(doc.getLength(), timestamp + texto + "\n", estilo);
                consolaMensajes.setCaretPosition(doc.getLength());
            } catch (Exception ignored) {}
        });
    }

    /**
     * Muestra el comando en consola si el usuario marcó la casilla.
     */
    private void mostrarComandoSiActivo(String comando) {
        if (chkMostrarComandos != null && chkMostrarComandos.isSelected()) {
            registrarMensaje("💻 CMD: " + comando);
        }
    }

    private void copiarConsola() {
        String texto = consolaMensajes.getText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
    }

    private void actualizarDispositivos() {
        mostrarComandoSiActivo("adb devices -l");
        adbService.buscarDispositivos(
                (List<Dispositivo> lista) -> {
                    SwingUtilities.invokeLater(() -> {
                        menuDispositivos.removeAllItems();
                        if (lista.isEmpty()) {
                            menuDispositivos.addItem(new Dispositivo("Ninguno", "Desconectado", "Ninguno"));
                        } else {
                            for (Dispositivo d : lista) menuDispositivos.addItem(d);
                        }
                    });
                },
                this::registrarMensaje
        );
    }

    private Dispositivo obtenerDispositivoValido() {
        Dispositivo d = (Dispositivo) menuDispositivos.getSelectedItem();
        if (d == null || d.getId().equals("Ninguno")) {
            registrarMensaje("❌ Error: Selecciona un dispositivo conectado.");
            return null;
        }
        return d;
    }

    private void iniciarBuscadorApps() {
        Dispositivo d = obtenerDispositivoValido();
        if (d == null) return;

        mostrarComandoSiActivo("adb -s " + d.getId() + " shell pm list packages -3");
        adbService.listarPaquetes(d, (List<String> paquetes) -> {
            SwingUtilities.invokeLater(() -> {
                if (paquetes.isEmpty()) {
                    registrarMensaje("❌ No hay aplicaciones de terceros para mostrar.");
                    return;
                }
                mostrarVentanaBuscador(paquetes);
                registrarMensaje("✅ Búsqueda terminada. " + paquetes.size() + " apps encontradas.");
            });
        }, this::registrarMensaje);
    }

    /**
     * Elimina físicamente del disco local todas las imágenes PNG guardadas en cache_iconos.
     */
    private void vaciarCacheDisco() {
        File carpeta = new File("cache_iconos");
        if (carpeta.exists() && carpeta.isDirectory()) {
            // Filtramos únicamente los archivos que terminen en .png
            File[] archivos = carpeta.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (archivos != null) {
                for (File f : archivos) {
                    f.delete();
                }
            }
        }
    }

    /**
     * Construye y muestra la ventana modal para buscar aplicaciones, incluyendo
     * campo de filtro, botón para limpiar caché, carga en segundo plano y contador.
     *
     * @param paquetesTotales Lista con los paquetes instalados obtenidos desde ADB.
     */
    private void mostrarVentanaBuscador(List<String> paquetesTotales) {
        Dispositivo dispositivoActual = obtenerDispositivoValido();
        if (dispositivoActual == null) return;

        // 1. Configuración de la ventana modal
        Window parentWindow = SwingUtilities.getWindowAncestor(entradaPaquete);
        JDialog dialogo = new JDialog((Frame) parentWindow, "Buscar Aplicación", true);
        dialogo.setSize(540, 600);
        dialogo.setLayout(new BorderLayout(10, 10));
        dialogo.setLocationRelativeTo(parentWindow);
        dialogo.getContentPane().setBackground(new Color(18, 22, 32));

        // 2. Panel superior: Barra de búsqueda y botón de limpiar caché
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));
        panelSuperior.setOpaque(false);
        panelSuperior.setBorder(new EmptyBorder(15, 15, 0, 15));

        JTextField campoFiltro = UIFactory.crearCampoTexto("Escribe para filtrar (ej. whatsapp)...");
        JButton btnLimpiarCache = UIFactory.crearBotonSecundario("🧹 Limpiar Caché", new Dimension(145, 35));
        btnLimpiarCache.setToolTipText("Elimina los iconos descargados y vuelve a consultarlos desde el dispositivo");

        panelSuperior.add(campoFiltro, BorderLayout.CENTER);
        panelSuperior.add(btnLimpiarCache, BorderLayout.EAST);

        // 3. Preparación de los datos iniciales
        List<com.rsec.gestorapp.model.AppInfo> appsTotales = new ArrayList<>();
        for (String pkg : paquetesTotales) {
            Icon iconoCache = cacheIconos.get(pkg);
            appsTotales.add(new com.rsec.gestorapp.model.AppInfo(pkg, iconoCache));
        }

        DefaultListModel<com.rsec.gestorapp.model.AppInfo> modeloLista = new DefaultListModel<>();
        appsTotales.forEach(modeloLista::addElement);

        // 4. Configuración del componente visual JList
        JList<com.rsec.gestorapp.model.AppInfo> listaPaquetes = new JList<>(modeloLista);
        listaPaquetes.setCellRenderer(new com.rsec.gestorapp.ui.components.AppListRenderer());
        listaPaquetes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaPaquetes.setBackground(new Color(18, 22, 32));

        JScrollPane scrollLista = new JScrollPane(listaPaquetes);
        scrollLista.setBorder(BorderFactory.createLineBorder(new Color(35, 45, 65), 1));
        scrollLista.setOpaque(false);
        scrollLista.getViewport().setOpaque(false);

        // 5. Panel inferior: Instrucciones y contador discreto de avance
        JPanel panelInferior = new JPanel(new BorderLayout(10, 0));
        panelInferior.setOpaque(false);
        panelInferior.setBorder(new EmptyBorder(5, 15, 15, 15));

        JLabel etiquetaInstruccion = new JLabel("Haz doble clic para seleccionar");
        etiquetaInstruccion.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        etiquetaInstruccion.setForeground(new Color(130, 140, 160));

        JLabel lblContador = new JLabel("Cargando iconos: 0/" + appsTotales.size());
        lblContador.setFont(new Font("Segoe UI Emoji", Font.BOLD, 12));
        lblContador.setForeground(new Color(77, 208, 225));

        panelInferior.add(etiquetaInstruccion, BorderLayout.WEST);
        panelInferior.add(lblContador, BorderLayout.EAST);

        // 6. Rutina asíncrona para descargar iconos en segundo plano
        Runnable tareaCargarIconos = () -> new Thread(() -> {
            int total = appsTotales.size();
            int procesados = 0;

            for (com.rsec.gestorapp.model.AppInfo app : appsTotales) {
                String pkg = app.getPaquete();

                // Si no se encuentra en memoria caché, consultamos el servicio
                if (!cacheIconos.containsKey(pkg)) {
                    Icon iconoReal = adbService.extraerIconoApp(dispositivoActual, pkg);
                    if (iconoReal != null) {
                        cacheIconos.put(pkg, iconoReal);
                        app.setIcono(iconoReal);
                        SwingUtilities.invokeLater(listaPaquetes::repaint);
                    } else {
                        // Caché negativa para aplicaciones sin imagen PNG extraíble
                        cacheIconos.put(pkg, com.rsec.gestorapp.ui.components.AppListRenderer.getIconoProvisional());
                    }
                } else {
                    app.setIcono(cacheIconos.get(pkg));
                }

                procesados++;
                int avance = procesados;

                SwingUtilities.invokeLater(() -> {
                    if (avance < total) {
                        lblContador.setText("Cargando iconos: " + avance + "/" + total);
                        lblContador.setForeground(new Color(77, 208, 225));
                    } else {
                        lblContador.setText("✔️ Iconos listos (" + total + ")");
                        lblContador.setForeground(new Color(76, 175, 80));
                        btnLimpiarCache.setEnabled(true);
                    }
                });
            }
        }).start();

        // 7. Acción del botón "Limpiar Caché"
        btnLimpiarCache.addActionListener(e -> {
            btnLimpiarCache.setEnabled(false);
            vaciarCacheDisco();
            cacheIconos.clear();

            // Restablecer iconos provisionales en los elementos de la lista
            for (com.rsec.gestorapp.model.AppInfo app : appsTotales) {
                app.setIcono(null);
            }
            listaPaquetes.repaint();

            lblContador.setText("Caché borrada. Recargando...");
            lblContador.setForeground(new Color(255, 193, 7)); // Color ámbar durante la purga

            // Iniciar de nuevo la descarga en segundo plano
            tareaCargarIconos.run();
        });

        // 8. Evento de filtrado en tiempo real al escribir
        campoFiltro.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filtrar(); }
            public void removeUpdate(DocumentEvent e) { filtrar(); }
            public void changedUpdate(DocumentEvent e) { filtrar(); }
            private void filtrar() {
                String texto = campoFiltro.getText().toLowerCase().trim();
                modeloLista.clear();
                for (com.rsec.gestorapp.model.AppInfo app : appsTotales) {
                    if (app.getPaquete().toLowerCase().contains(texto) ||
                            app.getNombreVisible().toLowerCase().contains(texto)) {
                        modeloLista.addElement(app);
                    }
                }
            }
        });

        // 9. Evento de selección por doble clic
        listaPaquetes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    com.rsec.gestorapp.model.AppInfo seleccion = listaPaquetes.getSelectedValue();
                    if (seleccion != null) {
                        entradaPaquete.setText(seleccion.getPaquete());
                        dialogo.dispose();
                        registrarMensaje("✅ App seleccionada: " + seleccion.getPaquete());
                    }
                }
            }
        });

        // 10. Ensamblado final y lanzamiento
        dialogo.add(panelSuperior, BorderLayout.NORTH);
        dialogo.add(scrollLista, BorderLayout.CENTER);
        dialogo.add(panelInferior, BorderLayout.SOUTH);

        // Lanzar la descarga inicial y abrir el diálogo
        tareaCargarIconos.run();
        dialogo.setVisible(true);
    }

    private void iniciarExtraccion() {
        String paquete = entradaPaquete.getText().trim();
        Dispositivo d = obtenerDispositivoValido();
        if (d == null || paquete.isEmpty()) {
            registrarMensaje("⚠ Error: Escribe el nombre del paquete primero.");
            return;
        }

        mostrarComandoSiActivo("adb -s " + d.getId() + " shell pm path " + paquete + " && adb -s " + d.getId() + " pull <ruta_base.apk> <destino>");
        JFileChooser selector = new JFileChooser();
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            adbService.extraerPaquete(paquete, d, selector.getSelectedFile(), this::registrarMensaje);
        }
    }

    /**
     * Inicia la instalación con detección automática de Splits.
     * El usuario solo hace clic en un archivo y el sistema reúne el resto automáticamente.
     */
    private void iniciarInstalacion() {
        Dispositivo d = obtenerDispositivoValido();
        if (d == null) return;

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona el APK (se detectarán los splits automáticamente)");
        selector.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selector.setMultiSelectionEnabled(false); // Solo requiere un clic normal del usuario

        // Filtramos para mostrar únicamente archivos APK
        selector.setFileFilter(new FileNameExtensionFilter("Archivos de Aplicación Android (*.apk)", "apk"));

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoElegido = selector.getSelectedFile();
            if (archivoElegido == null || !archivoElegido.exists()) {
                registrarMensaje("⚠ No se seleccionó ningún archivo válido.");
                return;
            }

            // Detección automática de todos los splits en la misma carpeta
            List<File> apksAInstalar = recolectarSplitsAutomaticamente(archivoElegido);

            // Registro didáctico del comando ADB equivalente
            if (apksAInstalar.size() == 1) {
                mostrarComandoSiActivo("adb -s " + d.getId() + " install -r -d \"" + apksAInstalar.get(0).getName() + "\"");
                registrarMensaje("📦 Instalando APK único: " + apksAInstalar.get(0).getName());
            } else {
                mostrarComandoSiActivo("adb -s " + d.getId() + " install-multiple -r -d [ " + apksAInstalar.size() + " archivos APK splits ]");
                registrarMensaje("📦 Detectados automáticamente " + apksAInstalar.size() + " Split APKs. Iniciando instalación combinada...");
            }

            // Llamada al servicio en segundo plano
            adbService.instalarApks(apksAInstalar, d, this::registrarMensaje);
        }
    }

    /**
     * Inspecciona la carpeta del APK seleccionado. Si contiene múltiples archivos .apk,
     * los agrupa todos de manera automática asegurando que 'base.apk' sea el primero.
     *
     * @param archivoElegido El archivo .apk sobre el cual el usuario hizo clic.
     * @return Lista completa de APKs a instalar juntos.
     */
    private List<File> recolectarSplitsAutomaticamente(File archivoElegido) {
        List<File> resultado = new java.util.ArrayList<>();
        File carpetaContenedora = archivoElegido.getParentFile();

        if (carpetaContenedora != null && carpetaContenedora.isDirectory()) {
            // Obtenemos todos los archivos que terminen en .apk dentro de la misma carpeta
            File[] todosLosApks = carpetaContenedora.listFiles((dir, name) -> name.toLowerCase().endsWith(".apk"));

            if (todosLosApks != null && todosLosApks.length > 1) {
                // Hay múltiples APKs (caso de Split APKs)
                File baseApk = null;
                List<File> configs = new java.util.ArrayList<>();

                for (File apk : todosLosApks) {
                    // Identificamos el APK base principal
                    if (apk.getName().equalsIgnoreCase("base.apk")) {
                        baseApk = apk;
                    } else {
                        configs.add(apk);
                    }
                }

                // El APK base debe ser el primero de la lista si existe
                if (baseApk != null) {
                    resultado.add(baseApk);
                }
                resultado.addAll(configs);

                return resultado;
            }
        }

        // Si es una aplicación simple (un solo APK), solo se añade ese archivo
        resultado.add(archivoElegido);
        return resultado;
    }

    private void iniciarAlineacion() {
        JFileChooser selectorEntrada = new JFileChooser();
        selectorEntrada.setFileFilter(new FileNameExtensionFilter("Archivo APK", "apk"));
        if (selectorEntrada.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return;

        JFileChooser selectorSalida = new JFileChooser();
        selectorSalida.setSelectedFile(new File("alineado.apk"));
        if (selectorSalida.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        mostrarComandoSiActivo("zipalign -v -p 4 " + selectorEntrada.getSelectedFile().getName() + " " + selectorSalida.getSelectedFile().getName());
        adbService.alinearApk(selectorEntrada.getSelectedFile(), selectorSalida.getSelectedFile(), this::registrarMensaje);
    }

    private void iniciarFirma() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter("Archivo APK", "apk"));
        if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            mostrarComandoSiActivo("apksigner sign --ks debug.keystore --ks-pass pass:android " + selector.getSelectedFile().getName());
            adbService.firmarApkDebug(selector.getSelectedFile(), this::registrarMensaje);
        }
    }

    private void iniciarDesinstalacion() {
        String paquete = entradaPaquete.getText().trim();
        Dispositivo d = obtenerDispositivoValido();
        if (d == null || paquete.isEmpty()) {
            registrarMensaje("⚠ Error: Escribe el nombre del paquete primero.");
            return;
        }

        mostrarComandoSiActivo("adb -s " + d.getId() + " uninstall " + paquete);
        adbService.desinstalarApp(paquete, d, this::registrarMensaje);
    }

    private void iniciarEjecucion() {
        String paquete = entradaPaquete.getText().trim();
        Dispositivo d = obtenerDispositivoValido();
        if (d == null || paquete.isEmpty()) {
            registrarMensaje("⚠ Error: Escribe el nombre del paquete primero.");
            return;
        }

        mostrarComandoSiActivo("adb -s " + d.getId() + " shell monkey -p " + paquete + " -c android.intent.category.LAUNCHER 1");
        adbService.ejecutarApp(paquete, d, this::registrarMensaje);
    }

    private void iniciarLimpiezaDatos() {
        String paquete = entradaPaquete.getText().trim();
        Dispositivo d = obtenerDispositivoValido();
        if (d == null || paquete.isEmpty()) {
            registrarMensaje("⚠ Error: Escribe el nombre del paquete primero.");
            return;
        }

        mostrarComandoSiActivo("adb -s " + d.getId() + " shell pm clear " + paquete);
        adbService.limpiarDatosApp(paquete, d, this::registrarMensaje);
    }

    private void iniciarScrcpy() {
        Dispositivo d = obtenerDispositivoValido();

        if (d == null) return;
        mostrarComandoSiActivo("scrcpy -s " + d.getId() + " --max-fps=60");
        adbService.iniciarScrcpyNativo(d, this::registrarMensaje);
    }

    private void iniciarCaptura() {
        Dispositivo d = obtenerDispositivoValido();
        if (d == null) return;

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Guardar captura de pantalla");
        selector.setSelectedFile(new File("captura_" + System.currentTimeMillis() + ".png"));

        if (selector.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File archivoDestino = selector.getSelectedFile();
            mostrarComandoSiActivo("adb -s " + d.getId() + " exec-out screencap -p > " + archivoDestino.getName());
            adbService.tomarCapturaPantalla(d, archivoDestino, this::registrarMensaje, () -> {
                try {
                    Image imagen = ImageIO.read(archivoDestino);
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                        public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
                        public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
                        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                            if (isDataFlavorSupported(flavor)) return imagen;
                            throw new UnsupportedFlavorException(flavor);
                        }
                    }, null);
                    registrarMensaje("📋 ¡Captura copiada al portapapeles automáticamente!");
                } catch (Exception ex) {
                    registrarMensaje("❌ Error al copiar al portapapeles: " + ex.getMessage());
                }
            });
        }
    }

    private void abrirVentanaLogcat() {
        Dispositivo d = obtenerDispositivoValido();
        if (d == null) return;

        mostrarComandoSiActivo("adb -s " + d.getId() + " logcat -v time");

        JDialog ventanaLog = new JDialog();
        ventanaLog.setTitle("Logcat En Vivo - " + d.getNombreModelo());
        ventanaLog.setSize(850, 600);
        ventanaLog.setLayout(new BorderLayout(5, 5));
        ventanaLog.setLocationRelativeTo(null);
        ventanaLog.getContentPane().setBackground(new Color(18, 22, 32));

        JPanel panelNorte = new JPanel(new BorderLayout(10, 0));
        panelNorte.setBorder(new EmptyBorder(10, 10, 5, 10));
        panelNorte.setOpaque(false);

        JTextField campoFiltro = UIFactory.crearCampoTexto("Buscar (ej. E/ para errores)...");
        panelNorte.add(campoFiltro, BorderLayout.CENTER);

        JToggleButton btnAutoScroll = new JToggleButton("⏸ Pausar Lectura");
        btnAutoScroll.setBackground(new Color(244, 67, 54));
        btnAutoScroll.setForeground(Color.WHITE);
        btnAutoScroll.setFocusPainted(false);
        panelNorte.add(btnAutoScroll, BorderLayout.EAST);

        ventanaLog.add(panelNorte, BorderLayout.NORTH);

        JTextPane areaLogs = new JTextPane();
        areaLogs.setEditable(false);
        areaLogs.setBackground(new Color(13, 17, 23));
        areaLogs.setFont(new Font("Consolas", Font.PLAIN, 12));

        DefaultCaret caret = (DefaultCaret) areaLogs.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        btnAutoScroll.addItemListener(e -> {
            if (btnAutoScroll.isSelected()) {
                caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
                btnAutoScroll.setText("▶ Reanudar Lectura");
                btnAutoScroll.setBackground(new Color(76, 175, 80));
            } else {
                caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                btnAutoScroll.setText("⏸ Pausar Lectura");
                btnAutoScroll.setBackground(new Color(244, 67, 54));
                areaLogs.setCaretPosition(areaLogs.getDocument().getLength());
            }
        });

        JScrollPane scroll = new JScrollPane(areaLogs);
        scroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        ventanaLog.add(scroll, BorderLayout.CENTER);

        StyledDocument doc = areaLogs.getStyledDocument();
        Style estiloNormal = areaLogs.addStyle("Normal", null);
        StyleConstants.setForeground(estiloNormal, new Color(200, 200, 200));
        Style estiloError = areaLogs.addStyle("Error", null);
        StyleConstants.setForeground(estiloError, new Color(244, 67, 54));
        Style estiloWarning = areaLogs.addStyle("Warning", null);
        StyleConstants.setForeground(estiloWarning, new Color(255, 193, 7));

        Process procesoLogcat = adbService.iniciarLogcat(d, linea -> {
            SwingUtilities.invokeLater(() -> {
                String filtro = campoFiltro.getText().trim().toLowerCase();
                if (!filtro.isEmpty() && !linea.toLowerCase().contains(filtro)) return;

                try {
                    Style estiloUsar = estiloNormal;
                    if (linea.contains(" E ") || linea.contains(" E/")) estiloUsar = estiloError;
                    else if (linea.contains(" W ") || linea.contains(" W/")) estiloUsar = estiloWarning;

                    doc.insertString(doc.getLength(), linea + "\n", estiloUsar);
                    if (doc.getLength() > 60000) doc.remove(0, 20000);
                } catch (Exception ignored) {}
            });
        });

        ventanaLog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (procesoLogcat != null) procesoLogcat.destroy();
                registrarMensaje("✅ Visor Logcat cerrado correctamente.");
            }
        });

        ventanaLog.setVisible(true);
        registrarMensaje("▶ Iniciando visor Logcat inteligente para " + d.getNombreModelo());
    }
}