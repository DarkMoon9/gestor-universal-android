package com.rsec.gestorapp.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BotonTarjeta extends JPanel {
    public BotonTarjeta(String icono, String titulo, String subtitulo, Color colorAcento, java.awt.event.ActionListener accion) {
        setLayout(new BorderLayout(15, 0));
        setBackground(new Color(30, 35, 50));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorAcento.darker(), 1, true),
                new EmptyBorder(10, 15, 10, 15)
        ));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblIcono = new JLabel(icono);
        lblIcono.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JPanel panelTextos = new JPanel(new GridLayout(2, 1));
        panelTextos.setOpaque(false);

        JLabel lblTitulo = new JLabel(titulo);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel(subtitulo);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSub.setForeground(Color.GRAY);

        panelTextos.add(lblTitulo);
        panelTextos.add(lblSub);

        JLabel lblFlecha = new JLabel("❯");
        lblFlecha.setForeground(Color.GRAY);

        add(lblIcono, BorderLayout.WEST);
        add(panelTextos, BorderLayout.CENTER);
        add(lblFlecha, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { setBackground(new Color(40, 45, 60)); }
            @Override
            public void mouseExited(MouseEvent e) { setBackground(new Color(30, 35, 50)); }
            @Override
            public void mouseClicked(MouseEvent e) { accion.actionPerformed(null); }
        });
    }
}