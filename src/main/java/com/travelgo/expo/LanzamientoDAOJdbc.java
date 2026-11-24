package com.travelgo.expo;

import com.travelgo.exception.AccesoDatosException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** DAO JDBC de {@code lanzamiento_expo} (tabla en schema.sql, sección ExpoSoftware). */
@Repository
public class LanzamientoDAOJdbc implements LanzamientoDAO {

    private static final String COLUMNAS = "id, nombre, destino_id, fecha, oculto, telefono";

    private final JdbcTemplate jdbc;

    public LanzamientoDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long guardar(String nombre, String destinoId, LocalDateTime fecha) {
        try {
            Long id = jdbc.queryForObject("""
                    INSERT INTO lanzamiento_expo (nombre, destino_id, fecha, oculto)
                    VALUES (?, ?, ?, FALSE) RETURNING id
                    """, Long.class, nombre, destinoId, fecha);
            if (id == null) {
                throw new AccesoDatosException("La base de datos no devolvió el id del lanzamiento", null);
            }
            return id;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el viaje lanzado", e);
        }
    }

    @Override
    public Optional<Lanzamiento> buscar(long id) {
        try {
            return jdbc.query("SELECT " + COLUMNAS + " FROM lanzamiento_expo WHERE id = ?", this::mapear, id)
                    .stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo buscar el viaje lanzado " + id, e);
        }
    }

    @Override
    public List<Lanzamiento> recientesVisibles(int limite) {
        try {
            List<Lanzamiento> lista = new ArrayList<>(jdbc.query(
                    "SELECT " + COLUMNAS + " FROM lanzamiento_expo WHERE NOT oculto ORDER BY id DESC LIMIT ?",
                    this::mapear, limite));
            Collections.reverse(lista);
            return lista;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los viajes lanzados", e);
        }
    }

    @Override
    public List<Lanzamiento> visiblesDesde(long desdeId, int limite) {
        try {
            return jdbc.query(
                    "SELECT " + COLUMNAS + " FROM lanzamiento_expo WHERE NOT oculto AND id > ? ORDER BY id LIMIT ?",
                    this::mapear, desdeId, limite);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los viajes nuevos", e);
        }
    }

    @Override
    public List<Lanzamiento> ultimos(int limite) {
        try {
            return jdbc.query("SELECT " + COLUMNAS + " FROM lanzamiento_expo ORDER BY id DESC LIMIT ?",
                    this::mapear, limite);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los viajes lanzados", e);
        }
    }

    @Override
    public long contarVisibles() {
        return contar("SELECT COUNT(*) FROM lanzamiento_expo WHERE NOT oculto");
    }

    @Override
    public long contarVisiblesDe(String destinoId) {
        return contar("SELECT COUNT(*) FROM lanzamiento_expo WHERE NOT oculto AND destino_id = ?", destinoId);
    }

    @Override
    public boolean cambiarOculto(long id, boolean oculto) {
        try {
            return jdbc.update("UPDATE lanzamiento_expo SET oculto = ? WHERE id = ?", oculto, id) > 0;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo cambiar el viaje lanzado " + id, e);
        }
    }

    @Override
    public boolean guardarContacto(long id, String telefono, LocalDateTime fecha) {
        try {
            return jdbc.update("UPDATE lanzamiento_expo SET telefono = ?, fecha_contacto = ? WHERE id = ?",
                    telefono, telefono == null ? null : fecha, id) > 0;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el contacto del viaje " + id, e);
        }
    }

    @Override
    public void marcarVioDestino(long id) {
        marcar("UPDATE lanzamiento_expo SET vio_destino = TRUE WHERE id = ? AND NOT vio_destino", id);
    }

    @Override
    public void marcarAbrioPaquete(long id) {
        marcar("UPDATE lanzamiento_expo SET abrio_paquete = TRUE WHERE id = ? AND NOT abrio_paquete", id);
    }

    @Override
    public void guardarWhatsApp(long id, String idMensaje, String estado, LocalDateTime fecha) {
        marcar("UPDATE lanzamiento_expo SET whatsapp_id = ?, whatsapp_estado = ?, whatsapp_fecha = ? WHERE id = ?",
                idMensaje, estado, fecha, id);
    }

    @Override
    public void actualizarEstadoWhatsApp(long id, String estado) {
        marcar("UPDATE lanzamiento_expo SET whatsapp_estado = ? WHERE id = ?", estado, id);
    }

    @Override
    public Optional<EnvioWhatsApp> envioWhatsApp(long id) {
        try {
            return jdbc.query("""
                    SELECT whatsapp_id, whatsapp_estado, whatsapp_fecha FROM lanzamiento_expo
                    WHERE id = ? AND whatsapp_id IS NOT NULL
                    """, (rs, fila) -> new EnvioWhatsApp(rs.getString("whatsapp_id"), rs.getString("whatsapp_estado"),
                    rs.getObject("whatsapp_fecha", LocalDateTime.class)), id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el WhatsApp del viaje " + id, e);
        }
    }

    @Override
    public Embudo embudo() {
        try {
            return jdbc.queryForObject("""
                    SELECT COUNT(*) AS lanzaron,
                           COUNT(*) FILTER (WHERE vio_destino) AS vieron,
                           COUNT(telefono) AS contacto,
                           COUNT(*) FILTER (WHERE whatsapp_estado IN ('ENTREGADO', 'LEIDO')) AS whatsapp,
                           COUNT(*) FILTER (WHERE abrio_paquete) AS paquete
                    FROM lanzamiento_expo WHERE NOT oculto
                    """, (rs, fila) -> new Embudo(rs.getLong("lanzaron"), rs.getLong("vieron"),
                    rs.getLong("contacto"), rs.getLong("whatsapp"), rs.getLong("paquete")));
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo calcular el embudo del stand", e);
        }
    }

    private long contar(String sql, Object... parametros) {
        try {
            Long cuantos = jdbc.queryForObject(sql, Long.class, parametros);
            return cuantos == null ? 0 : cuantos;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo contar los viajes lanzados", e);
        }
    }

    private void marcar(String sql, Object... parametros) {
        try {
            jdbc.update(sql, parametros);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo registrar el paso del viaje", e);
        }
    }

    private Lanzamiento mapear(ResultSet rs, int fila) throws SQLException {
        return new Lanzamiento(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("destino_id"),
                rs.getObject("fecha", LocalDateTime.class),
                rs.getBoolean("oculto"),
                rs.getString("telefono"));
    }
}
