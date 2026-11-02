package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC del DAO de reservas. Guarda la reserva en la tabla reserva y sus servicios
 * en la tabla intermedia reserva_servicio.
 */
@Repository
@Primary
public class ReservaDAOJdbc implements ReservaDAO {

    private static final String SELECT_RESERVA =
            "SELECT id, fecha_reserva, estado, total, usuario_id, paquete_id FROM reserva";

    private final JdbcTemplate jdbc;
    private final UsuarioDAO usuarioDAO;
    private final PaqueteDAO paqueteDAO;
    private final ServicioReservableDAO servicioDAO;

    public ReservaDAOJdbc(JdbcTemplate jdbc, UsuarioDAO usuarioDAO,
                          PaqueteDAO paqueteDAO, ServicioReservableDAO servicioDAO) {
        this.jdbc = jdbc;
        this.usuarioDAO = usuarioDAO;
        this.paqueteDAO = paqueteDAO;
        this.servicioDAO = servicioDAO;
    }

    @Override
    @Transactional
    public Reserva guardar(Reserva reserva) {
        try {
            PaqueteTuristico paquete = reserva.getPaqueteTuristico();
            Long paqueteId = paquete == null ? null : paquete.getId();

            jdbc.update("""
                    INSERT INTO reserva (id, fecha_reserva, estado, total, usuario_id, paquete_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        estado = EXCLUDED.estado, total = EXCLUDED.total, paquete_id = EXCLUDED.paquete_id
                    """, reserva.getId(), reserva.getFechaReserva(), reserva.getEstadoEnum().name(),
                    reserva.getTotal(), reserva.getUsuario().getId(), paqueteId);

            // Se reescribe la lista de servicios: se borran los anteriores y se insertan los actuales
            jdbc.update("DELETE FROM reserva_servicio WHERE reserva_id = ?", reserva.getId());
            for (ServicioReservable servicio : reserva.getServicios()) {
                jdbc.update("INSERT INTO reserva_servicio (reserva_id, servicio_id) VALUES (?, ?)",
                        reserva.getId(), servicio.getId());
            }
            return reserva;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar la reserva", e);
        }
    }

    @Override
    public Optional<Reserva> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_RESERVA + " WHERE id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar la reserva " + id, e);
        }
    }

    @Override
    public List<Reserva> listarTodas() {
        try {
            return jdbc.query(SELECT_RESERVA + " ORDER BY fecha_reserva DESC", this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar las reservas", e);
        }
    }

    @Override
    public List<Reserva> listarPorUsuario(String usuarioId) {
        try {
            return jdbc.query(SELECT_RESERVA + " WHERE usuario_id = ? ORDER BY fecha_reserva DESC",
                    this::mapear, usuarioId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar las reservas del usuario " + usuarioId, e);
        }
    }

    /** Reconstruye la reserva con el constructor de la base de datos de Integrante 1. */
    private Reserva mapear(ResultSet rs, int fila) throws SQLException {
        String id = rs.getString("id");

        Usuario usuario = usuarioDAO.buscarPorId(rs.getString("usuario_id"))
                .orElseThrow(() -> new IllegalStateException(
                        "La reserva " + id + " apunta a un usuario que no existe"));

        Long paqueteId = rs.getObject("paquete_id", Long.class);
        PaqueteTuristico paquete = paqueteId == null ? null : paqueteDAO.buscarPorId(paqueteId).orElse(null);

        List<ServicioReservable> servicios = new ArrayList<>();
        List<String> idsServicios = jdbc.queryForList(
                "SELECT servicio_id FROM reserva_servicio WHERE reserva_id = ?", String.class, id);
        for (String idServicio : idsServicios) {
            servicioDAO.buscarPorId(idServicio).ifPresent(servicios::add);
        }

        return new Reserva(id, usuario, paquete, servicios,
                rs.getObject("fecha_reserva", LocalDate.class),
                EstadoReserva.valueOf(rs.getString("estado")),
                rs.getDouble("total"));
    }
}