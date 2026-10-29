package com.travelgo.dto;

import com.travelgo.model.EstadoPago;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;

import java.time.LocalDate;

/** Respuesta con los datos de un pago y su factura. */
public record PagoRespuestaDTO(String id, double monto, MetodoPago metodo, EstadoPago estado,
                               LocalDate fechaPago, String reservaId, FacturaRespuestaDTO factura) {

    public static PagoRespuestaDTO desde(Pago p) {
        return new PagoRespuestaDTO(
                p.getId(), p.getMonto(), p.getMetodo(), p.getEstado(), p.getFechaPago(),
                p.getReserva().getId(), FacturaRespuestaDTO.desde(p.getFactura()));
    }
}