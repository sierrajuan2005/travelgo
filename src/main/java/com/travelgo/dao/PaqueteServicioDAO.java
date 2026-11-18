package com.travelgo.dao;

import java.util.List;

/**
 * Acceso a la tabla {@code paquete_servicio}: qué servicios (vuelo, hotel, transporte) componen un paquete.
 * La llena el constructor de paquetes con datos reales (Sprint 4).
 */
public interface PaqueteServicioDAO {

    /** Une un servicio a un paquete. Si ya estaban unidos, no hace nada. */
    void asociar(Long paqueteId, String servicioId);

    /** Ids de los servicios del paquete, en el orden en que se agregaron. Vacío si no tiene. */
    List<String> serviciosDe(Long paqueteId);
}
