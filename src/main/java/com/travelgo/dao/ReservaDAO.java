package com.travelgo.dao;

import com.travelgo.model.Reserva;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de Reserva. */
public interface ReservaDAO {

    Reserva guardar(Reserva reserva);

    Optional<Reserva> buscarPorId(String id);

    List<Reserva> listarTodas();

    List<Reserva> listarPorUsuario(String usuarioId);
}

