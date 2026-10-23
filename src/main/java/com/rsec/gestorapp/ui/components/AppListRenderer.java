package com.rsec.gestorapp.ui.components;

import com.rsec.gestorapp.model.AppInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Renderizador de celda que presenta el icono original de la app o un avatar
 * gráfico con sus iniciales si no posee imagen rasterizada en el APK.
 */
public class AppListRenderer extends DefaultListCellRenderer {

    // Paleta de colores suaves estilo Material Design para los avatares
    private static final Color[] COLORES_AVATAR = {
            new Color(33, 150, 243),  // Azul
            new Color(156, 39, 176),  // Morado
            new Color(0, 150, 136),   // Verde azulado
            new Color(255, 112, 67),  // Naranja suave
            new Color(63, 81, 181),   // Índigo
            new Color(0, 188, 212),   // Cian
            new Color(233, 30, 99)    // Rosa
    };

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JPanel panelFila = new JPanel(new BorderLayout(12, 0));
        panelFila.setBorder(new EmptyBorder(8, 12, 8, 12));

        if (value instanceof AppInfo) {
            AppInfo app = (AppInfo) value;

            // 1. Icono: Si tiene imagen descargada la usa; si no, genera un avatar con su inicial
            Icon iconoAMostrar = app.getIcono();
            if (iconoAMostrar == null) {
                iconoAMostrar = generarAvatarLetra(app.getNombreVisible(), app.getPaquete(), 32);
            }

            JLabel lblIcono = new JLabel(iconoAMostrar);
            panelFila.add(lblIcono, BorderLayout.WEST);

            // 2. Contenedor de dos líneas de texto
            JPanel panelTexto = new JPanel(new GridLayout(2, 1, 0, 3));
            panelTexto.setOpaque(false);

            // Nombre comercial deducido
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

            // 3. Estilos de fondo y selección
            if (isSelected) {
                panelFila.setBackground(new Color(40, 60, 95));
            } else {
                panelFila.setBackground(index % 2 == 0 ? new Color(20, 26, 38) : new Color(24, 31, 46));
            }
        }

        return panelFila;
    }

    /**
     * Genera un avatar elegante con fondo de color y las iniciales de la app
     * para aquellas aplicaciones que usan vectores puros o carecen de PNG.
     */
    public static Icon generarAvatarLetra(String nombre, String paquete, int tamano) {
        BufferedImage img = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Seleccionar un color consistente basado en el texto del paquete
        int hash = Math.abs(paquete != null ? paquete.hashCode() : nombre.hashCode());
        Color colorFondo = COLORES_AVATAR[hash % COLORES_AVATAR.length];

        // Dibujar contenedor redondeado
        g2.setColor(colorFondo);
        g2.fill(new RoundRectangle2D.Float(0, 0, tamano, tamano, 10, 10));

        // Obtener la inicial (o dos primeras letras si el nombre es corto)
        String texto = (nombre != null && !nombre.isEmpty()) ? nombre.substring(0, 1).toUpperCase() : "A";
        if (nombre != null && nombre.length() >= 2 && nombre.length() <= 4) {
            texto = nombre.substring(0, 2);
        }

        // Configurar tipografía del texto centrado
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, texto.length() > 1 ? 12 : 15));

        FontMetrics fm = g2.getFontMetrics();
        int x = (tamano - fm.stringWidth(texto)) / 2;
        int y = ((tamano - fm.getHeight()) / 2) + fm.getAscent();

        g2.drawString(texto, x, y);
        g2.dispose();

        return new ImageIcon(img);
    }

    public static Icon getIconoProvisional() {
        return generarAvatarLetra("A", "app", 32);
    }
}