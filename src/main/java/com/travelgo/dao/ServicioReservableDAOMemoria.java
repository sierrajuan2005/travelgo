package com.travelgo.dao;

import com.travelgo.model.ServicioReservable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Implementación temporal en memoria. En el Sprint 3 se reemplaza por JDBC. */
@Repository
public class ServicioReservableDAOMemoria implements ServicioReservableDAO {

    private final Map<String, ServicioReservable> almacen = new LinkedHashMap<>();

    @Override
    public synchronized <T extends ServicioReservable> T guardar(T servicio) {
        if (servicio == null) {
            throw new IllegalArgumentException("El servicio no puede ser nulo.");
        }

        almacen.put(servicio.getId(), servicio);
        return servicio;
    }

    @Override
    public synchronized Optional<ServicioReservable> buscarPorId(String id) {
        return Optional.ofNullable(almacen.get(id));
    }

    @Override
    public synchronized List<ServicioReservable> listarTodos() {
        return new ArrayList<>(almacen.values());
    }

    @Override
    public synchronized List<ServicioReservable> listarDisponibles() {
        return almacen.values().stream().filter(ServicioReservable::isDisponible).toList();
    }

    @Override
    public synchronized void eliminar(String id) {
        almacen.remove(id);
    }
}