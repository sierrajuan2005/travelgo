package com.travelgo.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa la factura de una reserva. Nace pendiente y pasa a pagada cuando se registra su pago.
 */
public class Factura {
    private final String id;
    private final String numero;
    private final Reserva reserva;
    private final LocalDate fechaEmision;
    private final double subtotal;
    private final double total;
    private EstadoFactura estado;

    /**
     * Construye la factura de una reserva.
     *
     * @param reserva reserva que se factura
     * @throws IllegalArgumentException si la reserva es nula o no tiene servicios
     */
    public Factura(Reserva reserva) {
        if (reserva == null) {
            throw new IllegalArgumentException("La reserva de la factura es obligatoria.");
        }
        double subtotal = reserva.calcularTotal();
        if (subtotal <= 0) {
            throw new IllegalArgumentException("No se puede facturar una reserva sin servicios.");
        }

        this.id = UUID.randomUUID().toString();
        this.numero = "FAC-" + id.substring(0, 8).toUpperCase();
        this.reserva = reserva;
        this.fechaEmision = LocalDate.now();
        this.subtotal = subtotal;
        this.total = subtotal; // sin impuestos ni descuentos por ahora
        this.estado = EstadoFactura.PENDIENTE;
    }

    /** Marca la factura como pagada. */
    public void marcarPagada() {
        if (estado == EstadoFactura.PAGADA) {
            throw new IllegalStateException("La factura ya está pagada.");
        }
        if (estado == EstadoFactura.ANULADA) {
            throw new IllegalStateException("No se puede pagar una factura anulada.");
        }
        estado = EstadoFactura.PAGADA;
    }

    /** Obtiene el id de la factura. */
    public String getId() {
        return id;
    }

    /** Obtiene el número de factura. */
    public String getNumero() {
        return numero;
    }

    /** Obtiene la reserva facturada. */
    public Reserva getReserva() {
        return reserva;
    }

    /** Obtiene la fecha de emisión. */
    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    /** Obtiene el subtotal. */
    public double getSubtotal() {
        return subtotal;
    }

    /** Obtiene el total. */
    public double getTotal() {
        return total;
    }

    /** Obtiene el estado de la factura. */
    public EstadoFactura getEstado() {
        return estado;
    }
}