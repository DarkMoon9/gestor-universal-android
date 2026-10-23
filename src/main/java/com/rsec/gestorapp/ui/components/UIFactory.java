package com.rsec.gestorapp.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Fábrica de componentes visuales reutilizables para la interfaz gráfica.
 */
public class UIFactory {

    /**
     * Crea un botón secundario estilizado con bordes suaves y cursor interactivo.
     * @param texto Texto o emoji que mostrará el botón.
     * @param dimension Tamaño preferido (ancho y alto).
     */
    public static JButton crearBotonSecundario(String texto, Dimension dimension) {
        JButton btn = new JButton(texto);
        btn.setPreferredSize(dimension);
        btn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
        btn.setBackground(new Color(30, 45, 70));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(new Color(45, 65, 95), 1, true));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Crea un panel contenedor con fondo oscuro y margen acolchado para agrupar secciones.
     */
    public static JPanel crearContenedorSeccion() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 30, 45));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 55, 75), 1, true),
                new EmptyBorder(15, 20, 15, 20)
        ));
        return panel;
    }

    /**
     * Construye un encabezado con un icono a la izquierda y dos etiquetas de texto (título y subtítulo).
     * @param icono Emoji o símbolo que representa la sección.
     * @param titulo Texto principal destacado.
     * @param subtitulo Texto secundario explicativo en color gris.
     */
    public static JPanel crearEncabezado(String icono, String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout(15, 5));
        panel.setOpaque(false);

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

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

        panel.add(lblIcono, BorderLayout.WEST);
        panel.add(panelTextos, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea un campo de texto estilizado con tonos azul noche y margen interior.
     * @param placeholder Texto de sugerencia cuando el campo está vacío.
     */
    public static JTextField crearCampoTexto(String placeholder) {
        JTextField campo = new JTextField();
        campo.setBackground(new Color(22, 28, 42));       // Fondo azul noche
        campo.setForeground(Color.WHITE);                   // Texto blanco
        campo.setCaretColor(Color.WHITE);                     // Cursor blanco
        campo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        campo.putClientProperty("JTextField.placeholderText", placeholder);
        campo.putClientProperty("JComponent.roundRect", true); // Bordes redondeados en FlatLaf

        // Borde azul acero con acolchado interior para que el texto respire
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(45, 65, 95), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        return campo;
    }
}