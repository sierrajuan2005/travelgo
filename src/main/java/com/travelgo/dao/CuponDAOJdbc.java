package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Cupon;
import com.travelgo.model.TipoCupon;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Implementación JDBC del DAO de cupones. */
@Repository
@Primary
public class CuponDAOJdbc implements CuponDAO {

    private static final String SELECT_CUPON = """
            SELECT id, codigo, tipo, valor, fecha_inicio, fecha_fin, usos_maximos, usos_actuales, activo
            FROM cupon
            """;

    private final JdbcTemplate jdbc;

    public CuponDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Cupon guardar(Cupon cupon) {
        try {
            jdbc.update("""
                    INSERT INTO cupon (id, codigo, tipo, valor, fecha_inicio, fecha_fin,
                                       usos_maximos, usos_actuales, activo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                        usos_actuales = EXCLUDED.usos_actuales, activo = EXCLUDED.activo
                    """, cupon.getId(), cupon.getCodigo(), cupon.getTipo().name(), cupon.getValor(),
                    cupon.getFechaInicio(), cupon.getFechaFin(), cupon.getUsosMaximos(),
                    cupon.getUsosActuales(), cupon.isActivo());
            return cupon;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el cupón", e);
        }
    }

    @Override
    public Optional<Cupon> buscarPorId(String id) {
        try {
            return jdbc.query(SELECT_CUPON + " WHERE id = ?", this::mapear, id).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el cupón " + id, e);
        }
    }

    @Override
    public Optional<Cupon> buscarPorCodigo(String codigo) {
        try {
            return jdbc.query(SELECT_CUPON + " WHERE codigo = ?", this::mapear, codigo).stream().findFirst();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el cupón " + codigo, e);
        }
    }

    @Override
    public List<Cupon> listarTodos() {
        try {
            return jdbc.query(SELECT_CUPON + " ORDER BY fecha_inicio DESC", this::mapear);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los cupones", e);
        }
    }

    private Cupon mapear(ResultSet rs, int fila) throws SQLException {
        return new Cupon(rs.getString("id"), rs.getString("codigo"),
                TipoCupon.valueOf(rs.getString("tipo")), rs.getDouble("valor"),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                rs.getInt("usos_maximos"), rs.getInt("usos_actuales"), rs.getBoolean("activo"));
    }
}