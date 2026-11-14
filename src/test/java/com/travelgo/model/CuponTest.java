package com.travelgo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del dominio de Cupon: validaciones, vigencia y cálculo del descuento. */
class CuponTest {

    private final LocalDate hoy = LocalDate.now();

    private Cupon porcentaje(double valor) {
        return new Cupon("verano10", TipoCupon.PORCENTAJE, valor, hoy.minusDays(1), hoy.plusDays(30), 2);
    }

    @Test
    @DisplayName("El código se guarda en mayúsculas y sin espacios")
    void normalizaElCodigo() {
        assertEquals("VERANO10", porcentaje(10).getCodigo());
    }

    @Test
    @DisplayName("Un cupón de porcentaje descuenta esa parte del subtotal")
    void descuentoPorcentaje() {
        assertEquals(105000.0, porcentaje(10).calcularDescuento(1050000), 0.001);
    }

    @Test
    @DisplayName("Un cupón de valor fijo descuenta ese valor")
    void descuentoValorFijo() {
        Cupon fijo = new Cupon("FIJO50", TipoCupon.VALOR_FIJO, 50000, hoy, hoy.plusDays(5), 1);
        assertEquals(50000.0, fijo.calcularDescuento(1050000), 0.001);
    }

    @Test
    @DisplayName("El descuento nunca supera el subtotal")
    void descuentoTopeSubtotal() {
        Cupon fijo = new Cupon("GRANDE", TipoCupon.VALOR_FIJO, 500000, hoy, hoy.plusDays(5), 1);
        assertEquals(100000.0, fijo.calcularDescuento(100000), 0.001);
    }

    @Test
    @DisplayName("Un porcentaje mayor a 100 lanza IllegalArgumentException")
    void porcentajeInvalido() {
        assertThrows(IllegalArgumentException.class, () -> porcentaje(150));
    }

    @Test
    @DisplayName("Un código inválido lanza IllegalArgumentException")
    void codigoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cupon("ab", TipoCupon.PORCENTAJE, 10, hoy, hoy.plusDays(1), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Cupon("con espacio", TipoCupon.PORCENTAJE, 10, hoy, hoy.plusDays(1), 1));
    }

    @Test
    @DisplayName("Una fecha de fin anterior al inicio lanza IllegalArgumentException")
    void fechasInvalidas() {
        assertThrows(IllegalArgumentException.class,
                () -> new Cupon("FECHAS", TipoCupon.PORCENTAJE, 10, hoy, hoy.minusDays(1), 1));
    }

    @Test
    @DisplayName("Un cupón vencido o aún no iniciado no está vigente")
    void vigenciaPorFechas() {
        Cupon vencido = new Cupon("VENCIDO", TipoCupon.PORCENTAJE, 10, hoy.minusDays(10), hoy.minusDays(1), 5);
        Cupon futuro = new Cupon("FUTURO", TipoCupon.PORCENTAJE, 10, hoy.plusDays(1), hoy.plusDays(10), 5);

        assertFalse(vencido.estaVigente(hoy));
        assertFalse(futuro.estaVigente(hoy));
        assertTrue(porcentaje(10).estaVigente(hoy));
    }

    @Test
    @DisplayName("Al agotar los usos el cupón deja de estar vigente y no se puede usar más")
    void agotaLosUsos() {
        Cupon cupon = porcentaje(10); // 2 usos

        cupon.registrarUso();
        cupon.registrarUso();

        assertFalse(cupon.estaVigente(hoy));
        assertThrows(IllegalStateException.class, cupon::registrarUso);
    }

    @Test
    @DisplayName("Un cupón desactivado no está vigente")
    void desactivado() {
        Cupon cupon = porcentaje(10);
        cupon.desactivar();
        assertFalse(cupon.estaVigente(hoy));
    }
}