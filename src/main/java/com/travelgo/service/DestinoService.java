package com.travelgo.service;
import com.travelgo.dao.DestinoDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reglas de negocio del catálogo de destinos. */
@Service
public class DestinoService {

    private final DestinoDAO destinoDAO;

    public DestinoService(DestinoDAO destinoDAO) {
        this.destinoDAO = destinoDAO;
    }

    public Destino crear(Destino destino) {
        destino.setId(null);
        return destinoDAO.guardar(destino);
    }

    public Destino obtener(Long id) {
        return destinoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Destino no encontrado: " + id));
    }

    public List<Destino> listar() {
        return destinoDAO.listarTodos();
    }

    public Destino actualizar(Long id, Destino datos) {
        Destino existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setPais(datos.getPais());
        existente.setCiudad(datos.getCiudad());
        return destinoDAO.guardar(existente);
    }

    public void eliminar(Long id) {
        obtener(id);
        destinoDAO.eliminar(id);
    }
}