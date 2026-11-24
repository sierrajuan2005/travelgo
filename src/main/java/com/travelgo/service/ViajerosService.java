package com.travelgo.service;

import com.travelgo.dao.ActividadDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.model.Actividad;
import com.travelgo.model.Destino;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Gusto;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.Viajero;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reglas de los viajeros de una reserva: quiénes viajan, qué les gusta y qué actividades pagas eligió cada uno.
 * Quién puede ver o cambiar la reserva lo revisa el controlador (igual que en el resto de reservas).
 */
@Service
public class ViajerosService {

    public static final int MAXIMO_VIAJEROS = 8;

    private final ReservaDAO reservaDAO;
    private final ActividadDAO actividadDAO;
    private final PagoDAO pagoDAO;

    public ViajerosService(ReservaDAO reservaDAO, ActividadDAO actividadDAO, PagoDAO pagoDAO) {
        this.reservaDAO = reservaDAO;
        this.actividadDAO = actividadDAO;
        this.pagoDAO = pagoDAO;
    }

    /** Lo que llega del formulario para una persona. */
    public record DatosViajero(String nombre, Set<Gusto> gustos, List<String> actividadesIds) {
    }

    /** Actividades pagas que se pueden elegir en los destinos del paquete de la reserva. */
    public List<Actividad> actividadesDisponibles(Reserva reserva) {
        return actividadesDe(reserva.getPaqueteTuristico());
    }

    /** Actividades pagas de los destinos de un paquete (también se muestran en el catálogo). */
    public List<Actividad> actividadesDe(PaqueteTuristico paquete) {
        return actividadDAO.listarPorDestinos(destinosIds(paquete));
    }

    /** true si todavía se pueden cambiar los viajeros: no está cancelada ni pagada. */
    public boolean editable(Reserva reserva) {
        return reserva.getEstadoEnum() != EstadoReserva.CANCELADA
                && pagoDAO.buscarPorReservaId(reserva.getId()).isEmpty();
    }

    /**
     * Cambia los viajeros de la reserva y guarda el nuevo total (servicios + actividades).
     * Solo acepta actividades de los destinos del paquete, aunque alguien cambie el formulario a mano.
     *
     * @throws IllegalArgumentException si los datos no son válidos
     * @throws IllegalStateException    si la reserva ya está pagada o cancelada
     */
    @Transactional
    public Reserva guardarViajeros(Reserva reserva, List<DatosViajero> datos) {
        if (!editable(reserva)) {
            throw new IllegalStateException("La reserva ya está pagada o cancelada; no se pueden cambiar los viajeros.");
        }
        List<DatosViajero> personas = datos == null ? List.of()
                : datos.stream().filter(Objects::nonNull).toList();
        if (personas.isEmpty()) {
            throw new IllegalArgumentException("Agrega al menos un viajero.");
        }
        if (personas.size() > MAXIMO_VIAJEROS) {
            throw new IllegalArgumentException("Una reserva puede tener máximo " + MAXIMO_VIAJEROS + " viajeros.");
        }

        Map<String, Actividad> permitidas = actividadesDisponibles(reserva).stream()
                .collect(Collectors.toMap(Actividad::id, Function.identity()));

        List<Viajero> viajeros = new ArrayList<>();
        for (DatosViajero persona : personas) {
            List<Actividad> elegidas = new ArrayList<>();
            for (String id : persona.actividadesIds() == null ? List.<String>of() : persona.actividadesIds()) {
                Actividad actividad = permitidas.get(id);
                if (actividad == null) {
                    throw new IllegalArgumentException("La actividad " + id + " no está disponible para este paquete.");
                }
                elegidas.add(actividad);
            }
            viajeros.add(new Viajero(persona.nombre(), persona.gustos(), elegidas));
        }

        reserva.reemplazarViajeros(viajeros);
        return reservaDAO.guardar(reserva);
    }

    static List<Long> destinosIds(PaqueteTuristico paquete) {
        if (paquete == null || paquete.getDestinos() == null) {
            return List.of();
        }
        return paquete.getDestinos().stream().map(Destino::getId).filter(Objects::nonNull).toList();
    }
}
