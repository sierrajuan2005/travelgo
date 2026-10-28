package com.travelgo.dao;

import com.travelgo.model.Factura;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de Factura. */
public interface FacturaDAO {

    Factura guardar(Factura factura);

    Optional<Factura> buscarPorId(String id);

    Optional<Factura> buscarPorReservaId(String reservaId);

    List<Factura> listarTodas();

    List<Factura> listarPorUsuario(String usuarioId);
}