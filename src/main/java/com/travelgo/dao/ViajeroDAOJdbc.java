package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Actividad;
import com.travelgo.model.Gusto;
import com.travelgo.model.Viajero;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Guarda los viajeros en reserva_viajero y sus actividades en viajero_actividad.
 * Los gustos van en una sola columna separados por comas (son pocos y siempre se leen juntos).
 */
@Repository
public class ViajeroDAOJdbc implements ViajeroDAO {

    private final JdbcTemplate jdbc;
    private final ActividadDAO actividadDAO;

    public ViajeroDAOJdbc(JdbcTemplate jdbc, ActividadDAO actividadDAO) {
        this.jdbc = jdbc;
        this.actividadDAO = actividadDAO;
    }

    @Override
    public List<Viajero> listarPorReserva(String reservaId) {
        try {
            return jdbc.query("SELECT id, nombre, gustos FROM reserva_viajero WHERE reserva_id = ? ORDER BY orden",
                    (rs, fila) -> {
                        String id = rs.getString("id");
                        return new Viajero(id, rs.getString("nombre"), gustos(rs.getString("gustos")),
                                actividades(id));
                    }, reservaId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudieron leer los viajeros de la reserva " + reservaId, e);
        }
    }

    @Override
    public void reemplazar(String reservaId, List<Viajero> viajeros) {
        try {
            // ON DELETE CASCADE también borra sus filas de viajero_actividad
            jdbc.update("DELETE FROM reserva_viajero WHERE reserva_id = ?", reservaId);
            int orden = 0;
            for (Viajero viajero : viajeros) {
                jdbc.update("INSERT INTO reserva_viajero (id, reserva_id, nombre, gustos, orden) VALUES (?, ?, ?, ?, ?)",
                        viajero.getId(), reservaId, viajero.getNombre(), texto(viajero.getGustos()), orden++);
                for (Actividad actividad : viajero.getActividades()) {
                    jdbc.update("INSERT INTO viajero_actividad (viajero_id, actividad_id) VALUES (?, ?)",
                            viajero.getId(), actividad.id());
                }
            }
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudieron guardar los viajeros de la reserva " + reservaId, e);
        }
    }

    private List<Actividad> actividades(String viajeroId) {
        List<Actividad> actividades = new ArrayList<>();
        for (String id : jdbc.queryForList(
                "SELECT actividad_id FROM viajero_actividad WHERE viajero_id = ?", String.class, viajeroId)) {
            actividadDAO.buscarPorId(id).ifPresent(actividades::add);
        }
        return actividades;
    }

    static String texto(Set<Gusto> gustos) {
        return gustos.stream().map(Gusto::name).collect(Collectors.joining(","));
    }

    /** Lee "FIESTA,CULTURA"; ignora valores que ya no existan en el enum. */
    static Set<Gusto> gustos(String texto) {
        Set<Gusto> gustos = EnumSet.noneOf(Gusto.class);
        if (texto == null || texto.isBlank()) {
            return gustos;
        }
        Arrays.stream(texto.split(",")).map(String::trim).forEach(nombre -> {
            for (Gusto g : Gusto.values()) {
                if (g.name().equals(nombre)) {
                    gustos.add(g);
                }
            }
        });
        return gustos;
    }
}
