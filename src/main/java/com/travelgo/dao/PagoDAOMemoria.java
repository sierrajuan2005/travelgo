package com.travelgo.dao;

import com.travelgo.model.Pago;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación temporal en memoria. En la Fase B se reemplaza por JDBC. */
@Repository
public class PagoDAOMemoria implements PagoDAO {

    private final Map<String, Pago> almacen = new LinkedHashMap<>();

    @Override
    public synchronized Pago guardar(Pago pago) {
        almacen.put(pago.getId(), pago);
        return pago;
    }

    @Override
    public synchronized Optional<Pago> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public synchronized Optional<Pago> buscarPorReservaId(String reservaId) {
        return almacen.values().stream()
                .filter(p -> p.getReserva().getId().equals(reservaId))
                .findFirst();
    }

    @Override
    public synchronized List<Pago> listarTodos() {
        return new ArrayList<>(almacen.values());
    }
}