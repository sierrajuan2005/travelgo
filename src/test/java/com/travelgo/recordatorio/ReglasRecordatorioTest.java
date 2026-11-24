package com.travelgo.recordatorio;

import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Hotel;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de las reglas: qué aviso le toca a una reserva según la fecha. */
class ReglasRecordatorioTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 10, 0);

    private final ReglasRecordatorio reglas = new ReglasRecordatorio(6, 2);
    private final Usuario cliente = new Usuario("u-1", "Ana", "ana@ejemplo.com", "x", RolUsuario.CLIENTE);

    private Vuelo vueloQueSale(LocalDateTime salida) {
        return new Vuelo("v-1", 500_000, true, "Avianca", "Bogotá", "Cartagena", salida, salida.plusHours(1));
    }

    private Hotel hotelConEntrada(LocalDate entrada) {
        return new Hotel("h-1", 200_000, true, "Hotel Caribe", "Calle 1", "Doble", 3,
                null, null, null, null, entrada);
    }

    private Reserva reserva(EstadoReserva estado, LocalDate creada, ServicioReservable... servicios) {
        return new Reserva("r-1", cliente, null, List.of(servicios), creada, estado, 700_000);
    }

    @Test
    @DisplayName("El inicio del viaje es lo primero entre el vuelo y la entrada al hotel (15:00)")
    void inicioDelViajeEsElPrimerServicio() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(),
                hotelConEntrada(LocalDate.of(2026, 12, 5)),
                vueloQueSale(LocalDateTime.of(2026, 12, 6, 8, 0)));

        assertThat(ReglasRecordatorio.inicioDelViaje(r)).contains(LocalDateTime.of(2026, 12, 5, 15, 0));
    }

    @Test
    @DisplayName("Sin fechas en los servicios no hay aviso de viaje")
    void sinFechasNoHayAvisoDeViaje() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), hotelConEntrada(null));

        assertThat(reglas.avisosQueCorresponden(r, Optional.empty(), AHORA)).isEmpty();
    }

    @Test
    @DisplayName("Faltan 5 días: aviso de 7 días")
    void faltanCincoDias() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.plusDays(5)));

        assertThat(reglas.avisosQueCorresponden(r, Optional.of(LocalDate.of(2026, 11, 1)), AHORA))
                .containsExactly(TipoAviso.VIAJE_7_DIAS);
    }

    @Test
    @DisplayName("Faltan 20 horas: aviso de 1 día")
    void faltanVeinteHoras() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.plusHours(20)));

        assertThat(reglas.avisoDeViaje(r, AHORA)).contains(TipoAviso.VIAJE_1_DIA);
    }

    @Test
    @DisplayName("Faltan 3 horas: solo el aviso de unas horas (las ventanas no se pisan)")
    void faltanTresHoras() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.plusHours(3)));

        assertThat(reglas.avisoDeViaje(r, AHORA)).contains(TipoAviso.VIAJE_HORAS);
    }

    @Test
    @DisplayName("Faltan 10 días o el viaje ya pasó: no hay aviso de viaje")
    void muyLejosOYaPaso() {
        assertThat(reglas.avisoDeViaje(
                reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.plusDays(10))), AHORA))
                .isEmpty();
        assertThat(reglas.avisoDeViaje(
                reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.minusHours(1))), AHORA))
                .isEmpty();
    }

    @Test
    @DisplayName("Reserva pendiente creada hace 3 días y sin pago: aviso de pago pendiente")
    void pagoPendiente() {
        Reserva r = reserva(EstadoReserva.PENDIENTE, AHORA.toLocalDate().minusDays(3),
                vueloQueSale(AHORA.plusDays(30)));

        assertThat(reglas.avisosQueCorresponden(r, Optional.empty(), AHORA))
                .containsExactly(TipoAviso.PAGO_PENDIENTE);
    }

    @Test
    @DisplayName("Reserva pendiente de hoy: todavía no se avisa el pago pendiente")
    void pagoPendienteRecienCreada() {
        Reserva r = reserva(EstadoReserva.PENDIENTE, AHORA.toLocalDate(), vueloQueSale(AHORA.plusDays(30)));

        assertThat(reglas.avisosQueCorresponden(r, Optional.empty(), AHORA)).isEmpty();
    }

    @Test
    @DisplayName("Pago de hoy: aviso de pago confirmado; pago de hace un mes: no")
    void pagoConfirmado() {
        Reserva r = reserva(EstadoReserva.CONFIRMADA, AHORA.toLocalDate(), vueloQueSale(AHORA.plusDays(30)));

        assertThat(reglas.avisosQueCorresponden(r, Optional.of(AHORA.toLocalDate()), AHORA))
                .containsExactly(TipoAviso.PAGO_CONFIRMADO);
        assertThat(reglas.avisosQueCorresponden(r, Optional.of(AHORA.toLocalDate().minusDays(30)), AHORA))
                .isEmpty();
    }

    @Test
    @DisplayName("Reserva cancelada: ningún aviso")
    void canceladaSinAvisos() {
        Reserva r = reserva(EstadoReserva.CANCELADA, AHORA.toLocalDate().minusDays(5), vueloQueSale(AHORA.plusDays(2)));

        assertThat(reglas.avisosQueCorresponden(r, Optional.empty(), AHORA)).isEmpty();
    }

    @Test
    @DisplayName("Las horas de anticipación deben estar entre 1 y 23")
    void configuracionInvalida() {
        assertThatThrownBy(() -> new ReglasRecordatorio(0, 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ReglasRecordatorio(24, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
