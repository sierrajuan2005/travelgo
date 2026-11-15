package com.travelgo.exception;

/** Se lanza cuando una API externa no responde o devuelve un error. */
public class ProveedorNoDisponibleException extends RuntimeException {

    public ProveedorNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public ProveedorNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

