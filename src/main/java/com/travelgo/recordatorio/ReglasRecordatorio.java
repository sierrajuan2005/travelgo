package com.travelgo.recordatorio;

import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Hotel;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Decide qué avisos le tocan a una reserva en un momento dado. No envía nada ni consulta la base de datos:
 * solo aplica las reglas, así que se prueba sin Spring.
 * <ul>
 *   <li><b>Viaje:</b> 7 días, 1 día y unas horas antes de la salida. Las ventanas no se pisan: si la reserva se
 *       hace 3 horas antes del vuelo, solo sale el aviso de "unas horas".</li>
 *   <li><b>Pago pendiente:</b> reserva PENDIENTE, sin pago, creada hace {@code diasPagoPendiente} días o más.</li>
 *   <li><b>Pago confirmado:</b> la reserva tiene pago de hoy o de ayer (los pagos viejos no se avisan para no
 *       mandar correos atrasados la primera vez que arranca el servicio).</li>
 * </ul>
 */
@Component
public class ReglasRecordatorio {

    /** Hora de entrada a un hotel cuando solo se conoce el día (hora de check-in típica). */
    static final LocalTime HORA_ENTRADA_HOTEL = LocalTime.of(15, 0);

    private final Duration ventanaHoras;
    private final int diasPagoPendiente;

    /**
     * @param horasAntes        cuántas horas antes de la salida se manda el último aviso
     * @param diasPagoPendiente cuántos días sin pagar esperan antes del aviso de pago pendiente
     */
    public ReglasRecordatorio(
            @Value("${travelgo.recordatorios.horas-antes:6}") int horasAntes,
            @Value("${travelgo.recordatorios.dias-pago-pendiente:2}") int diasPagoPendiente) {
        if (horasAntes <= 0 || horasAntes >= 24) {
            throw new IllegalArgumentException("travelgo.recordatorios.horas-antes debe estar entre 1 y 23");
        }
        if (diasPagoPendiente < 0) {
            throw new IllegalArgumentException("travelgo.recordatorios.dias-pago-pendiente no puede ser negativo");
        }
        this.ventanaHoras = Duration.ofHours(horasAntes);
        this.diasPagoPendiente = diasPagoPendiente;
    }

    /**
     * Avisos que corresponden a la reserva ahora.
     *
     * @param reserva   la reserva
     * @param fechaPago fecha del pago, vacía si no está pagada
     * @param ahora     momento de la revisión
     */
    public List<TipoAviso> avisosQueCorresponden(Reserva reserva, Optional<LocalDate> fechaPago,
                                                LocalDateTime ahora) {
        Objects.requireNonNull(reserva, "reserva");
        Objects.requireNonNull(ahora, "ahora");
        List<TipoAviso> avisos = new ArrayList<>();
        if (reserva.getEstadoEnum() == EstadoReserva.CANCELADA) {
            return avisos;
        }

        avisoDeViaje(reserva, ahora).ifPresent(avisos::add);

        LocalDate hoy = ahora.toLocalDate();
        if (fechaPago.isEmpty()
                && reserva.getEstadoEnum() == EstadoReserva.PENDIENTE
                && !reserva.getFechaReserva().isAfter(hoy.minusDays(diasPagoPendiente))) {
            avisos.add(TipoAviso.PAGO_PENDIENTE);
        }

        if (fechaPago.isPresent() && !fechaPago.get().isBefore(hoy.minusDays(1))) {
            avisos.add(TipoAviso.PAGO_CONFIRMADO);
        }
        return avisos;
    }

    /** Aviso de viaje según cuánto falta para la salida, si falta 7 días o menos. */
    Optional<TipoAviso> avisoDeViaje(Reserva reserva, LocalDateTime ahora) {
        Optional<LocalDateTime> salida = inicioDelViaje(reserva);
        if (salida.isEmpty() || !salida.get().isAfter(ahora)) {
            return Optional.empty();
        }
        Duration falta = Duration.between(ahora, salida.get());
        if (falta.compareTo(ventanaHoras) <= 0) {
            return Optional.of(TipoAviso.VIAJE_HORAS);
        }
        if (falta.compareTo(Duration.ofDays(1)) <= 0) {
            return Optional.of(TipoAviso.VIAJE_1_DIA);
        }
        if (falta.compareTo(Duration.ofDays(7)) <= 0) {
            return Optional.of(TipoAviso.VIAJE_7_DIAS);
        }
        return Optional.empty();
    }

    /**
     * Cuándo empieza el viaje: lo primero que ocurra entre la salida de los vuelos, la salida del transporte y
     * la entrada al hotel (a las 15:00 de su fecha de entrada). Vacío si ningún servicio tiene fecha.
     */
    public static Optional<LocalDateTime> inicioDelViaje(Reserva reserva) {
        return reserva.getServicios().stream()
                .map(ReglasRecordatorio::fechaDeInicio)
                .flatMap(Optional::stream)
                .min(LocalDateTime::compareTo);
    }

    private static Optional<LocalDateTime> fechaDeInicio(ServicioReservable servicio) {
        if (servicio instanceof Vuelo vuelo) {
            return Optional.ofNullable(vuelo.getFechaSalida());
        }
        if (servicio instanceof Transporte transporte) {
            return Optional.ofNullable(transporte.getFecha());
        }
        if (servicio instanceof Hotel hotel && hotel.getFechaEntrada() != null) {
            return Optional.of(hotel.getFechaEntrada().atTime(HORA_ENTRADA_HOTEL));
        }
        return Optional.empty();
    }
}
