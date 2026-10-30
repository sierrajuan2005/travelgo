package com.travelgo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del dominio de Factura y Pago (validaciones del constructor, estados y cálculo del total). */
class PagoFacturaTest {

    private Reserva reserva;

    @BeforeEach
    void preparar() {
        Usuario usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
        Vuelo vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30));
        Hotel hotel = new Hotel("h1", 100.0, true, "Hotel Mar", "Calle 1", "Doble", 3);
        reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        reserva.agregarServicio(hotel); // total 600
    }

    // ---------- Factura ----------

    @Test
    @DisplayName("La factura toma su subtotal y total de la reserva y nace pendiente")
    void facturaTomaElTotalDeLaReserva() {
        Factura factura = new Factura(reserva);

        assertEquals(600.0, factura.getSubtotal(), 0.001);
        assertEquals(600.0, factura.getTotal(), 0.001);
        assertEquals(EstadoFactura.PENDIENTE, factura.getEstado());
        assertSame(reserva, factura.getReserva());
        assertEquals(LocalDate.now(), factura.getFechaEmision());
        assertNotNull(factura.getId());
    }

    @Test
    @DisplayName("El número de factura tiene el formato FAC- más 8 caracteres en mayúscula")
    void facturaNumeroConFormato() {
        Factura factura = new Factura(reserva);

        assertTrue(factura.getNumero().matches("FAC-[0-9A-F]{8}"),
                "Número inesperado: " + factura.getNumero());
    }

    @Test
    @DisplayName("Dos facturas distintas tienen número e id distintos")
    void facturasConNumerosDistintos() {
        Factura a = new Factura(reserva);
        Factura b = new Factura(reserva);

        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(a.getNumero(), b.getNumero());
    }

    @Test
    @DisplayName("Una factura sin reserva lanza IllegalArgumentException")
    void facturaSinReserva() {
        assertThrows(IllegalArgumentException.class, () -> new Factura(null));
    }

    @Test
    @DisplayName("Una factura de una reserva sin servicios (total 0) lanza IllegalArgumentException")
    void facturaReservaSinServicios() {
        Usuario usuario = new Usuario("u2", "Luis", "luis@correo.com", "secreto", RolUsuario.CLIENTE);
        Reserva vacia = new Reserva(usuario, null);

        assertThrows(IllegalArgumentException.class, () -> new Factura(vacia));
    }

    @Test
    @DisplayName("marcarPagada cambia el estado de la factura a PAGADA")
    void marcarPagadaCambiaElEstado() {
        Factura factura = new Factura(reserva);

        factura.marcarPagada();

        assertEquals(EstadoFactura.PAGADA, factura.getEstado());
    }

    @Test
    @DisplayName("Marcar pagada una factura que ya está pagada lanza IllegalStateException")
    void marcarPagadaDosVeces() {
        Factura factura = new Factura(reserva);
        factura.marcarPagada();

        assertThrows(IllegalStateException.class, factura::marcarPagada);
    }

    // ---------- Pago ----------

    @Test
    @DisplayName("El monto del pago es el total de la factura, con id, fecha y estado aprobado")
    void pagoTomaElTotalDeLaFactura() {
        Factura factura = new Factura(reserva);

        Pago pago = new Pago(factura, MetodoPago.TARJETA_CREDITO);

        assertEquals(600.0, pago.getMonto(), 0.001);
        assertEquals(MetodoPago.TARJETA_CREDITO, pago.getMetodo());
        assertEquals(EstadoPago.APROBADO, pago.getEstado());
        assertSame(factura, pago.getFactura());
        assertSame(reserva, pago.getReserva());
        assertNotNull(pago.getId());
        assertEquals(LocalDate.now(), pago.getFechaPago());
    }

    @Test
    @DisplayName("Dos pagos distintos tienen ids distintos")
    void pagosConIdsDistintos() {
        Pago a = new Pago(new Factura(reserva), MetodoPago.EFECTIVO);
        Pago b = new Pago(new Factura(reserva), MetodoPago.EFECTIVO);

        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("Un pago sin factura lanza IllegalArgumentException")
    void pagoSinFactura() {
        assertThrows(IllegalArgumentException.class, () -> new Pago(null, MetodoPago.EFECTIVO));
    }

    @Test
    @DisplayName("Un pago sin método lanza IllegalArgumentException")
    void pagoSinMetodo() {
        Factura factura = new Factura(reserva);

        assertThrows(IllegalArgumentException.class, () -> new Pago(factura, null));
    }

    @Test
    @DisplayName("Un pago de una factura que ya no está pendiente lanza IllegalStateException")
    void pagoDeFacturaYaPagada() {
        Factura factura = new Factura(reserva);
        factura.marcarPagada();

        assertThrows(IllegalStateException.class, () -> new Pago(factura, MetodoPago.EFECTIVO));
    }
}