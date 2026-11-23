package com.travelgo.recordatorio;

import com.travelgo.exception.AccesoDatosException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DAO JDBC de {@code envio_recordatorio}. La tabla la creó el Integrante 4 en schema.sql con
 * {@code UNIQUE (reserva_id, tipo, canal)}, así que la base de datos tampoco deja repetir un aviso.
 */
@Repository
public class EnvioRecordatorioDAOJdbc implements EnvioRecordatorioDAO {

    private final JdbcTemplate jdbc;

    public EnvioRecordatorioDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean yaEnviado(String reservaId, TipoAviso tipo, CanalEnvio canal) {
        try {
            Integer cuantos = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM envio_recordatorio
                    WHERE reserva_id = ? AND tipo = ? AND canal = ? AND estado = 'ENVIADO'
                    """, Integer.class, reservaId, tipo.name(), canal.name());
            return cuantos != null && cuantos > 0;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar los envíos de la reserva " + reservaId, e);
        }
    }

    @Override
    public void registrar(EnvioRecordatorio envio) {
        try {
            jdbc.update("""
                    INSERT INTO envio_recordatorio (id, usuario_id, reserva_id, tipo, canal, estado, fecha_envio)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (reserva_id, tipo, canal) DO UPDATE SET
                        estado = EXCLUDED.estado, fecha_envio = EXCLUDED.fecha_envio
                    """, envio.id(), envio.usuarioId(), envio.reservaId(), envio.tipo().name(),
                    envio.canal().name(), envio.estado().name(), envio.fechaEnvio());
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo registrar el envío del aviso " + envio.tipo(), e);
        }
    }

    @Override
    public List<EnvioRecordatorio> listarRecientes(int limite) {
        try {
            return jdbc.query("""
                    SELECT id, usuario_id, reserva_id, tipo, canal, estado, fecha_envio
                    FROM envio_recordatorio ORDER BY fecha_envio DESC LIMIT ?
                    """, this::mapear, limite);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los envíos de recordatorios", e);
        }
    }

    private EnvioRecordatorio mapear(ResultSet rs, int fila) throws SQLException {
        return new EnvioRecordatorio(
                rs.getString("id"),
                rs.getString("usuario_id"),
                rs.getString("reserva_id"),
                TipoAviso.valueOf(rs.getString("tipo")),
                CanalEnvio.valueOf(rs.getString("canal")),
                EstadoEnvio.valueOf(rs.getString("estado")),
                rs.getObject("fecha_envio", LocalDateTime.class));
    }
}
