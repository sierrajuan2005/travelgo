package com.travelgo.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa el pago de una factura. El monto es siempre el total de la factura.
 */
public class Pago {
    private final String id;
    private final Factura factura;
    private final double monto;
    private final MetodoPago metodo;
    private final LocalDate fechaPago;
    private final EstadoPago estado;

    /**
     * Construye el pago de una factura pendiente.
     *
     * @param factura factura que se paga
     * @param metodo  forma de pago elegida
     * @throws IllegalArgumentException si la factura o el método son nulos
     * @throws IllegalStateException    si la factura no está pendiente de pago
     */
    public Pago(Factura factura, MetodoPago metodo) {
        if (factura == null) {
            throw new IllegalArgumentException("La factura del pago es obligatoria.");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("El método de pago es obligatorio.");
        }
        if (factura.getEstado() != EstadoFactura.PENDIENTE) {
            throw new IllegalStateException("La factura no está pendiente de pago.");
        }

        this.id = UUID.randomUUID().toString();
        this.factura = factura;
        this.monto = factura.getTotal();
        this.metodo = metodo;
        this.fechaPago = LocalDate.now();
        this.estado = EstadoPago.APROBADO; // pago simulado
    }

    /** Obtiene el id del pago. */
    public String getId() {
        return id;
    }

    /** Obtiene la factura pagada. */
    public Factura getFactura() {
        return factura;
    }

    /** Obtiene la reserva a la que pertenece el pago (a través de la factura). */
    public Reserva getReserva() {
        return factura.getReserva();
    }

    /** Obtiene el monto pagado. */
    public double getMonto() {
        return monto;
    }

    /** Obtiene el método de pago. */
    public MetodoPago getMetodo() {
        return metodo;
    }

    /** Obtiene la fecha del pago. */
    public LocalDate getFechaPago() {
        return fechaPago;
    }

    /** Obtiene el estado del pago. */
    public EstadoPago getEstado() {
        return estado;
    }
}