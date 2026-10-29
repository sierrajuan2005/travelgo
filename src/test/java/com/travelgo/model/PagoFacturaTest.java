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

/** Pruebas del dominio de Pago y Factura (validaciones del constructor y cálculo del total). */
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

    // ---------- Pago ----------

    @Test
    @DisplayName("El monto del pago es el total de la reserva, con id y fecha generados")
    void pagoTomaElTotalDeLaReserva() {
        Pago pago = new Pago(reserva, MetodoPago.TARJETA_CREDITO);

        assertEquals(600.0, pago.getMonto(), 0.001);
        assertEquals(MetodoPago.TARJETA_CREDITO, pago.getMetodo());
        assertSame(reserva, pago.getReserva());
        assertNotNull(pago.getId());
        assertNotNull(pago.getFechaPago());
    }

    @Test
    @DisplayName("Dos pagos distintos tienen ids distintos")
    void pagosConIdsDistintos() {
        Pago a = new Pago(reserva, MetodoPago.EFECTIVO);
        Pago b = new Pago(reserva, MetodoPago.EFECTIVO);

        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("Un pago sin reserva lanza IllegalArgumentException")
    void pagoSinReserva() {
        assertThrows(IllegalArgumentException.class, () -> new Pago(null, MetodoPago.EFECTIVO));
    }

    @Test
    @DisplayName("Un pago sin método lanza IllegalArgumentException")
    void pagoSinMetodo() {
        assertThrows(IllegalArgumentException.class, () -> new Pago(reserva, null));
    }

    @Test
    @DisplayName("Un pago de una reserva sin servicios (total 0) lanza IllegalArgumentException")
    void pagoReservaSinServicios() {
        Usuario usuario = new Usuario("u2", "Luis", "luis@correo.com", "secreto", RolUsuario.CLIENTE);
        Reserva vacia = new Reserva(usuario, null);

        assertThrows(IllegalArgumentException.class, () -> new Pago(vacia, MetodoPago.EFECTIVO));
    }

    // ---------- Factura ----------

    @Test
    @DisplayName("La factura toma su total del pago y expone la reserva facturada")
    void facturaTomaElTotalDelPago() {
        Pago pago = new Pago(reserva, MetodoPago.TRANSFERENCIA);

        Factura factura = new Factura(pago);

        assertEquals(600.0, factura.getTotal(), 0.001);
        assertSame(pago, factura.getPago());
        assertSame(reserva, factura.getReserva());
        assertEquals(LocalDate.now(), factura.getFechaEmision());
    }

    @Test
    @DisplayName("El número de factura tiene el formato FAC- más 8 caracteres en mayúscula")
    void facturaNumeroConFormato() {
        Factura factura = new Factura(new Pago(reserva, MetodoPago.EFECTIVO));

        assertTrue(factura.getNumero().matches("FAC-[0-9A-F]{8}"),
                "Número inesperado: " + factura.getNumero());
    }

    @Test
    @DisplayName("Dos facturas distintas tienen número e id distintos")
    void facturasConNumerosDistintos() {
        Factura a = new Factura(new Pago(reserva, MetodoPago.EFECTIVO));
        Factura b = new Factura(new Pago(reserva, MetodoPago.EFECTIVO));

        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(a.getNumero(), b.getNumero());
    }

    @Test
    @DisplayName("Una factura sin pago lanza IllegalArgumentException")
    void facturaSinPago() {
        assertThrows(IllegalArgumentException.class, () -> new Factura(null));
    }
}
