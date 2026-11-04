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


    /** Reconstruye un pago con los datos guardados en la base de datos. */
    public Pago(String id, Factura factura, double monto, MetodoPago metodo,
                LocalDate fechaPago, EstadoPago estado) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del pago es obligatorio.");
        }
        if (factura == null || metodo == null || fechaPago == null || estado == null) {
            throw new IllegalArgumentException("La factura, el método, la fecha y el estado del pago son obligatorios.");
        }
        if (!Double.isFinite(monto) || monto < 0) {
            throw new IllegalArgumentException("El monto debe ser un número válido y no negativo.");
        }
        this.id = id;
        this.factura = factura;
        this.monto = monto;
        this.metodo = metodo;
        this.fechaPago = fechaPago;
        this.estado = estado;
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