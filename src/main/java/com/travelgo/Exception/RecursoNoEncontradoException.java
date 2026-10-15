package com.travelgo.Exception;


/** Se lanza cuando un recurso solicitado no existe. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}