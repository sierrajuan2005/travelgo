package com.travelgo.dao;
import com.travelgo.exception.AccesoDatosException;
import com.travelgo.model.Destino;
import com.travelgo.repository.DestinoRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Implementación del DAO de Destino sobre Spring Data JPA. */
@Repository
public class DestinoDAOImpl implements DestinoDAO {

    private final DestinoRepository repository;

    public DestinoDAOImpl(DestinoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Destino guardar(Destino destino) {
        try {
            return repository.save(destino);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo guardar el destino", e);
        }
    }

    @Override
    public Optional<Destino> buscarPorId(Long id) {
        try {
            return repository.findById(id);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo consultar el destino " + id, e);
        }
    }

    @Override
    public List<Destino> listarTodos() {
        try {
            return repository.findAll();
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo listar los destinos", e);
        }
    }

    @Override
    public void eliminar(Long id) {
        try {
            repository.deleteById(id);
        } catch (DataAccessException e) {
            throw new AccesoDatosException("No se pudo eliminar el destino " + id, e);
        }
    }
}