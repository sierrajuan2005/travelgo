package com.travelgo.dao;

import com.travelgo.model.Cupon;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de Cupon. */
public interface CuponDAO {

    Cupon guardar(Cupon cupon);

    Optional<Cupon> buscarPorId(String id);

    Optional<Cupon> buscarPorCodigo(String codigo);

    List<Cupon> listarTodos();
}