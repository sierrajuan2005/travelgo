package com.travelgo.dao;

import com.travelgo.model.Actividad;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Catálogo de actividades pagas por destino. */
public interface ActividadDAO {

    /** Actividades activas de esos destinos, ordenadas por gusto y precio. */
    List<Actividad> listarPorDestinos(Collection<Long> destinosIds);

    Optional<Actividad> buscarPorId(String id);
}
