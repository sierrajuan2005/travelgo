package com.travelgo.dto;

/** Datos para crear o actualizar un hotel (el precio es por noche). */
public record HotelRequest(Double precio, Boolean disponible, String nombre,
                           String direccion, String tipoHabitacion, Integer noches) {
}