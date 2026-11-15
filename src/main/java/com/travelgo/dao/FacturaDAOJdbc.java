package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Cupon;
import com.travelgo.model.EstadoFactura;
import com.travelgo.model.Factura;
import com.travelgo.model.Reserva;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Implementación JDBC del DAO de facturas. */
@Repository
@Primary
public class FacturaDAOJdbc implements FacturaDAO {

    private static final String SELECT_FACTURA = """
            SELECT f.id, f.numero, f.fecha_emision, f.subtotal, f.descuento, f.total,
                   f.estado, f.reserva_id, f.cupon_id
            FROM factura f
            """;

    private final JdbcTemplate jdbc;
    private final ReservaDAO reservaDAO;
    private final CuponDAO cuponDAO;

    public FacturaDAOJdbc(JdbcTemplate jdbc, ReservaDAO reservaDAO, CuponDAO cuponDAO) {
        this.jdbc = jdbc;
        this.reservaDAO = reservaDAO;
        this.cuponDAO = cuponDAO;
    }

    @Override
    public Factura guardar(Factura factura) {
        try {
            String cuponId = factura.getCupon() == null ? null : factura.getCupon().getId();
            jdbc.update("""
                    INSERT INTO factura (id, numero, fecha_emision, subtotal, descuento, total,
                                         estado, reserva_id, cupon_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        subtotal = EXCLUDED.subtotal, descuento = EXCLUDED.descuento,
                        total = EXCLUDED.total, estado = EXCLUDED.estado, cupon_id = EXCLUDED.cupon_id
                    """, factura.getId(), factura.getNumero(), factura.getFechaEmision(),
                    factura.getSubtotal(), factura.getDescuento(), factura.getTotal(),
                    factura.getEstado().name(), factura.getReserva().getId(), cuponId);
            return factura;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar la factura", e);
        }
    }

    @Override
    public Optional<Factura> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_FACTURA + " WHERE f.id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar la factura " + id, e);
        }
    }

    @Override
    public Optional<Factura> buscarPorReservaId(String reservaId) {
        try {
            return jdbc.query(SELECT_FACTURA + " WHERE f.reserva_id = ?", this::mapear, reservaId)
                    .stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar la factura de la reserva " + reservaId, e);
        }
    }

    @Override
    public List<Factura> listarTodas() {
        try {
            return jdbc.query(SELECT_FACTURA + " ORDER BY f.fecha_emision DESC", this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar las facturas", e);
        }
    }

    @Override
    public List<Factura> listarPorUsuario(String usuarioId) {
        try {
            return jdbc.query(SELECT_FACTURA
                    + " JOIN reserva r ON r.id = f.reserva_id WHERE r.usuario_id = ?"
                    + " ORDER BY f.fecha_emision DESC", this::mapear, usuarioId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar las facturas del usuario " + usuarioId, e);
        }
    }

    private Factura mapear(ResultSet rs, int fila) throws SQLException {
        String id = rs.getString("id");
        Reserva reserva = reservaDAO.buscarPorId(rs.getString("reserva_id"))
                .orElseThrow(() -> new IllegalStateException(
                        "La factura " + id + " apunta a una reserva que no existe"));

        String cuponId = rs.getString("cupon_id");
        Cupon cupon = cuponId == null ? null : cuponDAO.buscarPorId(cuponId).orElse(null);

        return new Factura(id, rs.getString("numero"), reserva,
                rs.getObject("fecha_emision", LocalDate.class),
                rs.getDouble("subtotal"), rs.getDouble("total"),
                EstadoFactura.valueOf(rs.getString("estado")),
                rs.getDouble("descuento"), cupon);
    }
}