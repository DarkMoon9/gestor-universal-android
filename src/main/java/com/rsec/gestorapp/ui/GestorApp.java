package com.rsec.gestorapp.ui;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.rsec.gestorapp.model.AppInfo;
import com.rsec.gestorapp.model.Dispositivo;
import com.rsec.gestorapp.service.ADBService;
import com.rsec.gestorapp.ui.components.AppListRenderer;
import com.rsec.gestorapp.ui.components.BotonTarjeta;
import com.rsec.gestorapp.ui.components.DispositivoRenderer;
import com.rsec.gestorapp.ui.components.UIFactory;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controlador principal y ventana de la aplicación GestorApp.
 * <p>
 * Coordina la interfaz gráfica en Java Swing con el servicio {@link ADBService},
 * proporcionando herramientas para la gestión de paquetes Android, transmisión de pantalla,
 * visor de logs en tiempo real y utilidades del sistema.
 *
 * @author Marco Vences
 * @version 1.0
 */
public class GestorApp extends JFrame {

    /**
     * Área de texto enriquecido para la consola de registros de la interfaz.
     */
    private JTextPane consolaMensajes;

    /**
     * Selector desplegable con la lista de dispositivos ADB detectados.
     */
    private JComboBox<Dispositivo> menuDispositivos;

    /**
     * Campo de entrada para el nombre de paquete objetivo (ej. "com.whatsapp").
     */
    private JTextField entradaPaquete;

    /**
     * Casilla de verificación para mostrar los comandos ADB nativos equivalentes en la consola.
     */
    private JCheckBox chkMostrarComandos;

    /**
     * Servicio de comunicación y subprocesos ADB.
     */
    private ADBService adbService;

    /**
     * Formateador de hora para los marcas de tiempo (timestamps) de la consola.
     */
    private SimpleDateFormat formatoHora = new SimpleDateFormat("HH:mm:ss");

    /**
     * Mapa concurrente en memoria RAM para almacenar temporalmente los iconos de las aplicaciones.
     */
    private Map<String, Icon> cacheIconos = new ConcurrentHashMap<>();

    /**
     * Constructor por defecto. Inicializa la instancia del servicio de comunicaciones ADB.
     */
    public GestorApp() {
        this.adbService = new ADBService();
    }

    /**
     * Punto de entrada principal para la ejecución de la aplicación.
     * Configura el tema visual oscuro FlatLaf e inicia la ventana en el hilo de eventos de Swing (EDT).
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Personalización global de curvatura para componentes de FlatLaf
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

    /**
     * Construye y ensambla todos los paneles y componentes de la ventana principal.
     */
    private void construirVentana() {
        setTitle("Gestor Universal Android");
        try {
            // Carga la imagen desde la carpeta de recursos del proyecto
            java.net.URL urlIcono = getClass().getResource("/android.png");
            if (urlIcono != null) {
                Image iconoApp = Toolkit.getDefaultToolkit().getImage(urlIcono);
                setIconImage(iconoApp); // Establece el icono en la ventana y la barra de tareas
            }
        } catch (Exception e) {
            System.err.println("No se pudo cargar el icono de la aplicación: " + e.getMessage());
        }
        setSize(950, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(new Color(18, 22, 32));

        // ==========================================
        // PANEL SUPERIOR: Configuración de Dispositivo y App
        // ==========================================
        JPanel panelNorte = new JPanel(new GridLayout(1, 2, 15, 0));
        panelNorte.setOpaque(false);
        panelNorte.setBorder(new EmptyBorder(15, 15, 5, 15));

        // Sección Dispositivo ADB
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

        // Sección Selección de Aplicación
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
        // PANEL CENTRAL: Acciones organizadas en Pestañas
        // ==========================================
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setOpaque(false);
        panelCentral.setBorder(new EmptyBorder(5, 15, 5, 15));

        JTabbedPane panelPestanas = new JTabbedPane();
        panelPestanas.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        panelPestanas.setFocusable(false);

        // Pestaña 1: Gestión de Aplicaciones
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

        // Pestaña 2: Herramientas de Sistema
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
        // PANEL INFERIOR: Consola de Registro e Historial
        // ==========================================
        JPanel panelSur = new JPanel(new BorderLayout(0, 10));
        panelSur.setOpaque(false);
        panelSur.setBorder(new EmptyBorder(5, 15, 15, 15));

        JPanel encabezadoSur = new JPanel(new BorderLayout());
        encabezadoSur.setOpaque(false);
        encabezadoSur.add(UIFactory.crearEncabezado("📄", "Registro del Sistema", "Aquí se muestran los mensajes y el estado."), BorderLayout.WEST);

        JPanel botonesSur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        botonesSur.setOpaque(false);

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

        // Carga inicial de dispositivos al abrir la ventana
        actualizarDispositivos();
        setVisible(true);
    }

    /**
     * Registra un mensaje formateado con la hora actual en la consola de la UI.
     * Asigna colores según el nivel del mensaje (éxito, error, comando CMD, etc.).
     *
     * @param texto Cadena de texto a mostrar en el panel de logs.
     */
    private void registrarMensaje(String texto) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = consolaMensajes.getStyledDocument();
                Style estilo = consolaMensajes.addStyle("ColorStyle", null);

                String timestamp = "[" + formatoHora.format(new Date()) + "] ";

                if (texto.startsWith("✅") || texto.startsWith("Descargando:") || texto.startsWith("📦")) {
                    StyleConstants.setForeground(estilo, new Color(76, 175, 80)); // Verde éxito
                } else if (texto.startsWith("❌") || texto.startsWith("⚠")) {
                    StyleConstants.setForeground(estilo, new Color(244, 67, 54)); // Rojo error
                } else if (texto.startsWith("💻")) {
                    StyleConstants.setForeground(estilo, new Color(77, 208, 225)); // Cian comandos
                } else {
                    StyleConstants.setForeground(estilo, new Color(220, 220, 220)); // Gris claro normal
                }

                doc.insertString(doc.getLength(), timestamp + texto + "\n", estilo);
                consolaMensajes.setCaretPosition(doc.getLength());
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Publica el comando ADB ejecutado en la consola si la casilla correspondiente está marcada.
     *
     * @param comando Cadena con el comando nativo.
     */
    private void mostrarComandoSiActivo(String comando) {
        if (chkMostrarComandos != null && chkMostrarComandos.isSelected()) {
            registrarMensaje("💻 CMD: " + comando);
        }
    }

    /**
     * Copia el texto completo de la consola de mensajes al portapapeles del sistema operativo.
     */
    private void copiarConsola() {
        String texto = consolaMensajes.getText();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(texto), null);
    }

    /**
     * Llama al servicio ADB para escanear y actualizar la lista de dispositivos en el selector.
     */
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

    /**
     * Obtiene el dispositivo actualmente seleccionado en el combo desplegable.
     *
     * @return Objeto {@link Dispositivo} seleccionado o {@code null} si no hay dispositivos conectados.
     */
    private Dispositivo obtenerDispositivoValido() {
        Dispositivo d = (Dispositivo) menuDispositivos.getSelectedItem();
        if (d == null || d.getId().equals("Ninguno")) {
            registrarMensaje("❌ Error: Selecciona un dispositivo conectado.");
            return null;
        }
        return d;
    }

    /**
     * Inicia la consulta de aplicaciones de terceros y abre la ventana modal de búsqueda.
     */
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
     * Elimina físicamente del disco local todas las imágenes PNG guardadas en la carpeta 'cache_iconos'.
     */
    private void vaciarCacheDisco() {
        File carpeta = new File("cache_iconos");
        if (carpeta.exists() && carpeta.isDirectory()) {
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

        Window parentWindow = SwingUtilities.getWindowAncestor(entradaPaquete);
        JDialog dialogo = new JDialog((Frame) parentWindow, "Buscar Aplicación", true);
        dialogo.setSize(540, 600);
        dialogo.setLayout(new BorderLayout(10, 10));
        dialogo.setLocationRelativeTo(parentWindow);
        dialogo.getContentPane().setBackground(new Color(18, 22, 32));

        // Panel superior: Barra de búsqueda y botón de limpiar caché
        JPanel panelSuperior = new JPanel(new BorderLayout(10, 0));
        panelSuperior.setOpaque(false);
        panelSuperior.setBorder(new EmptyBorder(15, 15, 0, 15));

        JTextField campoFiltro = UIFactory.crearCampoTexto("Escribe para filtrar (ej. whatsapp)...");
        JButton btnLimpiarCache = UIFactory.crearBotonSecundario("🧹 Limpiar Caché", new Dimension(145, 35));
        btnLimpiarCache.setToolTipText("Elimina los iconos descargados y vuelve a consultarlos desde el dispositivo");

        panelSuperior.add(campoFiltro, BorderLayout.CENTER);
        panelSuperior.add(btnLimpiarCache, BorderLayout.EAST);

        // Preparación del modelo de datos de aplicaciones
        List<AppInfo> appsTotales = new ArrayList<>();
        for (String pkg : paquetesTotales) {
            Icon iconoCache = cacheIconos.get(pkg);
            appsTotales.add(new AppInfo(pkg, iconoCache));
        }

        DefaultListModel<AppInfo> modeloLista = new DefaultListModel<>();
        appsTotales.forEach(modeloLista::addElement);

        JList<AppInfo> listaPaquetes = new JList<>(modeloLista);
        listaPaquetes.setCellRenderer(new AppListRenderer());
        listaPaquetes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaPaquetes.setBackground(new Color(18, 22, 32));

        JScrollPane scrollLista = new JScrollPane(listaPaquetes);
        scrollLista.setBorder(BorderFactory.createLineBorder(new Color(35, 45, 65), 1));
        scrollLista.setOpaque(false);
        scrollLista.getViewport().setOpaque(false);

        // Panel inferior: Contador y progreso
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

        // Tarea en segundo plano para cargar los iconos
        Runnable tareaCargarIconos = () -> new Thread(() -> {
            int total = appsTotales.size();
            int procesados = 0;

            for (AppInfo app : appsTotales) {
                String pkg = app.getPaquete();

                if (!cacheIconos.containsKey(pkg)) {
                    Icon iconoReal = adbService.extraerIconoApp(dispositivoActual, pkg);
                    if (iconoReal != null) {
                        cacheIconos.put(pkg, iconoReal);
                        app.setIcono(iconoReal);
                        SwingUtilities.invokeLater(listaPaquetes::repaint);
                    } else {
                        cacheIconos.put(pkg, AppListRenderer.getIconoProvisional());
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

        // Acción del botón para purgar la caché
        btnLimpiarCache.addActionListener(e -> {
            btnLimpiarCache.setEnabled(false);
            vaciarCacheDisco();
            cacheIconos.clear();

            for (AppInfo app : appsTotales) {
                app.setIcono(null);
            }
            listaPaquetes.repaint();

            lblContador.setText("Caché borrada. Recargando...");
            lblContador.setForeground(new Color(255, 193, 7));

            tareaCargarIconos.run();
        });

        // Filtrado en tiempo real
        campoFiltro.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }

            private void filtrar() {
                String texto = campoFiltro.getText().toLowerCase().trim();
                modeloLista.clear();
                for (AppInfo app : appsTotales) {
                    if (app.getPaquete().toLowerCase().contains(texto) ||
                            app.getNombreVisible().toLowerCase().contains(texto)) {
                        modeloLista.addElement(app);
                    }
                }
            }
        });

        // Evento de selección con doble clic
        listaPaquetes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    AppInfo seleccion = listaPaquetes.getSelectedValue();
                    if (seleccion != null) {
                        entradaPaquete.setText(seleccion.getPaquete());
                        dialogo.dispose();
                        registrarMensaje("✅ App seleccionada: " + seleccion.getPaquete());
                    }
                }
            }
        });

        dialogo.add(panelSuperior, BorderLayout.NORTH);
        dialogo.add(scrollLista, BorderLayout.CENTER);
        dialogo.add(panelInferior, BorderLayout.SOUTH);

        tareaCargarIconos.run();
        dialogo.setVisible(true);
    }

    /**
     * Inicia el proceso de extracción de los archivos APK de la aplicación seleccionada hacia el PC.
     */
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
     * Inicia la instalación con detección automática de partes divididas (Splits).
     */
    private void iniciarInstalacion() {
        Dispositivo d = obtenerDispositivoValido();
        if (d == null) return;

        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Selecciona el APK (se detectarán los splits automáticamente)");
        selector.setFileSelectionMode(JFileChooser.FILES_ONLY);
        selector.setMultiSelectionEnabled(false);
        selector.setFileFilter(new FileNameExtensionFilter("Archivos de Aplicación Android (*.apk)", "apk"));

        if (selector.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivoElegido = selector.getSelectedFile();
            if (archivoElegido == null || !archivoElegido.exists()) {
                registrarMensaje("⚠ No se seleccionó ningún archivo válido.");
                return;
            }

            List<File> apksAInstalar = recolectarSplitsAutomaticamente(archivoElegido);

            if (apksAInstalar.size() == 1) {
                mostrarComandoSiActivo("adb -s " + d.getId() + " install -r -d \"" + apksAInstalar.get(0).getName() + "\"");
                registrarMensaje("📦 Instalando APK único: " + apksAInstalar.get(0).getName());
            } else {
                mostrarComandoSiActivo("adb -s " + d.getId() + " install-multiple -r -d [ " + apksAInstalar.size() + " archivos APK splits ]");
                registrarMensaje("📦 Detectados automáticamente " + apksAInstalar.size() + " Split APKs. Iniciando instalación combinada...");
            }

            adbService.instalarApks(apksAInstalar, d, this::registrarMensaje);
        }
    }

    /**
     * Inspecciona la carpeta del APK elegido para agrupar automáticamente archivos Split APKs.
     *
     * @param archivoElegido El archivo .apk sobre el cual se hizo clic.
     * @return Lista completa de APKs a instalar juntos.
     */
    private List<File> recolectarSplitsAutomaticamente(File archivoElegido) {
        List<File> resultado = new ArrayList<>();
        File carpetaContenedora = archivoElegido.getParentFile();

        if (carpetaContenedora != null && carpetaContenedora.isDirectory()) {
            File[] todosLosApks = carpetaContenedora.listFiles((dir, name) -> name.toLowerCase().endsWith(".apk"));

            if (todosLosApks != null && todosLosApks.length > 1) {
                File baseApk = null;
                List<File> configs = new ArrayList<>();

                for (File apk : todosLosApks) {
                    if (apk.getName().equalsIgnoreCase("base.apk")) {
                        baseApk = apk;
                    } else {
                        configs.add(apk);
                    }
                }

                if (baseApk != null) {
                    resultado.add(baseApk);
                }
                resultado.addAll(configs);

                return resultado;
            }
        }

        resultado.add(archivoElegido);
        return resultado;
    }

    /**
     * Ejecuta el comando de alineación de bloques de memoria mediante `zipalign`.
     */
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

    /**
     * Ejecuta la firma del APK utilizando el certificado de depuración mediante `apksigner`.
     */
    private void iniciarFirma() {
        JFileChooser selector = new JFileChooser();
        selector.setFileFilter(new FileNameExtensionFilter("Archivo APK", "apk"));
        if (selector.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            mostrarComandoSiActivo("apksigner sign --ks debug.keystore --ks-pass pass:android " + selector.getSelectedFile().getName());
            adbService.firmarApkDebug(selector.getSelectedFile(), this::registrarMensaje);
        }
    }

    /**
     * Solicita la desinstalación de la aplicación especificada en el campo de paquete.
     */
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

    /**
     * Inicia la actividad principal de la aplicación seleccionada mediante la herramienta `monkey`.
     */
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

    /**
     * Borra la memoria caché y datos de usuario de la aplicación objetivo.
     */
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

    /**
     * Inicia la transmisión de pantalla en tiempo real utilizando la utilidad Scrcpy.
     */
    private void iniciarScrcpy() {
        Dispositivo d = obtenerDispositivoValido();

        if (d == null) return;
        mostrarComandoSiActivo("scrcpy -s " + d.getId() + " --max-fps=60");
        adbService.iniciarScrcpyNativo(d, this::registrarMensaje);
    }

    /**
     * Captura la pantalla del dispositivo, la guarda en disco y la copia al portapapeles del sistema.
     */
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
                        public DataFlavor[] getTransferDataFlavors() {
                            return new DataFlavor[]{DataFlavor.imageFlavor};
                        }

                        public boolean isDataFlavorSupported(DataFlavor flavor) {
                            return DataFlavor.imageFlavor.equals(flavor);
                        }

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

    /**
     * Abre una ventana emergente en vivo para monitorear el flujo de logs (`logcat`) del dispositivo.
     */
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
                } catch (Exception ignored) {
                }
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