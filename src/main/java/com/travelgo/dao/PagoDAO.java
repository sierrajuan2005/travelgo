package com.travelgo.dao;

import com.travelgo.model.Pago;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de Pago. */
public interface PagoDAO {

    Pago guardar(Pago pago);

    Optional<Pago> buscarPorId(String id);

    Optional<Pago> buscarPorReservaId(String reservaId);

    List<Pago> listarTodos();
}