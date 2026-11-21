package com.travelgo.dto;

import java.time.LocalDate;

/** Datos para crear o actualizar un hotel (el precio es por noche). */
public record HotelRequest(Double precio, Boolean disponible, String nombre,
                           String direccion, String tipoHabitacion, Integer noches, LocalDate fechaEntrada) {
}