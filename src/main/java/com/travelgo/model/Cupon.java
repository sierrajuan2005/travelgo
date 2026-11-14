package com.travelgo.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Código de descuento aplicable al pago de una reserva.
 * Tiene vigencia por fechas y un máximo de usos.
 */
public class Cupon {
    private final String id;
    private final String codigo;
    private final TipoCupon tipo;
    private final double valor;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final int usosMaximos;
    private int usosActuales;
    private boolean activo;

    /** Construye un cupón nuevo: activo y sin usos. */
    public Cupon(String codigo, TipoCupon tipo, double valor, LocalDate fechaInicio,
                 LocalDate fechaFin, int usosMaximos) {
        this.codigo = normalizarCodigo(codigo);
        validar(tipo, valor, fechaInicio, fechaFin, usosMaximos);
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.valor = valor;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.usosMaximos = usosMaximos;
        this.usosActuales = 0;
        this.activo = true;
    }

    /** Reconstruye un cupón con los datos guardados en la base de datos. */
    public Cupon(String id, String codigo, TipoCupon tipo, double valor, LocalDate fechaInicio,
                 LocalDate fechaFin, int usosMaximos, int usosActuales, boolean activo) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El id del cupón es obligatorio.");
        }
        if (usosActuales < 0) {
            throw new IllegalArgumentException("Los usos actuales no pueden ser negativos.");
        }
        this.codigo = normalizarCodigo(codigo);
        validar(tipo, valor, fechaInicio, fechaFin, usosMaximos);
        this.id = id;
        this.tipo = tipo;
        this.valor = valor;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.usosMaximos = usosMaximos;
        this.usosActuales = usosActuales;
        this.activo = activo;
    }

    /** Pone el código en mayúsculas y verifica que solo tenga letras, números, guion o guion bajo. */
    public static String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del cupón es obligatorio.");
        }
        String limpio = codigo.trim().toUpperCase(Locale.ROOT);
        if (!limpio.matches("[A-Z0-9_-]{3,30}")) {
            throw new IllegalArgumentException(
                    "El código debe tener entre 3 y 30 caracteres: letras, números, guion o guion bajo.");
        }
        return limpio;
    }

    private static void validar(TipoCupon tipo, double valor, LocalDate inicio, LocalDate fin, int usosMaximos) {
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de cupón es obligatorio.");
        }
        if (!Double.isFinite(valor) || valor <= 0) {
            throw new IllegalArgumentException("El valor del cupón debe ser mayor a cero.");
        }
        if (tipo == TipoCupon.PORCENTAJE && valor > 100) {
            throw new IllegalArgumentException("Un cupón de porcentaje no puede superar el 100 %.");
        }
        if (inicio == null || fin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (fin.isBefore(inicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (usosMaximos < 1) {
            throw new IllegalArgumentException("El cupón debe poder usarse al menos una vez.");
        }
    }

    /** Un cupón sirve si está activo, dentro de sus fechas y con usos disponibles. */
    public boolean estaVigente(LocalDate hoy) {
        return activo && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaFin)
                && usosActuales < usosMaximos;
    }

    /** Calcula el descuento sobre un subtotal. Nunca supera el subtotal. */
    public double calcularDescuento(double subtotal) {
        if (!Double.isFinite(subtotal) || subtotal <= 0) {
            throw new IllegalArgumentException("El subtotal debe ser mayor a cero.");
        }
        double descuento = tipo == TipoCupon.PORCENTAJE ? subtotal * valor / 100.0 : valor;
        return Math.round(Math.min(descuento, subtotal) * 100.0) / 100.0;
    }

    /** Registra un uso del cupón. */
    public void registrarUso() {
        if (!estaVigente(LocalDate.now())) {
            throw new IllegalStateException("El cupón " + codigo + " no está vigente.");
        }
        usosActuales++;
    }

    /** Deja el cupón fuera de uso sin borrarlo. */
    public void desactivar() {
        activo = false;
    }

    public String getId() { return id; }
    public String getCodigo() { return codigo; }
    public TipoCupon getTipo() { return tipo; }
    public double getValor() { return valor; }
    public LocalDate getFechaInicio() { return fechaInicio; }
    public LocalDate getFechaFin() { return fechaFin; }
    public int getUsosMaximos() { return usosMaximos; }
    public int getUsosActuales() { return usosActuales; }
    public boolean isActivo() { return activo; }
}