package com.rsec.gestorapp.ui.components;

import com.rsec.gestorapp.model.Dispositivo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Renderizador de celda para el combo de dispositivos.
 * Dibuja iconos vectoriales limpios (eliminando el glifo roto) y
 * un punto de estado de color HTML (verde para conectado, rojo para desconectado).
 */
public class DispositivoRenderer extends DefaultListCellRenderer {

    // Iconos vectoriales generados por código (16x16 píxeles)
    private static final Icon ICONO_MOVIL = generarIconoMovil(16, 16);
    private static final Icon ICONO_EMULADOR = generarIconoMonitor(16, 16);

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel etiqueta = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        etiqueta.setBorder(new EmptyBorder(7, 10, 7, 10));
        etiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        if (value instanceof Dispositivo) {
            Dispositivo d = (Dispositivo) value;

            if ("Ninguno".equalsIgnoreCase(d.getId()) || "Desconectado".equalsIgnoreCase(d.getEstado())) {
                etiqueta.setIcon(null);
                etiqueta.setText("<html><font color='#E74C3C'>●</font> <font color='#9E9E9E'>Ningún dispositivo conectado</font></html>");
            } else {
                // 1. Asignar icono gráfico nativo según el tipo de equipo
                etiqueta.setIcon(d.isEsEmulador() ? ICONO_EMULADOR : ICONO_MOVIL);

                // 2. Determinar el color del punto de estado (Verde = Activo, Rojo = Desconectado / No autorizado)
                boolean estaConectado = "Conectado".equalsIgnoreCase(d.getEstado()) || "device".equalsIgnoreCase(d.getEstado());
                String colorPunto = estaConectado ? "#2ECC71" : "#E74C3C";

                // 3. Etiqueta destacada en tono cian para emuladores
                String tagEmulador = d.isEsEmulador() ? "<font color='#4DD0E1'>[" + d.getTipoEmulador() + "]</font> " : "";

                // Color del número de serie o puerto
                String colorId = isSelected ? "#E0E0E0" : "#8A9BA8";

                // 4. Formato HTML para renderizar el punto coloreado junto al texto
                String textoHtml = String.format(
                        "<html><font color='%s'>●</font> %s<b>%s</b> <font color='%s' size='3'>(%s)</font></html>",
                        colorPunto,
                        tagEmulador,
                        d.getNombreModelo(),
                        colorId,
                        d.getId()
                );

                etiqueta.setText(textoHtml);
            }
        }

        // Estilos de resaltado en la lista desplegable
        if (isSelected) {
            etiqueta.setBackground(new Color(33, 150, 243)); // Azul acento
            etiqueta.setForeground(Color.WHITE);
        } else {
            etiqueta.setBackground(new Color(22, 28, 42)); // Fondo oscuro principal
        }

        return etiqueta;
    }

    /**
     * Dibuja un smartphone vectorial nítido con pantalla iluminada (16x16 px).
     */
    private static Icon generarIconoMovil(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Chasis del teléfono
        g2.setColor(new Color(55, 71, 79));
        g2.fill(new RoundRectangle2D.Float(3, 1, 10, 14, 4, 4));

        // Pantalla
        g2.setColor(new Color(41, 182, 246));
        g2.fillRect(5, 3, 6, 8);

        // Botón inferior
        g2.setColor(new Color(176, 190, 197));
        g2.fillOval(7, 12, 2, 2);

        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Dibuja un monitor de escritorio vectorial que representa el emulador en PC (16x16 px).
     */
    private static Icon generarIconoMonitor(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Marco de la pantalla
        g2.setColor(new Color(55, 71, 79));
        g2.fill(new RoundRectangle2D.Float(1, 1, 14, 10, 3, 3));

        // Panel de pantalla iluminado
        g2.setColor(new Color(38, 198, 218));
        g2.fillRect(3, 3, 10, 6);

        // Base y soporte del monitor
        g2.setColor(new Color(120, 144, 156));
        g2.fillRect(7, 11, 2, 2);
        g2.fillRect(4, 13, 8, 2);

        g2.dispose();
        return new ImageIcon(img);
    }
}