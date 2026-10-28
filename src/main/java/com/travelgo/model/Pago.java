package com.travelgo.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa el pago de una reserva. El monto es siempre el total de la reserva.
 */
public class Pago {
    private final String id;
    private final Reserva reserva;
    private final double monto;
    private final MetodoPago metodo;
    private final LocalDateTime fechaPago;

    /**
     * Construye un pago para una reserva.
     *
     * @param reserva reserva que se paga
     * @param metodo  forma de pago elegida
     * @throws IllegalArgumentException si la reserva o el método son nulos, o si la reserva no tiene servicios
     */
    public Pago(Reserva reserva, MetodoPago metodo) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva del pago es obligatoria.");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio.");
        }
        double total = reserva.calcularTotal();
        if (total <= 0) {
            throw new IllegalArgumentException("No se puede pagar una reserva sin servicios.");
        }

        this.id = UUID.randomUUID().toString();
        this.reserva = reserva;
        this.monto = total;
        this.metodo = metodo;
        this.fechaPago = LocalDateTime.now();
    }

    /** Obtiene el id del pago. */
    public String getId() {
        return id;
    }

    /** Obtiene la reserva pagada. */
    public Reserva getReserva() {
        return reserva;
    }

    /** Obtiene el monto pagado. */
    public double getMonto() {
        return monto;
    }

    /** Obtiene el método de pago. */
    public MetodoPago getMetodo() {
        return metodo;
    }

    /** Obtiene la fecha y hora del pago. */
    public LocalDateTime getFechaPago() {
        return fechaPago;
    }
}