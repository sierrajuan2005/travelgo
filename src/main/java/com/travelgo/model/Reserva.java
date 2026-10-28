package com.travelgo.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/*
    Representa una reserva de un viaje realizada por un usuario
*/

public class Reserva{
    private final String id;
    private final Usuario usuario;
    private final List<ServicioReservable> servicios;
    private final LocalDate fechaReserva;
    private String estado;
    private double total;

    /*
        Construye una reserva asociada a un usuario.
    */
    public Reserva(Usuario usuario) {
        if(usuario == null){
            throw new IllegalArgumentException("El usuario de la reserva debe de ser obligatorio.");
        }

        this.id = UUID.randomUUID().toString();
        this.usuario = usuario;
        this.servicios = new ArrayList<>();
        this.fechaReserva = LocalDate.now();
        this.estado = "PENDIENTE";
        this.total = 0.0;
    }

    /*Agrega un servicio a la reserva y actualiza el total */
    public void agregarServicio(ServicioReservable servicio){
        if(servicio == null){
            throw new IllegalArgumentException("El servicio a agregar no puede ser nulo.");
        }

        if("CANCELADA".equals(estado)){
            throw new IllegalStateException("No se pueden agregar servicios a una reserva cancelada.");
        }

        if(!servicio.isDisponible()){
            throw new IllegalStateException("El servicio no está disponible para agregar a la reserva.");
        }

        this.servicios.add(servicio);
        calcularTotal();
    }

    /*Calcula el total de la reserva */
    public double calcularTotal(){
        total = servicios.stream().mapToDouble(ServicioReservable::calcularCosto).sum();
        return total;
    }

    /*Confirma la reserva */
    public void confirmarReserva(){
        if("CANCELADA".equals(estado)){
            throw new IllegalStateException("No se puede confirmar una reserva cancelada.");
        }

        if(servicios.isEmpty()){
            throw new IllegalStateException("No se puede confirmar una reserva sin servicios.");
        }

        calcularTotal();
        estado="CONFIRMADA";
    }

    /*Cancela la reserva */
    public void cancelarReserva(){
        if("CANCELADA".equals(estado)){
            throw new IllegalStateException("La reserva ya esta cancelada.");
        }

        estado = "CANCELADA";
    }

    /*Obtiene el ID de la reserva */
    public String getId() {
        return id;
    }

    /*Obtiene el usuario asociado a la reserva */
    public Usuario getUsuario() {
        return usuario;
    }

    /*Obtiene los servicios de la reserva */
    public List<ServicioReservable> getServicios() {
        return Collections.unmodifiableList(servicios);
    }

    /*Obtiene la fecha de la reserva */
    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    /*Obtiene el estado de la reserva */
    public String getEstado() {
        return estado;
    }

    /*Obtiene el total de la reserva */
    public double getTotal() {
        return total;
    }
}