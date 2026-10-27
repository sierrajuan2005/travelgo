package com.travelgo.dao;
import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.repository.PaqueteRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Implementación del DAO de PaqueteTuristico sobre Spring Data JPA. */
@Repository
public class PaqueteDAOImpl implements PaqueteDAO {

    private final PaqueteRepository repository;

    public PaqueteDAOImpl(PaqueteRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaqueteTuristico guardar(PaqueteTuristico paquete) {
        try {
            return repository.save(paquete);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el paquete", e);
        }
    }

    @Override
    public Optional<PaqueteTuristico> buscarPorId(Long id) {
        try {
            return repository.findById(id);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el paquete " + id, e);
        }
    }

    @Override
    public List<PaqueteTuristico> listarTodos() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los paquetes", e);
        }
    }

    @Override
    public List<PaqueteTuristico> listarActivos() {
        try {
            return repository.findByActivoTrue();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los paquetes activos", e);
        }
    }
}
