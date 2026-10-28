package com.travelgo.dto;

import com.travelgo.model.Reserva;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta con los datos de una reserva y su paquete asociado.
 */
public record ReservaRespuestaDTO(
        String id,
        LocalDate fechaReserva,
        String estado,
        double total,
        String usuarioId,
        String usuarioNombre,
        Long paqueteId,
        String paqueteNombre,
        List<ServicioRespuestaDTO> servicios) {

    public static ReservaRespuestaDTO desde(Reserva r) {
        return new ReservaRespuestaDTO(
                r.getId(),
                r.getFechaReserva(),
                r.getEstado(),
                r.getTotal(),
                r.getUsuario().getId(),
                r.getUsuario().getNombre(),
                r.getPaqueteTuristico() != null
                        ? r.getPaqueteTuristico().getId() : null,
                r.getPaqueteTuristico() != null
                        ? r.getPaqueteTuristico().getNombre() : null,
                r.getServicios().stream()
                        .map(ServicioRespuestaDTO::desde)
                        .toList()
        );
    }
}