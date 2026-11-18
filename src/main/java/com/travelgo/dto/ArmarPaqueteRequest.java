package com.travelgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Datos para armar un paquete con un hotel real. Los vuelos ya guardados se agregan con serviciosExtraIds. */
public record ArmarPaqueteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String nombre,
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres") String descripcion,
        @NotEmpty(message = "Elige al menos un destino") List<Long> destinosIds,
        @NotBlank(message = "El nombre de la ciudad es obligatorio") String ciudadNombre,
        @NotBlank(message = "La clave del hotel es obligatoria") String hotelClave,
        @NotNull(message = "La fecha de entrada es obligatoria") LocalDate entrada,
        @NotNull(message = "La fecha de salida es obligatoria") LocalDate salida,
        Integer habitaciones,
        Integer adultos,
        List<String> serviciosExtraIds) {
}