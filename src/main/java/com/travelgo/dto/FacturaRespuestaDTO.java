package com.travelgo.dto;

import com.travelgo.model.EstadoFactura;
import com.travelgo.model.Factura;

import java.time.LocalDate;
import java.util.List;

/** Respuesta con los datos de una factura. No expone la contraseña del usuario. */
public record FacturaRespuestaDTO(String id, String numero, LocalDate fechaEmision,
                                  double subtotal, double total, EstadoFactura estado,
                                  String reservaId, String usuarioId, String usuarioNombre,
                                  List<ServicioRespuestaDTO> servicios) {

    public static FacturaRespuestaDTO desde(Factura f) {
        return new FacturaRespuestaDTO(
                f.getId(), f.getNumero(), f.getFechaEmision(),
                f.getSubtotal(), f.getTotal(), f.getEstado(),
                f.getReserva().getId(),
                f.getReserva().getUsuario().getId(),
                f.getReserva().getUsuario().getNombre(),
                f.getReserva().getServicios().stream().map(ServicioRespuestaDTO::desde).toList());
    }
}