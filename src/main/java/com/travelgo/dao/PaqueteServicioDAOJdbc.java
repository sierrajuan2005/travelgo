package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Implementación JDBC de {@link PaqueteServicioDAO} sobre la tabla {@code paquete_servicio}. */
@Repository
public class PaqueteServicioDAOJdbc implements PaqueteServicioDAO {

    private final JdbcTemplate jdbc;

    public PaqueteServicioDAOJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void asociar(Long paqueteId, String servicioId) {
        try {
            jdbc.update("""
                INSERT INTO paquete_servicio (paquete_id, servicio_id) VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """, paqueteId, servicioId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo unir el servicio " + servicioId
                    + " al paquete " + paqueteId, e);
        }
    }

    @Override
    public List<String> serviciosDe(Long paqueteId) {
        try {
            // Primero el vuelo, luego el hotel y luego el transporte
            return jdbc.queryForList("""
                SELECT ps.servicio_id
                FROM paquete_servicio ps
                JOIN servicio_reservable s ON s.id = ps.servicio_id
                WHERE ps.paquete_id = ?
                ORDER BY CASE s.tipo WHEN 'VUELO' THEN 1 WHEN 'HOTEL' THEN 2 ELSE 3 END
                """, String.class, paqueteId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar los servicios del paquete " + paqueteId, e);
        }
    }
}
