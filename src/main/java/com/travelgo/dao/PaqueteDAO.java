package com.travelgo.dao;
import com.travelgo.model.PaqueteTuristico;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de PaqueteTuristico. */
public interface PaqueteDAO {

    PaqueteTuristico guardar(PaqueteTuristico paquete);

    Optional<PaqueteTuristico> buscarPorId(Long id);

    List<PaqueteTuristico> listarTodos();

    List<PaqueteTuristico> listarActivos();
}