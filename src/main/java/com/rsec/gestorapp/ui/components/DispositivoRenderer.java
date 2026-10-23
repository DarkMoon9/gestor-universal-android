package com.rsec.gestorapp.ui.components;

import com.rsec.gestorapp.model.Dispositivo;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * Componente renderizador de celdas personalizado para la lista desplegable ({@link JComboBox})
 * de selección de dispositivos ADB.
 * <p>
 * Representa visualmente cada equipo mediante iconos vectoriales generados por código,
 * un punto de estado de conexión coloreado mediante etiquetas HTML (verde para activo, rojo para inactivo)
 * y detalles como la marca del emulador, modelo y puerto/serie.
 *
 * @author Marco Vences
 * @version 1.0
 */
public class DispositivoRenderer extends DefaultListCellRenderer {

    /** Icono vectorial nativo escalado a 16x16 píxeles que representa un teléfono móvil físico. */
    private static final Icon ICONO_MOVIL = generarIconoMovil(16, 16);

    /** Icono vectorial nativo escalado a 16x16 píxeles que representa un monitor/emulador PC. */
    private static final Icon ICONO_EMULADOR = generarIconoMonitor(16, 16);

    /**
     * Configura y retorna el componente visual utilizado para dibujar cada celda individual
     * en la lista desplegable de dispositivos.
     *
     * @param list         El componente {@link JList} interno que está renderizando la celda.
     * @param value        El objeto a renderizar en la celda actual (se espera un {@link Dispositivo}).
     * @param index        Índice posicional de la celda.
     * @param isSelected   {@code true} si la celda está seleccionada por el usuario.
     * @param cellHasFocus {@code true} si la celda posee el foco de navegación.
     * @return El componente {@link Component} (instancia de {@link JLabel}) configurado para dibujar la celda.
     */
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel etiqueta = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        // Asignación de márgenes internos (padding) y fuente del componente
        etiqueta.setBorder(new EmptyBorder(7, 10, 7, 10));
        etiqueta.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        if (value instanceof Dispositivo) {
            Dispositivo d = (Dispositivo) value;

            // Manejo de estado de lista vacía o dispositivo desconectado
            if ("Ninguno".equalsIgnoreCase(d.getId()) || "Desconectado".equalsIgnoreCase(d.getEstado())) {
                etiqueta.setIcon(null);
                etiqueta.setText("<html><font color='#E74C3C'>●</font> <font color='#9E9E9E'>Ningún dispositivo conectado</font></html>");
            } else {
                // 1. Asignar icono gráfico nativo vectorial según el tipo de equipo (móvil o emulador)
                etiqueta.setIcon(d.isEsEmulador() ? ICONO_EMULADOR : ICONO_MOVIL);

                // 2. Determinar el color del punto indicador (Verde = Activo, Rojo = Desconectado/No autorizado)
                boolean estaConectado = "Conectado".equalsIgnoreCase(d.getEstado()) || "device".equalsIgnoreCase(d.getEstado());
                String colorPunto = estaConectado ? "#2ECC71" : "#E74C3C";

                // 3. Etiqueta en tono cian para destacar emuladores (ej. [MuMu Player])
                String tagEmulador = d.isEsEmulador() ? "<font color='#4DD0E1'>[" + d.getTipoEmulador() + "]</font> " : "";

                // Color dinámico para el identificador de serie o puerto
                String colorId = isSelected ? "#E0E0E0" : "#8A9BA8";

                // 4. Construcción del formato HTML para combinar texto y punto de estado coloreado
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

        // 5. Aplicación de colores de fondo según la selección interactiva
        if (isSelected) {
            etiqueta.setBackground(new Color(33, 150, 243)); // Azul de selección activa
            etiqueta.setForeground(Color.WHITE);
        } else {
            etiqueta.setBackground(new Color(22, 28, 42)); // Fondo oscuro coherente con el tema
        }

        return etiqueta;
    }

    /**
     * Dibuja y genera dinámicamente en memoria un icono de teléfono inteligente de 16x16 píxeles.
     * Utiliza {@link Graphics2D} y primitivas geométricas para asegurar la máxima nitidez.
     *
     * @param ancho Ancho en píxeles de la imagen destino.
     * @param alto  Alto en píxeles de la imagen destino.
     * @return Objeto {@link Icon} vectorial del teléfono.
     */
    private static Icon generarIconoMovil(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Activar suavizado de bordes (Antialiasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Chasis exterior del teléfono (rectángulo redondeado)
        g2.setColor(new Color(55, 71, 79));
        g2.fill(new RoundRectangle2D.Float(3, 1, 10, 14, 4, 4));

        // Pantalla iluminada
        g2.setColor(new Color(41, 182, 246));
        g2.fillRect(5, 3, 6, 8);

        // Botón inferior
        g2.setColor(new Color(176, 190, 197));
        g2.fillOval(7, 12, 2, 2);

        g2.dispose(); // Liberar recursos del contexto gráfico
        return new ImageIcon(img);
    }

    /**
     * Dibuja y genera dinámicamente en memoria un icono de monitor de PC de 16x16 píxeles
     * para representar la ejecución de un emulador.
     *
     * @param ancho Ancho en píxeles de la imagen destino.
     * @param alto  Alto en píxeles de la imagen destino.
     * @return Objeto {@link Icon} vectorial del monitor.
     */
    private static Icon generarIconoMonitor(int ancho, int alto) {
        BufferedImage img = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        // Activar suavizado de bordes (Antialiasing)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Marco exterior del monitor
        g2.setColor(new Color(55, 71, 79));
        g2.fill(new RoundRectangle2D.Float(1, 1, 14, 10, 3, 3));

        // Panel de pantalla iluminado
        g2.setColor(new Color(38, 198, 218));
        g2.fillRect(3, 3, 10, 6);

        // Soporte y base del monitor
        g2.setColor(new Color(120, 144, 156));
        g2.fillRect(7, 11, 2, 2);
        g2.fillRect(4, 13, 8, 2);

        g2.dispose(); // Liberar recursos del contexto gráfico
        return new ImageIcon(img);
    }
}