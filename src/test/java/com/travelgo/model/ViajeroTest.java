package com.travelgo.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Viajeros de una reserva: nombre, gustos, actividades y su efecto en el total. */
class ViajeroTest {

    private final Actividad bares = new Actividad("roma-bares", 1L, "Noche de bares", "Tres bares",
            Gusto.FIESTA, 180000, 4);
    private final Actividad vaticano = new Actividad("roma-vaticano", 1L, "Museos Vaticanos", "Entrada",
            Gusto.CULTURA, 320000, 3);

    private final Usuario usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);

    @Test
    @DisplayName("El nombre se limpia y es obligatorio")
    void nombre() {
        assertEquals("Ana María", new Viajero("  Ana   María ", Set.of(), List.of()).getNombre());
        assertThrows(IllegalArgumentException.class, () -> new Viajero("   ", Set.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Viajero("x".repeat(41), Set.of(), List.of()));
    }

    @Test
    @DisplayName("Una actividad repetida se cobra una sola vez")
    void actividadRepetida() {
        Viajero ana = new Viajero("Ana", EnumSet.of(Gusto.FIESTA), List.of(bares, bares, vaticano));
        assertEquals(2, ana.getActividades().size());
        assertEquals(500000, ana.costoActividades(), 0.001);
    }

    @Test
    @DisplayName("Sin gustos es válido (el plan le pide que elija)")
    void sinGustos() {
        assertTrue(new Viajero("Luis", null, null).getGustos().isEmpty());
    }

    @Test
    @DisplayName("El total de la reserva suma servicios + actividades de todos los viajeros")
    void totalConActividades() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(new Vuelo("v1", 1000000, true, "ITA", "Bogotá", "Roma",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 2, 9, 30)));

        reserva.reemplazarViajeros(List.of(
                new Viajero("Ana", EnumSet.of(Gusto.FIESTA), List.of(bares)),
                new Viajero("Luis", EnumSet.of(Gusto.CULTURA, Gusto.RELIGION), List.of(vaticano))));

        assertEquals(1500000, reserva.getTotal(), 0.001);
        assertEquals(1500000, reserva.calcularTotal(), 0.001);
        assertEquals(2, reserva.getViajeros().size());
    }

    @Test
    @DisplayName("Cargar viajeros desde la base de datos no cambia el total guardado")
    void cargarNoRecalcula() {
        Reserva reserva = new Reserva("r1", usuario, null, List.of(), java.time.LocalDate.now(),
                EstadoReserva.PENDIENTE, 123.0);
        reserva.cargarViajeros(List.of(new Viajero("Ana", Set.of(), List.of(bares))));
        assertEquals(123.0, reserva.getTotal(), 0.001);
    }

    @Test
    @DisplayName("No se cambian los viajeros de una reserva cancelada")
    void canceladaNoCambia() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.cancelarReserva();
        assertThrows(IllegalStateException.class,
                () -> reserva.reemplazarViajeros(List.of(new Viajero("Ana", Set.of(), List.of()))));
    }

    @Test
    @DisplayName("Una actividad con precio negativo o sin gusto es inválida")
    void actividadInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> new Actividad("a", 1L, "X", null, Gusto.FIESTA, -1, 2));
        assertThrows(IllegalArgumentException.class,
                () -> new Actividad("a", 1L, "X", null, null, 10, 2));
    }
}
