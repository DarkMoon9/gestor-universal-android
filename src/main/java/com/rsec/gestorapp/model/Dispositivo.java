package com.rsec.gestorapp.model;

/**
 * Modelo de datos que representa un dispositivo Android físico o una máquina virtual (emulador)
 * detectado a través del servicio ADB (Android Debug Bridge).
 *
 * @author Marco Vences
 * @version 1.0
 */
public class Dispositivo {

    /** Identificador único de ADB (número de serie USB o dirección IP con puerto local). */
    private String id;

    /** Nombre comercial del dispositivo (ej. "Galaxy S21", "Pixel 7 AVD"). */
    private String nombreModelo;

    /** Estado de la conexión reportado por ADB (ej. "Conectado", "device", "unauthorized"). */
    private String estado;

    /** Indica si el dispositivo corresponde a un emulador virtual (true) o un equipo físico (false). */
    private boolean esEmulador;

    /** Plataforma o marca del emulador (ej. "Android Studio AVD", "MuMu Player", "NoxPlayer", "BlueStacks"). */
    private String tipoEmulador;

    /**
     * Constructor secundario para dispositivos físicos estándar o instancias genéricas.
     * Define automáticamente que no es un emulador y establece el tipo como nulo.
     *
     * @param id           Identificador de serie o IP del dispositivo.
     * @param nombreModelo Nombre del modelo comercial.
     * @param estado       Estado actual de autorización en ADB.
     */
    public Dispositivo(String id, String nombreModelo, String estado) {
        this(id, nombreModelo, estado, false, null);
    }

    /**
     * Constructor completo que permite especificar todas las propiedades del dispositivo,
     * incluyendo el tipo de virtualización si se trata de un emulador.
     *
     * @param id           Identificador de serie o IP del dispositivo.
     * @param nombreModelo Nombre del modelo comercial.
     * @param estado       Estado actual de autorización en ADB.
     * @param esEmulador   {@code true} si es una máquina virtual, {@code false} si es un dispositivo físico.
     * @param tipoEmulador Nombre de la plataforma del emulador (ej. "MuMu Player", "AVD").
     */
    public Dispositivo(String id, String nombreModelo, String estado, boolean esEmulador, String tipoEmulador) {
        this.id = id;
        this.nombreModelo = nombreModelo;
        this.estado = estado;
        this.esEmulador = esEmulador;
        this.tipoEmulador = tipoEmulador;
    }

    /**
     * Obtiene el identificador único o dirección IP del dispositivo.
     *
     * @return Cadena con el identificador de serie o puerto.
     */
    public String getId() {
        return id;
    }

    /**
     * Actualiza el identificador único del dispositivo.
     *
     * @param id Nuevo identificador o puerto.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del modelo comercial del equipo.
     *
     * @return Cadena con el modelo.
     */
    public String getNombreModelo() {
        return nombreModelo;
    }

    /**
     * Actualiza el nombre del modelo comercial del equipo.
     *
     * @param nombreModelo Nuevo nombre de modelo.
     */
    public void setNombreModelo(String nombreModelo) {
        this.nombreModelo = nombreModelo;
    }

    /**
     * Obtiene el estado actual de la conexión reportado por ADB.
     *
     * @return Cadena con el estado ("Conectado", "device", "unauthorized", etc.).
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Actualiza el estado de la conexión ADB.
     *
     * @param estado Nuevo estado de conexión.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Indica si el dispositivo es un emulador de software.
     *
     * @return {@code true} si es un emulador; {@code false} si es un teléfono físico.
     */
    public boolean isEsEmulador() {
        return esEmulador;
    }

    /**
     * Establece si el dispositivo debe ser tratado como un emulador.
     *
     * @param esEmulador {@code true} para emulador, {@code false} para teléfono físico.
     */
    public void setEsEmulador(boolean esEmulador) {
        this.esEmulador = esEmulador;
    }

    /**
     * Obtiene la marca o tipo de emulador. Si es nulo, devuelve "Emulador" por defecto.
     *
     * @return Cadena con el nombre de la plataforma virtual.
     */
    public String getTipoEmulador() {
        return tipoEmulador != null ? tipoEmulador : "Emulador";
    }

    /**
     * Actualiza la plataforma o marca de emulador asociada.
     *
     * @param tipoEmulador Cadena con el nombre del emulador (ej. "BlueStacks").
     */
    public void setTipoEmulador(String tipoEmulador) {
        this.tipoEmulador = tipoEmulador;
    }

    /**
     * Genera una representación en cadena legible del dispositivo para depuración o componentes visuales básicos.
     *
     * @return Cadena formateada con el tipo, modelo e identificador entre paréntesis.
     */
    @Override
    public String toString() {
        if ("Ninguno".equalsIgnoreCase(id)) {
            return "Ningún dispositivo conectado";
        }
        String prefijo = esEmulador ? "[" + getTipoEmulador() + "] " : "";
        return prefijo + nombreModelo + " (" + id + ")";
    }
}