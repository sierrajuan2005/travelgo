package com.travelgo.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del dominio de Reserva: reglas que usan los pagos (no confirmar canceladas ni vacías,
 * total = suma de costos) y el constructor de rehidratación que usará el DAO JDBC.
 */
class ReservaTest {

    private Usuario usuario;
    private Vuelo vuelo;
    private Hotel hotel;
    private Transporte noDisponible;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
        vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30));
        hotel = new Hotel("h1", 100.0, true, "Hotel Mar", "Calle 1", "Doble", 3);
        noDisponible = new Transporte("t1", 50.0, false, "Bus", "Bogotá", "Tunja",
                LocalDateTime.of(2026, 12, 2, 6, 0));
    }

    // ---------- constructor nuevo ----------

    @Test
    @DisplayName("Una reserva nueva nace PENDIENTE con total 0 y fecha de hoy")
    void nacePendiente() {
        Reserva reserva = new Reserva(usuario, null);

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoEnum());
        assertEquals("PENDIENTE", reserva.getEstado());
        assertEquals(0.0, reserva.getTotal(), 0.001);
        assertEquals(LocalDate.now(), reserva.getFechaReserva());
    }

    @Test
    @DisplayName("Una reserva sin usuario es inválida")
    void sinUsuario() {
        assertThrows(IllegalArgumentException.class, () -> new Reserva(null, null));
    }

    // ---------- servicios y total ----------

    @Test
    @DisplayName("El total es la suma de los costos de los servicios (hotel = precio x noches)")
    void totalEsLaSuma() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        reserva.agregarServicio(hotel);

        assertEquals(600.0, reserva.getTotal(), 0.001);
    }

    @Test
    @DisplayName("No se puede agregar un servicio nulo")
    void servicioNulo() {
        Reserva reserva = new Reserva(usuario, null);

        assertThrows(IllegalArgumentException.class, () -> reserva.agregarServicio(null));
    }

    @Test
    @DisplayName("No se puede agregar un servicio no disponible")
    void servicioNoDisponible() {
        Reserva reserva = new Reserva(usuario, null);

        assertThrows(IllegalStateException.class, () -> reserva.agregarServicio(noDisponible));
    }

    @Test
    @DisplayName("No se pueden agregar servicios a una reserva cancelada")
    void agregarACancelada() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.cancelarReserva();

        assertThrows(IllegalStateException.class, () -> reserva.agregarServicio(vuelo));
    }

    @Test
    @DisplayName("La lista de servicios devuelta no se puede modificar desde fuera")
    void listaInmutable() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);

        assertThrows(UnsupportedOperationException.class, () -> reserva.getServicios().clear());
    }

    // ---------- estados ----------

    @Test
    @DisplayName("Confirmar una reserva con servicios la pasa a CONFIRMADA")
    void confirmar() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);

        reserva.confirmarReserva();

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoEnum());
    }

    @Test
    @DisplayName("No se puede confirmar una reserva sin servicios")
    void confirmarSinServicios() {
        Reserva reserva = new Reserva(usuario, null);

        assertThrows(IllegalStateException.class, reserva::confirmarReserva);
    }

    @Test
    @DisplayName("No se puede confirmar una reserva cancelada")
    void confirmarCancelada() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        reserva.cancelarReserva();

        assertThrows(IllegalStateException.class, reserva::confirmarReserva);
    }

    @Test
    @DisplayName("Cancelar una reserva la pasa a CANCELADA")
    void cancelar() {
        Reserva reserva = new Reserva(usuario, null);

        reserva.cancelarReserva();

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstadoEnum());
    }

    @Test
    @DisplayName("Cancelar dos veces lanza IllegalStateException")
    void cancelarDosVeces() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.cancelarReserva();

        assertThrows(IllegalStateException.class, reserva::cancelarReserva);
    }

    // ---------- constructor de rehidratación (carga desde la BD) ----------

    @Test
    @DisplayName("Rehidratar conserva id, fecha, estado y total reales")
    void rehidratarConservaDatos() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);

        Reserva reserva = new Reserva("r-1", usuario, null, List.of(vuelo, hotel),
                fecha, EstadoReserva.CONFIRMADA, 600.0);

        assertEquals("r-1", reserva.getId());
        assertSame(usuario, reserva.getUsuario());
        assertEquals(fecha, reserva.getFechaReserva());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoEnum());
        assertEquals(600.0, reserva.getTotal(), 0.001);
        assertEquals(2, reserva.getServicios().size());
    }

    @Test
    @DisplayName("Rehidratar con servicios nulos deja la reserva sin servicios")
    void rehidratarServiciosNulos() {
        Reserva reserva = new Reserva("r-1", usuario, null, null,
                LocalDate.of(2026, 10, 1), EstadoReserva.PENDIENTE, 0.0);

        assertTrue(reserva.getServicios().isEmpty());
    }

    @Test
    @DisplayName("Rehidratar copia la lista de servicios: cambiarla afuera no altera la reserva")
    void rehidratarCopiaLaLista() {
        List<ServicioReservable> origen = new java.util.ArrayList<>(List.of(vuelo));
        Reserva reserva = new Reserva("r-1", usuario, null, origen,
                LocalDate.of(2026, 10, 1), EstadoReserva.PENDIENTE, 300.0);

        origen.add(hotel);

        assertEquals(1, reserva.getServicios().size());
    }

    @Test
    @DisplayName("Rehidratar sin id, con id vacío, sin usuario o sin fecha lanza IllegalArgumentException")
    void rehidratarDatosObligatorios() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);

        assertThrows(IllegalArgumentException.class,
                () -> new Reserva(null, usuario, null, List.of(), fecha, EstadoReserva.PENDIENTE, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Reserva("  ", usuario, null, List.of(), fecha, EstadoReserva.PENDIENTE, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Reserva("r-1", null, null, List.of(), fecha, EstadoReserva.PENDIENTE, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Reserva("r-1", usuario, null, List.of(), null, EstadoReserva.PENDIENTE, 0.0));
    }

    @Test
    @DisplayName("Rehidratar con total negativo o no numérico lanza IllegalArgumentException")
    void rehidratarTotalInvalido() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);

        assertThrows(IllegalArgumentException.class,
                () -> new Reserva("r-1", usuario, null, List.of(), fecha, EstadoReserva.PENDIENTE, -1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new Reserva("r-1", usuario, null, List.of(), fecha, EstadoReserva.PENDIENTE, Double.NaN));
    }

    @Test
    @DisplayName("Una reserva rehidratada como CANCELADA no se puede confirmar ni pagar después")
    void rehidratadaCanceladaNoSeConfirma() {
        Reserva reserva = new Reserva("r-1", usuario, null, List.of(vuelo),
                LocalDate.of(2026, 10, 1), EstadoReserva.CANCELADA, 300.0);

        assertThrows(IllegalStateException.class, reserva::confirmarReserva);
    }
}
