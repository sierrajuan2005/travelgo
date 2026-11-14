package com.travelgo.dto;

import com.travelgo.model.TipoCupon;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Datos para crear un cupón. */
public record CuponRequest(@NotBlank(message = "El código es obligatorio") String codigo,
                           @NotNull(message = "El tipo es obligatorio") TipoCupon tipo,
                           @NotNull(message = "El valor es obligatorio")
                           @DecimalMin(value = "0.01", message = "El valor debe ser mayor a cero") Double valor,
                           @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
                           @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
                           @NotNull(message = "Los usos máximos son obligatorios")
                           @Min(value = 1, message = "Debe poder usarse al menos una vez") Integer usosMaximos) {
}