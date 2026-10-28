package com.travelgo.dto;

import java.time.LocalDateTime;

/** Datos para crear o actualizar un vuelo. */
public record VueloRequest(Double precio, Boolean disponible, String aerolinea,
                           String origen, String destino,
                           LocalDateTime fechaSalida, LocalDateTime fechaLlegada) {
}