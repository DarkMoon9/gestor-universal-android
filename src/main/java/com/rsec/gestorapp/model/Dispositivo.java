package com.rsec.gestorapp.model;

/**
 * Representa un dispositivo Android físico o virtual detectado vía ADB.
 */
public class Dispositivo {
    private String id;
    private String nombreModelo;
    private String estado;
    private boolean esEmulador;
    private String tipoEmulador; // Ej: "AVD", "BlueStacks", "Nox", "LDPlayer", etc.

    public Dispositivo(String id, String nombreModelo, String estado) {
        this(id, nombreModelo, estado, false, null);
    }

    public Dispositivo(String id, String nombreModelo, String estado, boolean esEmulador, String tipoEmulador) {
        this.id = id;
        this.nombreModelo = nombreModelo;
        this.estado = estado;
        this.esEmulador = esEmulador;
        this.tipoEmulador = tipoEmulador;
    }

    public String getId() {
        return id;
    }

    public String getNombreModelo() {
        return nombreModelo;
    }

    public String getEstado() {
        return estado;
    }

    public boolean isEsEmulador() {
        return esEmulador;
    }

    public String getTipoEmulador() {
        return tipoEmulador != null ? tipoEmulador : "Emulador";
    }

    @Override
    public String toString() {
        if ("Ninguno".equalsIgnoreCase(id)) {
            return "Ningún dispositivo conectado";
        }
        String prefijo = esEmulador ? "[" + getTipoEmulador() + "] " : "";
        return prefijo + nombreModelo + " (" + id + ")";
    }
}