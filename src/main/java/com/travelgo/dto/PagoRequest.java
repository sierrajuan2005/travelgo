package com.travelgo.dto;

import com.travelgo.model.MetodoPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos que envía el cliente para pagar una reserva. El código de cupón es opcional. */
public record PagoRequest(@NotBlank(message = "La reserva es obligatoria") String reservaId,
                          @NotNull(message = "El método de pago es obligatorio") MetodoPago metodo,
                          String codigoCupon) {

    /** Pago sin cupón. */
    public PagoRequest(String reservaId, MetodoPago metodo) {
        this(reservaId, metodo, null);
    }
}