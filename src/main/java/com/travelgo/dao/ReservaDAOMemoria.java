package com.travelgo.dao;

import com.travelgo.model.Reserva;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación temporal en memoria. En el Sprint 3 se reemplaza por JDBC. */
@Repository
public class ReservaDAOMemoria implements ReservaDAO {

    private final Map<String, Reserva> almacen = new LinkedHashMap<>();

    @Override
    public synchronized Reserva guardar(Reserva reserva) {
        almacen.put(reserva.getId(), reserva);
        return reserva;
    }

    @Override
    public synchronized Optional<Reserva> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public synchronized List<Reserva> listarTodas() {
        return new ArrayList<>(almacen.values());
    }

    @Override
    public synchronized List<Reserva> listarPorUsuario(String usuarioId) {
        return almacen.values().stream()
                .filter(r -> r.getUsuario().getId().equals(usuarioId))
                .toList();
    }
}