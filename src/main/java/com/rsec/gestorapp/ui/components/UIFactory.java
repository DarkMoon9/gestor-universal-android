package com.rsec.gestorapp.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Clase de utilidad que implementa el patrón de diseño Factory (Fábrica) para la
 * creación estandarizada de componentes gráficos reutilizables de la interfaz de usuario.
 * <p>
 * Centraliza los estilos visuales del tema oscuro de la aplicación (fuentes, bordes,
 * colores de fondo y márgenes) para garantizar coherencia en todas las ventanas.
 *
 * @author Marco Vences
 * @version 1.0
 */
public class UIFactory {

    /**
     * Constructor privado para prevenir la instanciación directa de esta clase de utilidad.
     */
    private UIFactory() {
        throw new UnsupportedOperationException("UIFactory es una clase de utilidad y no debe ser instanciada.");
    }

    /**
     * Crea y configura un botón secundario estilizado con bordes suaves,
     * tipografía compatible con emojis y cursor interactivo.
     *
     * @param texto     Texto plano o símbolo/emoji a presentar en el botón.
     * @param dimension Dimensiones preferidas (ancho y alto) del componente.
     * @return Objeto {@link JButton} listo para agregarse al panel de la interfaz.
     */
    public static JButton crearBotonSecundario(String texto, Dimension dimension) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(dimension);

        // Tipografía compatible con jeroglíficos y emojis en sistemas Windows
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btn.setBackground(new Color(30, 45, 70));            // Fondo azul marino oscuro
        btn.setForeground(Color.WHITE);                        // Texto blanco

        // Borde fino azul acero con esquinas redondeadas
        btn.setBorder(BorderFactory.createLineBorder(new Color(45, 65, 95), 1, true));
        btn.setFocusPainted(false);                            // Elimina el recuadro de enfoque por teclado
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));        // Cambia el puntero al pasar el ratón

        return btn;
    }

    /**
     * Crea un panel contenedor con fondo oscuro y un margen acolchado interno (padding)
     * para agrupar visualmente distintas secciones o formularios.
     *
     * @return Objeto {@link JPanel} con disposición {@link BorderLayout} y borde compuesto.
     */
    public static JPanel crearContenedorSeccion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 30, 45)); // Fondo azul noche principal

        // Borde compuesto: Marco exterior estilizado + Margen acolchado interno de 15x20 px
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 55, 75), 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));

        return panel;
    }

    /**
     * Construye un panel de encabezado que organiza un icono representativo a la izquierda
     * y un área de texto vertical con título y subtítulo explicativo.
     *
     * @param icono     Emoji o símbolo que representa visualmente la sección.
     * @param titulo    Texto del título principal destacado en negrita.
     * @param subtitulo Texto secundario explicativo en tono gris tenue.
     * @return Objeto {@link JPanel} transparente configurado como encabezado.
     */
    public static JPanel crearEncabezado(String icono, String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout(15, 5));
        panel.setOpaque(false); // Transparente para adaptarse al fondo del contenedor padre

        // 1. Icono visual ubicado a la izquierda
        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        // 2. Contenedor de dos filas para los textos descriptivos
        JPanel panelTextos = new JPanel(new GridLayout(2, 1));
        panelTextos.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(Color.GRAY);

        panelTextos.add(lblTitulo);
        panelTextos.add(lblSub);

        // Ensamblado en las regiones Oeste y Centro
        panel.add(lblIcono, BorderLayout.WEST);
        panel.add(panelTextos, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Crea un campo de entrada de texto estilizado en tonos oscuros, con margen interno
     * y soporte para texto de sugerencia (placeholder).
     *
     * @param placeholder Texto indicativo que se muestra cuando el campo está vacío.
     * @return Objeto {@link JTextField} configurado con el tema visual oscuro.
     */
    public static JTextField crearCampoTexto(String placeholder) {
        JTextField campo = new JTextField();
        campo.setBackground(new Color(22, 28, 42));       // Fondo azul noche
        campo.setForeground(Color.WHITE);                   // Texto de entrada en blanco
        campo.setCaretColor(Color.WHITE);                     // Cursor de edición en blanco
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        // Propiedades de cliente compatibles con Look & Feel modernos (FlatLaf)
        campo.putClientProperty("JTextField.placeholderText", placeholder);
        campo.putClientProperty("JComponent.roundRect", true);

        // Borde azul acero con margen de relleno interno (padding) para evitar texto pegado al borde
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 65, 95), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));

        return campo;
    }
}