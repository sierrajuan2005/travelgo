package com.travelgo.dto;

import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;

import java.time.LocalDateTime;

/** Respuesta con los datos de un pago. */
public record PagoRespuestaDTO(String id, String reservaId, double monto,
                               MetodoPago metodo, LocalDateTime fechaPago) {

    public static PagoRespuestaDTO desde(Pago p) {
        return new PagoRespuestaDTO(p.getId(), p.getReserva().getId(), p.getMonto(),
                p.getMetodo(), p.getFechaPago());
    }
}