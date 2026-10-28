package com.travelgo.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa la factura generada a partir de un pago.
 */
public class Factura {
    private final String id;
    private final String numero;
    private final Pago pago;
    private final LocalDate fechaEmision;
    private final double total;

    /**
     * Construye la factura de un pago.
     *
     * @param pago pago que se factura
     * @throws IllegalArgumentException si el pago es nulo
     */
    public Factura(Pago pago) {
        if (pago == null) {
            throw new IllegalArgumentException("El pago de la factura es obligatorio.");
        }
        this.id = UUID.randomUUID().toString();
        this.numero = "FAC-" + id.substring(0, 8).toUpperCase();
        this.pago = pago;
        this.fechaEmision = LocalDate.now();
        this.total = pago.getMonto();
    }

    /** Obtiene el id de la factura. */
    public String getId() {
        return id;
    }

    /** Obtiene el número de factura. */
    public String getNumero() {
        return numero;
    }

    /** Obtiene el pago facturado. */
    public Pago getPago() {
        return pago;
    }

    /** Obtiene la reserva facturada. */
    public Reserva getReserva() {
        return pago.getReserva();
    }

    /** Obtiene la fecha de emisión. */
    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    /** Obtiene el total facturado. */
    public double getTotal() {
        return total;
    }
}