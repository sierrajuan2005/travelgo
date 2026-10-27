package com.travelgo.dto;

import com.travelgo.model.ServicioReservable;

/** Resumen de un servicio dentro de una reserva. */
public record ServicioRespuestaDTO(String id, String tipo, double precio, double costo) {

    public static ServicioRespuestaDTO desde(ServicioReservable s) {
        return new ServicioRespuestaDTO(s.getId(), s.getClass().getSimpleName(),
                s.getPrecio(), s.calcularCosto());
    }
}