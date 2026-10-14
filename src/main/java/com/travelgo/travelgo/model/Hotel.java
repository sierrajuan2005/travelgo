package com.travelgo.travelgo.model;

/*
    Representa un hotel disponible para reserva
*/
public class Hotel extends ServicioReservable {
    private final String nombre;
    private final String direccion;
    private final String tipoHabitacion;
    private final int noches;

    /*
        Construye un hotel
        param id identificador unico del hotel
        param precio precio por noche 
        param disponible indica si el hotel esta disponible
        param nombre nombre del hotel
        param direccion direccion del hotel
        param tipoHabitacion tipo de habitacion del hotel
        param noches cantidad de noches de la reserva
        throws IllegalArgumentException si alguno de los parametros es nulo o vacio, o si el precio es negativo o nulo.
    */

    public Hotel(String id, double precio, boolean disponible, String nombre, String direccion, String tipoHabitacion, int noches) {
        super(id, precio, disponible);

        if(nombre==null || nombre.isBlank()){
            throw new IllegalArgumentException(
                "El nombre del hotel es obligatorio."
            );
        }

        if(direccion==null || direccion.isBlank()){
            throw new IllegalArgumentException(
                "La direccion del hotel es obligatoria."
            );
        }

        if(tipoHabitacion==null || tipoHabitacion.isBlank()){
            throw new IllegalArgumentException(
                "El tipo de habitacion es obligatorio."
            );
        }

        if(noches<=0){
            throw new IllegalArgumentException(
                "La cantidad de noches debe ser mayor a cero."
            );
        }

        this.nombre = nombre;
        this.direccion = direccion;
        this.tipoHabitacion = tipoHabitacion;
        this.noches = noches;
    }
    
}
