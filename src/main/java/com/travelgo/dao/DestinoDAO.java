package com.travelgo.dao;
import com.travelgo.model.Destino;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de Destino. */
public interface DestinoDAO {

    Destino guardar(Destino destino);

    Optional<Destino> buscarPorId(Long id);

    List<Destino> listarTodos();

    void eliminar(Long id);
}