package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de servicios reservables.
 * Cada servicio se guarda en dos tablas: servicio_reservable (datos comunes y tipo)
 * y la tabla de su subtipo (vuelo, hotel o transporte), que comparte el mismo id.
 */
@Repository
@Primary
public class ServicioReservableDAOJdbc implements ServicioReservableDAO {

    private static final String SELECT_SERVICIOS = """
        SELECT s.id, s.precio, s.disponible, s.tipo,
               s.fuente, s.id_externo, s.fecha_consulta, s.enlace_reserva,
               v.aerolinea, v.origen AS v_origen, v.destino AS v_destino,
               v.fecha_salida, v.fecha_llegada,
               h.nombre, h.direccion, h.tipo_habitacion, h.noches,
               t.tipo AS t_tipo, t.origen AS t_origen, t.destino AS t_destino, t.fecha
        FROM servicio_reservable s
        LEFT JOIN vuelo v ON v.id = s.id
        LEFT JOIN hotel h ON h.id = s.id
        LEFT JOIN transporte t ON t.id = s.id
        """;

    private final JdbcTemplate jdbc;

    public ServicioReservableDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public <T extends ServicioReservable> T guardar(T servicio) {
        try {
            jdbc.update("""
                INSERT INTO servicio_reservable (
                    id, precio, disponible, tipo,
                    fuente, id_externo, fecha_consulta, enlace_reserva
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    precio = EXCLUDED.precio,
                    disponible = EXCLUDED.disponible,
                    tipo = EXCLUDED.tipo,
                    fuente = EXCLUDED.fuente,
                    id_externo = EXCLUDED.id_externo,
                    fecha_consulta = EXCLUDED.fecha_consulta,
                    enlace_reserva = EXCLUDED.enlace_reserva
                """,
                servicio.getId(),
                servicio.getPrecio(),
                servicio.isDisponible(),
                tipoDe(servicio),
                servicio.getFuente(),
                servicio.getIdExterno(),
                servicio.getFechaConsulta(),
                servicio.getEnlaceReserva());

            switch (servicio) {
                case Vuelo vuelo -> guardarVuelo(vuelo);
                case Hotel hotel -> guardarHotel(hotel);
                case Transporte transporte -> guardarTransporte(transporte);
                default -> throw new IllegalArgumentException(
                        "Tipo de servicio no soportado: " + servicio.getClass().getSimpleName());
            }
            return servicio;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el servicio", e);
        }
    }

    @Override
    public Optional<ServicioReservable> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_SERVICIOS + " WHERE s.id = ?", this::mapear, id)
                    .stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el servicio " + id, e);
        }
    }

    @Override
    public List<ServicioReservable> listarTodos() {
        try {
            return jdbc.query(SELECT_SERVICIOS, this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los servicios", e);
        }
    }

    @Override
    public List<ServicioReservable> listarDisponibles() {
        try {
            return jdbc.query(SELECT_SERVICIOS + " WHERE s.disponible = TRUE", this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los servicios disponibles", e);
        }
    }

    @Override
    @Transactional
    public void eliminar(String id) {
        try {
            // Las tablas hijas se borran solas (ON DELETE CASCADE).
            // Falla si el servicio ya está dentro de una reserva.
            jdbc.update("DELETE FROM servicio_reservable WHERE id = ?", id);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo eliminar el servicio " + id, e);
        }
    }

    private void guardarVuelo(Vuelo v) {
        jdbc.update("""
                INSERT INTO vuelo (id, aerolinea, origen, destino, fecha_salida, fecha_llegada)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    aerolinea = EXCLUDED.aerolinea, origen = EXCLUDED.origen,
                    destino = EXCLUDED.destino, fecha_salida = EXCLUDED.fecha_salida,
                    fecha_llegada = EXCLUDED.fecha_llegada
                """, v.getId(), v.getAerolinea(), v.getOrigen(), v.getDestino(),
                v.getFechaSalida(), v.getFechaLlegada());
    }

    private void guardarHotel(Hotel h) {
        jdbc.update("""
                INSERT INTO hotel (id, nombre, direccion, tipo_habitacion, noches)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    nombre = EXCLUDED.nombre, direccion = EXCLUDED.direccion,
                    tipo_habitacion = EXCLUDED.tipo_habitacion, noches = EXCLUDED.noches
                """, h.getId(), h.getNombre(), h.getDireccion(), h.getTipoHabitacion(), h.getNoches());
    }

    private void guardarTransporte(Transporte t) {
        jdbc.update("""
                INSERT INTO transporte (id, tipo, origen, destino, fecha)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
                    tipo = EXCLUDED.tipo, origen = EXCLUDED.origen,
                    destino = EXCLUDED.destino, fecha = EXCLUDED.fecha
                """, t.getId(), t.getTipo(), t.getOrigen(), t.getDestino(), t.getFecha());
    }

    /** Valor de la columna tipo de servicio_reservable (VUELO, HOTEL o TRANSPORTE). */
    private String tipoDe(ServicioReservable servicio) {
        return switch (servicio) {
            case Vuelo v -> "VUELO";
            case Hotel h -> "HOTEL";
            case Transporte t -> "TRANSPORTE";
            default -> throw new IllegalArgumentException(
                    "Tipo de servicio no soportado: " + servicio.getClass().getSimpleName());
        };
    }

    /** Reconstruye el objeto del dominio según el tipo guardado en la fila. */
    private ServicioReservable mapear(ResultSet rs, int fila) throws SQLException {
        String id = rs.getString("id");
        double precio = rs.getDouble("precio");
        boolean disponible = rs.getBoolean("disponible");
        String tipo = rs.getString("tipo");

        String fuente = rs.getString("fuente");
        String idExterno = rs.getString("id_externo");
        LocalDateTime fechaConsulta =
            rs.getObject("fecha_consulta", LocalDateTime.class);
        String enlaceReserva = rs.getString("enlace_reserva");

        return switch (tipo) {
            case "VUELO" -> new Vuelo(
                id, precio, disponible,
                rs.getString("aerolinea"),
                rs.getString("v_origen"),
                rs.getString("v_destino"),
                rs.getObject("fecha_salida", LocalDateTime.class),
                rs.getObject("fecha_llegada", LocalDateTime.class),
                fuente, idExterno, fechaConsulta, enlaceReserva);

            case "HOTEL" -> new Hotel(
                id, precio, disponible,
                rs.getString("nombre"),
                rs.getString("direccion"),
                rs.getString("tipo_habitacion"),
                rs.getInt("noches"),
                fuente, idExterno, fechaConsulta, enlaceReserva);

            case "TRANSPORTE" -> new Transporte(
                id, precio, disponible,
                rs.getString("t_tipo"),
                rs.getString("t_origen"),
                rs.getString("t_destino"),
                rs.getObject("fecha", LocalDateTime.class),fuente, idExterno, fechaConsulta, enlaceReserva);

            default -> throw new SQLException(
                "Tipo de servicio desconocido en la BD: " + tipo);
        };
    }
}