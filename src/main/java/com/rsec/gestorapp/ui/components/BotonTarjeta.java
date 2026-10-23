package com.rsec.gestorapp.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Componente gráfico personalizado que extiende de {@link JPanel} para representar
 * un botón con aspecto de tarjeta interactiva en la interfaz gráfica.
 * <p>
 * Incluye un icono visual, título, subtítulo, una flecha indicadora de acción
 * y retroalimentación visual al pasar el cursor (efecto hover).
 *
 * @author RSEC
 * @version Marco Vences
 */
public class BotonTarjeta extends JPanel {

    /** Etiqueta contenedora del icono representativo de la tarjeta. */
    private JLabel lblIcono;

    /** Etiqueta con el título principal de la acción. */
    private JLabel lblTitulo;

    /** Etiqueta con el subtítulo descriptivo secundario. */
    private JLabel lblSubtitulo;

    /** Color de fondo base en estado reposo. */
    private Color colorFondoBase = new Color(30, 35, 50);

    /** Color de fondo al pasar el cursor sobre la tarjeta (efecto hover). */
    private Color colorFondoHover = new Color(40, 45, 60);

    /**
     * Construye e inicializa un botón con formato de tarjeta interactiva.
     *
     * @param icono       Texto o glifo emoji para el icono de la izquierda (ej. "📱").
     * @param titulo      Texto del título principal de la tarjeta.
     * @param subtitulo   Texto descriptivo breve de la acción.
     * @param colorAcento Color utilizado para generar el borde personalizado de la tarjeta.
     * @param accion      Escuchador de eventos {@link ActionListener} ejecutado al hacer clic.
     */
    public BotonTarjeta(String icono, String titulo, String subtitulo, Color colorAcento, ActionListener accion) {
        // Configuración de la disposición del panel con separaciones horizontales
        setLayout(new BorderLayout(15, 0));
        setBackground(colorFondoBase);

        // Borde compuesto: marco con el color de acento oscurecido y margen interno (padding)
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorAcento.darker(), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Cambiar el puntero del ratón a la mano interactiva al pasar por encima
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 1. Configuración del icono lateral izquierdo
        lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        // 2. Panel central con dos filas para título y subtítulo
        JPanel panelTextos = new JPanel(new GridLayout(2, 1));
        panelTextos.setOpaque(false);

        lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Color.WHITE);

        lblSubtitulo = new JLabel(subtitulo);
        lblSubtitulo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSubtitulo.setForeground(Color.GRAY);

        panelTextos.add(lblTitulo);
        panelTextos.add(lblSubtitulo);

        // 3. Flecha de navegación lateral derecha
        JLabel lblFlecha = new JLabel("❯");
        lblFlecha.setForeground(Color.GRAY);

        // Agregar los componentes al panel en sus respectivas zonas de BorderLayout
        add(lblIcono, BorderLayout.WEST);
        add(panelTextos, BorderLayout.CENTER);
        add(lblFlecha, BorderLayout.EAST);

        // 4. Registro de eventos de ratón para interactividad (Hover y Clic)
        addMouseListener(new MouseAdapter() {
            /**
             * Cambia el fondo a un tono más claro cuando el cursor entra en la tarjeta.
             */
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(colorFondoHover);
            }

            /**
             * Restaura el fondo base cuando el cursor sale del área de la tarjeta.
             */
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(colorFondoBase);
            }

            /**
             * Dispara la acción asociada cuando el usuario hace clic sobre la tarjeta.
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                if (accion != null) {
                    accion.actionPerformed(null);
                }
            }
        });
    }

    /**
     * Obtiene el título principal de la tarjeta.
     *
     * @return Cadena con el título actual.
     */
    public String getTitulo() {
        return lblTitulo.getText();
    }

    /**
     * Actualiza dinámicamente el título principal de la tarjeta.
     *
     * @param titulo Nuevo texto para el título.
     */
    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    /**
     * Obtiene el subtítulo descriptivo de la tarjeta.
     *
     * @return Cadena con el subtítulo actual.
     */
    public String getSubtitulo() {
        return lblSubtitulo.getText();
    }

    /**
     * Actualiza dinámicamente el subtítulo de la tarjeta.
     *
     * @param subtitulo Nuevo texto para el subtítulo.
     */
    public void setSubtitulo(String subtitulo) {
        lblSubtitulo.setText(subtitulo);
    }

    /**
     * Actualiza el icono de la tarjeta.
     *
     * @param icono Nuevo emoji o texto de icono.
     */
    public void setIcono(String icono) {
        lblIcono.setText(icono);
    }
}