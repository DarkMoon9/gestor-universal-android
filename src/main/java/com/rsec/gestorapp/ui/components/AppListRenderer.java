package com.rsec.gestorapp.ui.components;

import com.rsec.gestorapp.model.AppInfo;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Componente renderizador personalizado para elementos de listas {@link JList}
 * que muestran información de objetos {@link AppInfo}.
 * <p>
 * Diseña celdas compuestas por una imagen (icono original o avatar dinámico con iniciales)
 * y dos líneas de texto estructuradas (nombre comercial formateado e identificador técnico de paquete).
 *
 * @author Marco Vences
 * @version 1.0
 */
public class AppListRenderer extends DefaultListCellRenderer {

    /** Paleta estática de colores inspirada en Material Design para asignar a los avatares dinámicos. */
    private static final Color[] COLORES_AVATAR = {
            new Color(33, 150, 243),  // Azul
            new Color(156, 39, 176),  // Morado
            new Color(0, 150, 136),   // Verde azulado
            new Color(255, 112, 67),  // Naranja suave
            new Color(63, 81, 181),   // Índigo
            new Color(0, 188, 212),   // Cian
            new Color(233, 30, 99)    // Rosa
    };

    /**
     * Configura y retorna el componente visual utilizado para dibujar cada celda individual de la lista.
     *
     * @param list         El componente {@link JList} que está pintando la celda.
     * @param value        El valor u objeto retornado para la celda actual (se espera un {@link AppInfo}).
     * @param index        El índice posicional de la celda dentro del modelo.
     * @param isSelected   {@code true} si la celda actual está seleccionada por el usuario.
     * @param cellHasFocus {@code true} si la celda posee el foco de navegación del teclado.
     * @return El componente {@link Component} configurado para ser renderizado en pantalla.
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        // Panel contenedor principal de la celda con disposición horizontal
        JPanel panelFila = new JPanel(new BorderLayout(12, 0));
        panelFila.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (value instanceof AppInfo) {
            AppInfo app = (AppInfo) value;

            // 1. Procesamiento de la imagen del icono
            Icon iconoAMostrar = app.getIcono();
            if (iconoAMostrar == null) {
                // Genera un avatar vectorial dinámico en tiempo real si el APK carece de PNG
                iconoAMostrar = generarAvatarLetra(app.getNombreVisible(), app.getPaquete(), 32);
            }

            JLabel lblIcono = new JLabel(iconoAMostrar);
            panelFila.add(lblIcono, BorderLayout.WEST);

            // 2. Contenedor vertical para las dos líneas de texto
            JPanel panelTexto = new JPanel(new GridLayout(2, 1, 0, 3));
            panelTexto.setOpaque(false);

            // Nombre comercial formateado
            JLabel lblNombre = new JLabel(app.getNombreVisible());
            lblNombre.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblNombre.setForeground(Color.WHITE);

            // Paquete técnico
            JLabel lblPaquete = new JLabel(app.getPaquete());
            lblPaquete.setFont(new Font("Consolas", Font.PLAIN, 11));
            lblPaquete.setForeground(new Color(150, 165, 195));

            panelTexto.add(lblNombre);
            panelTexto.add(lblPaquete);
            panelFila.add(panelTexto, BorderLayout.CENTER);

            // 3. Aplicación de colores según estado de selección y fila par/impar (Efecto cebra)
            if (isSelected) {
                panelFila.setBackground(new Color(40, 60, 95)); // Color azul de selección activa
            } else {
                panelFila.setBackground(index % 2 == 0 ? new Color(20, 26, 38) : new Color(24, 31, 46));
            }
        }

        return panelFila;
    }

    /**
     * Genera dinámicamente un objeto {@link Icon} cuadrangular de esquinas redondeadas
     * con las iniciales del nombre de la aplicación sobre un fondo de color determinado por su hash.
     *
     * @param nombre  Nombre comercial legible de la aplicación.
     * @param paquete Nombre técnico del paquete (utilizado para calcular la semilla de color fija).
     * @param tamano  Dimensión cuadrada en píxeles (ancho y alto) de la imagen a generar.
     * @return Objeto {@link Icon} dibujado en memoria listo para su uso.
     */
    public static Icon generarAvatarLetra(String nombre, String paquete, int tamano) {
        // Creación del lienzo en memoria con soporte de transparencia ARGB
        BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Activación de filtros de suavizado (Antialiasing) para bordes y texto de alta calidad
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Selección determinista de color basada en el hash del paquete (mismo paquete = mismo color siempre)
        int hash = Math.abs(paquete != null ? paquete.hashCode() : nombre.hashCode());
        Color colorFondo = COLORES_AVATAR[hash % COLORES_AVATAR.length];

        // Dibujo del rectángulo redondeado de fondo
        g2.setColor(colorFondo);
        g2.fill(new RoundRectangle2D.Float(0, 0, tamano, tamano, 10, 10));

        // Deducción del texto a presentar (1 o 2 caracteres)
        String texto = (nombre != null && !nombre.isEmpty()) ? nombre.substring(0, 1).toUpperCase() : "A";
        if (nombre != null && nombre.length() >= 2 && nombre.length() <= 4) {
            texto = nombre.substring(0, 2);
        }

        // Configuración de tipografía blanca centrada
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, texto.length() > 1 ? 12 : 15));

        // Cálculo geométrico del centrado del texto
        FontMetrics fm = g2.getFontMetrics();
        int x = (tamano - fm.stringWidth(texto)) / 2;
        int y = ((tamano - fm.getHeight()) / 2) + fm.getAscent();

        g2.drawString(texto, x, y);
        g2.dispose(); // Liberación de recursos gráficos del contexto Java 2D

        return new ImageIcon(img);
    }

    /**
     * Retorna un icono provisional predeterminado de 32x32 píxeles con la letra "A"
     * útil para estados de carga iniciales.
     *
     * @return Objeto {@link Icon} genérico de respaldo.
     */
    public static Icon getIconoProvisional() {
        return generarAvatarLetra("A", "app", 32);
    }
}