package com.travelgo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Datos que envía el cliente para crear una reserva. */
public record ReservaRequest(@NotBlank(message = "El usuario es obligatorio") String usuarioId,
                             @NotEmpty(message = "Debes elegir al menos un servicio") List<String> serviciosIds,
                             Long paqueteId) {
}