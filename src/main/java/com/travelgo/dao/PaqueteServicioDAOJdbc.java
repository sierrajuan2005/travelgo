package com.travelgo.dao;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.ServicioReservable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/** Implementación JDBC de la unión paquete-servicio (tabla paquete_servicio). */
@Repository
public class PaqueteServicioDAOJdbc implements PaqueteServicioDAO {

    private final JdbcTemplate jdbc;
    private final ServicioReservableDAO servicioDAO;

    public PaqueteServicioDAOJdbc(JdbcTemplate jdbc, ServicioReservableDAO servicioDAO) {
        this.jdbc = jdbc;
        this.servicioDAO = servicioDAO;
    }

    @Override
    public void vincular(Long paqueteId, String servicioId) {
        try {
            jdbc.update("""
                    INSERT INTO paquete_servicio (paquete_id, servicio_id) VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """, paqueteId, servicioId);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo vincular el servicio al paquete " + paqueteId, e);
        }
    }

    @Override
    public List<ServicioReservable> listarServicios(Long paqueteId) {
        try {
            List<String> ids = jdbc.queryForList(
                    "SELECT servicio_id FROM paquete_servicio WHERE paquete_id = ?", String.class, paqueteId);
            List<ServicioReservable> servicios = new ArrayList<>();
            for (String id : ids) {
                servicioDAO.buscarPorId(id).ifPresent(servicios::add);
            }
            return servicios;
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los servicios del paquete " + paqueteId, e);
        }
    }
}