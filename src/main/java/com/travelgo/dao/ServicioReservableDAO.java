package com.travelgo.dao;

import com.travelgo.model.ServicioReservable;

import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de los servicios reservables. */
public interface ServicioReservableDAO {

    <T extends ServicioReservable> T guardar(T servicio);

    Optional<ServicioReservable> buscarPorId(String id);

    List<ServicioReservable> listarTodos();

    List<ServicioReservable> listarDisponibles();

    void eliminar(String id);
}