package com.travelgo.travelgo.model;

import java.time.LocalDateTime;

/*
    Representa un transporte disponible para reserva
*/
public class Transporte extends ServicioReservable{

    private final String tipo;
    private final String origen;
    private final String destino;
    private final LocalDateTime fecha;
    
    /*
        Construye un transporte
        param id identificador unico del transporte
        param precio precio base del transporte
        param disponible indica si el transporte esta disponible
        param tipo tipo de transporte (bus, tren, etc.)
        param origen ciudad de origen del transporte
        param destino ciudad de destino del transporte
        param fecha fecha y hora de salida del transporte
        throws IllegalArgumentException si alguno de los parametros es nulo o vacio, o si el precio es negativo o nulo.
    */
    public Transporte(String id, double precio, boolean disponible, String tipo, String origen, String destino, LocalDateTime fecha) {
        super(id, precio, disponible);

        if(tipo==null || tipo.isBlank()){
            throw new IllegalArgumentException(
                "El tipo de transporte es obligatorio."
            );
        }

        if(origen==null || origen.isBlank()){
            throw new IllegalArgumentException(
                "La ciudad de origen es obligatoria."
            );
        }

        if(destino==null || destino.isBlank()){
            throw new IllegalArgumentException(
                "La ciudad de destino es obligatoria."
            );
        }

        if (origen.trim().equalsIgnoreCase(destino.trim())) {
            throw new IllegalArgumentException(
                "La ciudad de destino no puede ser la misma que la ciudad de origen."
            );
        }

        if (fecha == null) {
            throw new IllegalArgumentException(
                "La fecha y hora de salida es obligatoria."
            );
        }

        this.tipo = tipo.trim();
        this.origen = origen.trim();
        this.destino = destino.trim();
        this.fecha = fecha;
    }

    /*Calcula el costo del transporte */
    @Override
    public double calcularCosto() {
        return getPrecio();
    }

    /*Obtiene el tipo de transporte */
    public String getTipo() {
        return tipo;
    }

    /*Obtiene la ciudad de origen */
    public String getOrigen() {
        return origen;
    }

    /*Obtiene la ciudad de destino */
    public String getDestino() {
        return destino;
    }

    /*Obtiene la fecha y hora de salida */
    public LocalDateTime getFecha() {
        return fecha;
    }
}
