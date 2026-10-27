package com.travelgo.dto;

import java.time.LocalDateTime;

/** Datos para crear o actualizar un transporte. */
public record TransporteRequest(Double precio, Boolean disponible, String tipo,
                                String origen, String destino, LocalDateTime fecha) {
}