package com.travelgo.exception;

/** Error ocurrido en la capa de acceso a datos. */
public class AccesoDatosException extends RuntimeException {

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}