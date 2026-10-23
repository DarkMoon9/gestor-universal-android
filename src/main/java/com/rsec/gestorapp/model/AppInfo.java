package com.rsec.gestorapp.model;

import javax.swing.Icon;

/**
 * Modelo de datos que representa una aplicación instalada en el dispositivo Android.
 * Encapsula la información del paquete técnico, su icono representativo y
 * aplica una heurística para deducir un nombre comercial legible para la interfaz gráfica.
 *
 * @author Marco Vences
 * @version 1.0
 */
public class AppInfo {

    /** Identificador técnico único del paquete (ej. "com.whatsapp"). */
    private String paquete;

    /** Nombre comercial deducido o asignado para mostrar en la interfaz de usuario. */
    private String nombreVisible;

    /** Icono gráfico de la aplicación (extraído del APK o avatar de respaldo). */
    private Icon icono;

    /**
     * Constructor principal que inicializa el paquete, asigna el icono correspondiente
     * y calcula automáticamente el nombre visible mediante la heurística interna.
     *
     * @param paquete Identificador único de la aplicación Android (no debe ser nulo).
     * @param icono   Componente visual con la imagen de la aplicación o nulo si aún no se ha cargado.
     */
    public AppInfo(String paquete, Icon icono) {
        this.paquete = paquete;
        this.icono = icono;
        this.nombreVisible = deducirNombre(paquete);
    }

    /**
     * Deduce un nombre comercial limpio y legible a partir de la estructura del paquete técnico.
     * Maneja coincidencias directas en diccionario y descarta sufijos genéricos como "app" o "android".
     *
     * @param pkg Cadena con el nombre técnico del paquete (ej. "com.google.android.youtube").
     * @return Nombre formateado con la primera letra en mayúscula o "Aplicación" si la cadena es inválida.
     */
    private String deducirNombre(String pkg) {
        // Validar que la cadena del paquete contenga texto válido
        if (pkg == null || pkg.trim().isEmpty()) {
            return "Aplicación";
        }

        // 1. Diccionario rápido para aplicaciones con identificadores abreviados o conocidos
        if (pkg.equals("com.google.android.gm")) return "Gmail";
        if (pkg.equals("com.android.chrome")) return "Google Chrome";
        if (pkg.equals("com.whatsapp")) return "WhatsApp";
        if (pkg.equals("com.google.android.youtube")) return "YouTube";

        // 2. Dividir la estructura de dominio separada por puntos
        String[] partes = pkg.split("\\.");
        String candidata = partes[partes.length - 1];

        // 3. Si la última palabra es genérica, tomar la palabra penúltima como nombre del producto
        if ((candidata.equalsIgnoreCase("app") ||
                candidata.equalsIgnoreCase("android") ||
                candidata.equalsIgnoreCase("mobile")) && partes.length > 1) {
            candidata = partes[partes.length - 2];
        }

        // 4. Limpiar caracteres de separación internos (guiones bajos y guiones medios)
        candidata = candidata.replace("_", " ").replace("-", " ").trim();

        if (candidata.isEmpty()) {
            return "App";
        }

        // 5. Aplicar formato de capitalización (primera letra en mayúscula)
        return Character.toUpperCase(candidata.charAt(0)) + candidata.substring(1);
    }

    /**
     * Obtiene el identificador de paquete técnico de la aplicación.
     *
     * @return Cadena con el nombre del paquete (ej. "com.whatsapp").
     */
    public String getPaquete() {
        return paquete;
    }

    /**
     * Obtiene el nombre comercial formateado listo para ser presentado en listas o componentes visuales.
     *
     * @return Cadena con el nombre legible deducido.
     */
    public String getNombreVisible() {
        return nombreVisible;
    }

    /**
     * Permite sobreescribir manualmente el nombre visible de la aplicación.
     *
     * @param nombreVisible Nuevo nombre formateado para la interfaz.
     */
    public void setNombreVisible(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    /**
     * Obtiene el icono representativo de la aplicación.
     *
     * @return Objeto {@link Icon} con la imagen cargada o nulo si está pendiente.
     */
    public Icon getIcono() {
        return icono;
    }

    /**
     * Actualiza el icono asignado a la aplicación (útil al procesar imágenes de forma asíncrona).
     *
     * @param icono Nuevo objeto {@link Icon} a asignar.
     */
    public void setIcono(Icon icono) {
        this.icono = icono;
    }

    /**
     * Representación en formato texto del objeto.
     *
     * @return Cadena con el nombre técnico del paquete.
     */
    @Override
    public String toString() {
        return paquete;
    }
}