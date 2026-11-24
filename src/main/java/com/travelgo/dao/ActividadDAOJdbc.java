package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Actividad;
import com.travelgo.model.Gusto;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Lee la tabla actividad (las filas vienen de data.sql). */
@Repository
public class ActividadDAOJdbc implements ActividadDAO {

    private static final String SELECT =
            "SELECT id, destino_id, nombre, descripcion, gusto, precio, duracion_horas FROM actividad";

    private final JdbcTemplate jdbc;

    public ActividadDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Actividad> listarPorDestinos(Collection<Long> destinosIds) {
        if (destinosIds == null || destinosIds.isEmpty()) {
            return Collections.emptyList();
        }
        String signos = String.join(",", Collections.nCopies(destinosIds.size(), "?"));
        try {
            return jdbc.query(SELECT + " WHERE activa AND destino_id IN (" + signos + ") ORDER BY gusto, precio",
                    this::mapear, destinosIds.toArray());
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudieron listar las actividades", e);
        }
    }

    @Override
    public Optional<Actividad> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT + " WHERE id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar la actividad " + id, e);
        }
    }

    private Actividad mapear(ResultSet rs, int fila) throws SQLException {
        return new Actividad(rs.getString("id"), rs.getLong("destino_id"), rs.getString("nombre"),
                rs.getString("descripcion"), Gusto.valueOf(rs.getString("gusto")),
                rs.getDouble("precio"), rs.getInt("duracion_horas"));
    }
}
