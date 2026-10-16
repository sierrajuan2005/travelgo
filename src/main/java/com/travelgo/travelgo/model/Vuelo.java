package com.travelgo.travelgo.model;

import java.time.LocalDateTime;
/*
    Representa un vuelo disponible para reserva
*/ 
public class Vuelo extends ServicioReservable {
    
    private final String aerolinea;
    private final String origen;
    private final String destino;
    private final LocalDateTime fechaSalida;
    private final LocalDateTime fechaLlegada;

    /*
        Construye un vuelo
        param id identificador unico del vuelo
        param precio precio base del vuelo
        param disponible indica si el vuelo esta disponible
        param aerolinea nombre de la aerolinea que opera el vuelo
        param origen ciudad de origen del vuelo
        param destino ciudad de destino del vuelo
        param fechaSalida fecha y hora de salida del vuelo
        param fechaLlegada fecha y hora de llegada del vuelo
        throws IllegalArgumentException si alguno de los parametros es nulo o vacio, o si el precio es negativo o nulo.
    */

    public Vuelo(String id, double precio, boolean disponible, String aerolinea, String origen, String destino, LocalDateTime fechaSalida, LocalDateTime fechaLlegada) {
        
        super(id, precio, disponible);

        if(aerolinea==null || aerolinea.isBlank()){
            throw new IllegalArgumentException(
                "El nombre de la aerolinea es obligatorio."
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

        if(destino.equalsIgnoreCase(origen)){
            throw new IllegalArgumentException(
                "La ciudad de destino no puede ser la misma que la ciudad de origen."
            );
        }

        if(fechaSalida==null || fechaLlegada==null){
            throw new IllegalArgumentException(
                    "Las fechas de salida y llegada son obligatorias.");
        }

        if (!fechaLlegada.isAfter(fechaSalida)) {
            throw new IllegalArgumentException(
                    "La llegada debe ser posterior a la salida.");
        }
        this.aerolinea = aerolinea.trim();
        this.origen = origen.trim();
        this.destino = destino.trim();
        this.fechaSalida = fechaSalida;
        this.fechaLlegada = fechaLlegada;
    }

    /*Calcula el costo del vuelo */
    @Override
    public double calcularCosto() {
        return getPrecio();
    }

    /*Obtiene el nombre de la aerolinea */

    public String getAerolinea() {
        return aerolinea;
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
    public LocalDateTime getFechaSalida() {
        return fechaSalida;
    }

    /*Obtiene la fecha y hora de llegada */
    public LocalDateTime getFechaLlegada() {
        return fechaLlegada;
    }
}
