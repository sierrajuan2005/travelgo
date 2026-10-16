package com.travelgo.travelgo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        this.estado = "Pendiente";
        this.total = 0.0;
    }
}