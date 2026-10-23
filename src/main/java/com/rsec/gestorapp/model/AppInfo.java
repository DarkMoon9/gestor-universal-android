package com.rsec.gestorapp.model;

import javax.swing.Icon;

/**
 * Modelo de datos con algoritmo mejorado para deducir nombres legibles de aplicaciones.
 */
public class AppInfo {
    private String paquete;
    private String nombreVisible;
    private Icon icono;

    public AppInfo(String paquete, Icon icono) {
        this.paquete = paquete;
        this.icono = icono;
        this.nombreVisible = deducirNombre(paquete);
    }

    /**
     * Deduce un nombre legible a partir del paquete, manejando casos especiales
     * y evitando palabras genéricas como "app" o "android".
     */
    private String deducirNombre(String pkg) {
        if (pkg == null || pkg.trim().isEmpty()) return "Aplicación";

        // 1. Diccionario rápido para aplicaciones con identificadores abreviados
        if (pkg.equals("com.google.android.gm")) return "Gmail";
        if (pkg.equals("com.android.chrome")) return "Google Chrome";
        if (pkg.equals("com.whatsapp")) return "WhatsApp";
        if (pkg.equals("com.google.android.youtube")) return "YouTube";

        // 2. Dividimos el paquete por puntos
        String[] partes = pkg.split("\\.");
        String candidata = partes[partes.length - 1];

        // 3. Si la última palabra es genérica, tomamos la penúltima
        if ((candidata.equalsIgnoreCase("app") ||
                candidata.equalsIgnoreCase("android") ||
                candidata.equalsIgnoreCase("mobile")) && partes.length > 1) {
            candidata = partes[partes.length - 2];
        }

        // 4. Limpiamos guiones o barras bajas
        candidata = candidata.replace("_", " ").replace("-", " ").trim();

        if (candidata.isEmpty()) return "App";

        // 5. Ponemos en mayúscula la primera letra
        return Character.toUpperCase(candidata.charAt(0)) + candidata.substring(1);
    }

    public String getPaquete() {
        return paquete;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public void setNombreVisible(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public Icon getIcono() {
        return icono;
    }

    public void setIcono(Icon icono) {
        this.icono = icono;
    }

    @Override
    public String toString() {
        return paquete;
    }
}