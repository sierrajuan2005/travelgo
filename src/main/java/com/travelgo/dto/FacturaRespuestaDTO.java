package com.travelgo.dto;

import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Respuesta con los datos de una factura. No expone la contraseña del usuario. */
public record FacturaRespuestaDTO(String id, String numero, LocalDate fechaEmision, double total,
                                  String reservaId, String usuarioId, String usuarioNombre,
                                  String pagoId, MetodoPago metodoPago, LocalDateTime fechaPago,
                                  List<ServicioRespuestaDTO> servicios) {

    public static FacturaRespuestaDTO desde(Factura f) {
        return new FacturaRespuestaDTO(
                f.getId(), f.getNumero(), f.getFechaEmision(), f.getTotal(),
                f.getReserva().getId(),
                f.getReserva().getUsuario().getId(),
                f.getReserva().getUsuario().getNombre(),
                f.getPago().getId(), f.getPago().getMetodo(), f.getPago().getFechaPago(),
                f.getReserva().getServicios().stream().map(ServicioRespuestaDTO::desde).toList());
    }
}