package com.travelgo.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Representa la factura de una reserva. Nace pendiente y pasa a pagada cuando se registra su pago.
 * Puede llevar un cupón de descuento, que se aplica mientras está pendiente.
 */
public class Factura {
    private final String id;
    private final String numero;
    private final Reserva reserva;
    private final LocalDate fechaEmision;
    private final double subtotal;
    private double total;
    private double descuento;
    private Cupon cupon;
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
        this.total = subtotal;
        this.descuento = 0.0;
        this.cupon = null;
        this.estado = EstadoFactura.PENDIENTE;
    }

    /** Reconstruye una factura sin cupón con los datos guardados en la base de datos. */
    public Factura(String id, String numero, Reserva reserva, LocalDate fechaEmision,
                   double subtotal, double total, EstadoFactura estado) {
        this(id, numero, reserva, fechaEmision, subtotal, total, estado, 0.0, null);
    }

    /** Reconstruye una factura con los datos guardados en la base de datos, incluido su cupón. */
    public Factura(String id, String numero, Reserva reserva, LocalDate fechaEmision,
                   double subtotal, double total, EstadoFactura estado, double descuento, Cupon cupon) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id de la factura es obligatorio.");
        }
        if (numero == null || numero.isBlank()) {
            throw new IllegalArgumentException("El número de la factura es obligatorio.");
        }
        if (reserva == null || fechaEmision == null || estado == null) {
            throw new IllegalArgumentException("La reserva, la fecha y el estado de la factura son obligatorios.");
        }
        if (!Double.isFinite(subtotal) || subtotal < 0 || !Double.isFinite(total) || total < 0) {
            throw new IllegalArgumentException("El subtotal y el total deben ser números válidos y no negativos.");
        }
        if (!Double.isFinite(descuento) || descuento < 0) {
            throw new IllegalArgumentException("El descuento debe ser un número válido y no negativo.");
        }
        this.id = id;
        this.numero = numero;
        this.reserva = reserva;
        this.fechaEmision = fechaEmision;
        this.subtotal = subtotal;
        this.total = total;
        this.descuento = descuento;
        this.cupon = cupon;
        this.estado = estado;
    }

    /**
     * Aplica un cupón: calcula el descuento sobre el subtotal y deja el nuevo total.
     * Solo se puede mientras la factura está pendiente y una sola vez.
     */
    public void aplicarCupon(Cupon cupon) {
        if (cupon == null) {
            throw new IllegalArgumentException("El cupón es obligatorio.");
        }
        if (estado != EstadoFactura.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aplicar un cupón a una factura pendiente.");
        }
        if (this.cupon != null) {
            throw new IllegalStateException("La factura ya tiene un cupón aplicado.");
        }
        double calculado = cupon.calcularDescuento(subtotal);
        if (calculado >= subtotal) {
            throw new IllegalArgumentException("El descuento no puede cubrir el total de la factura.");
        }
        this.descuento = calculado;
        this.total = Math.round((subtotal - calculado) * 100.0) / 100.0;
        this.cupon = cupon;
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

    public String getId() { return id; }
    public String getNumero() { return numero; }
    public Reserva getReserva() { return reserva; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public double getSubtotal() { return subtotal; }
    public double getTotal() { return total; }
    public double getDescuento() { return descuento; }
    public Cupon getCupon() { return cupon; }
    public EstadoFactura getEstado() { return estado; }
}