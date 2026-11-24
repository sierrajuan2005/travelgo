package com.travelgo.expo;

import java.time.LocalDateTime;

/**
 * Un viaje lanzado por un visitante en la ExpoSoftware (fila de {@code lanzamiento_expo}).
 *
 * @param oculto   true si un administrador lo quitó de la pantalla (por ejemplo, un nombre ofensivo)
 * @param telefono celular que el visitante dejó (con autorización) para recibir su postal y ofertas;
 *                 null si no dejó. Nunca sale en la pantalla ni en la API pública.
 */
public record Lanzamiento(long id, String nombre, String destinoId, LocalDateTime fecha, boolean oculto,
                          String telefono) {

    /** Lanzamiento sin contacto. */
    public Lanzamiento(long id, String nombre, String destinoId, LocalDateTime fecha, boolean oculto) {
        this(id, nombre, destinoId, fecha, oculto, null);
    }
}
