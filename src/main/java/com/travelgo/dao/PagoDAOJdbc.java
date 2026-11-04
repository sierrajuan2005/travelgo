package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.EstadoPago;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Implementación JDBC del DAO de pagos. */
@Repository
@Primary
public class PagoDAOJdbc implements PagoDAO {

    private static final String SELECT_PAGO = """
            SELECT p.id, p.monto, p.fecha_pago, p.metodo, p.estado, p.factura_id
            FROM pago p
            """;

    private final JdbcTemplate jdbc;
    private final FacturaDAO facturaDAO;

    public PagoDAOJdbc(JdbcTemplate jdbc, FacturaDAO facturaDAO) {
        this.jdbc = jdbc;
        this.facturaDAO = facturaDAO;
    }

    @Override
    public Pago guardar(Pago pago) {
        try {
            jdbc.update("""
                    INSERT INTO pago (id, monto, fecha_pago, metodo, estado, factura_id)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET estado = EXCLUDED.estado
                    """, pago.getId(), pago.getMonto(), pago.getFechaPago(),
                    pago.getMetodo().name(), pago.getEstado().name(), pago.getFactura().getId());
            return pago;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el pago", e);
        }
    }

    @Override
    public Optional<Pago> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_PAGO + " WHERE p.id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el pago " + id, e);
        }
    }

    @Override
    public Optional<Pago> buscarPorReservaId(String reservaId) {
        try {
            return jdbc.query(SELECT_PAGO + " JOIN factura f ON f.id = p.factura_id WHERE f.reserva_id = ?",
                    this::mapear, reservaId).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el pago de la reserva " + reservaId, e);
        }
    }

    @Override
    public List<Pago> listarTodos() {
        try {
            return jdbc.query(SELECT_PAGO + " ORDER BY p.fecha_pago DESC", this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los pagos", e);
        }
    }

    private Pago mapear(ResultSet rs, int fila) throws SQLException {
        String id = rs.getString("id");
        Factura factura = facturaDAO.buscarPorId(rs.getString("factura_id"))
                .orElseThrow(() -> new IllegalStateException(
                        "El pago " + id + " apunta a una factura que no existe"));

        return new Pago(id, factura, rs.getDouble("monto"),
                MetodoPago.valueOf(rs.getString("metodo")),
                rs.getObject("fecha_pago", LocalDate.class),
                EstadoPago.valueOf(rs.getString("estado")));
    }
}