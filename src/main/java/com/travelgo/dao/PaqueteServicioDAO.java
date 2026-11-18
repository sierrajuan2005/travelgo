package com.travelgo.dao;

import com.travelgo.model.ServicioReservable;

import java.util.List;

/** Contrato de la unión entre paquetes (JPA) y los servicios que incluyen (JDBC). */
public interface PaqueteServicioDAO {

    void vincular(Long paqueteId, String servicioId);

    List<ServicioReservable> listarServicios(Long paqueteId);
}