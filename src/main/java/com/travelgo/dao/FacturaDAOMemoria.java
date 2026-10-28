package com.travelgo.dao;

import com.travelgo.model.Factura;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación temporal en memoria. En la Fase B se reemplaza por JDBC. */
@Repository
public class FacturaDAOMemoria implements FacturaDAO {

    private final Map<String, Factura> almacen = new LinkedHashMap<>();

    @Override
    public synchronized Factura guardar(Factura factura) {
        almacen.put(factura.getId(), factura);
        return factura;
    }

    @Override
    public synchronized Optional<Factura> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public synchronized Optional<Factura> buscarPorReservaId(String reservaId) {
        return almacen.values().stream()
                .filter(f -> f.getReserva().getId().equals(reservaId))
                .findFirst();
    }

    @Override
    public synchronized List<Factura> listarTodas() {
        return new ArrayList<>(almacen.values());
    }

    @Override
    public synchronized List<Factura> listarPorUsuario(String usuarioId) {
        return almacen.values().stream()
                .filter(f -> f.getReserva().getUsuario().getId().equals(usuarioId))
                .toList();
    }
}