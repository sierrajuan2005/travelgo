package com.travelgo.dao;

import com.travelgo.model.Viajero;

import java.util.List;

/** Viajeros de una reserva, con sus gustos y actividades. */
public interface ViajeroDAO {

    List<Viajero> listarPorReserva(String reservaId);

    /** Borra los viajeros que tenía la reserva y guarda estos (en el mismo orden). */
    void reemplazar(String reservaId, List<Viajero> viajeros);
}
