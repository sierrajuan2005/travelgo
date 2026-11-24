package com.travelgo.expo;

import java.time.LocalDateTime;

/**
 * Un viaje lanzado por un visitante en la ExpoSoftware (fila de {@code lanzamiento_expo}).
 *
 * @param oculto            true si un administrador lo quitó de la pantalla (por ejemplo, un nombre ofensivo)
 * @param telefono          celular que el visitante dejó (con autorización) para recibir ofertas; null si no dejó.
 *                          Nunca sale en la pantalla ni en la API pública.
 * @param invitadoPor       id del viaje de quien le compartió la postal ("se unió al viaje de Jaime"); null si
 *                          llegó por el QR del stand
 * @param invitadoPorNombre nombre de quien lo invitó (para la pantalla); null si no lo invitaron
 */
public record Lanzamiento(long id, String nombre, String destinoId, LocalDateTime fecha, boolean oculto,
                          String telefono, Long invitadoPor, String invitadoPorNombre) {

    /** Lanzamiento sin contacto ni invitación. */
    public Lanzamiento(long id, String nombre, String destinoId, LocalDateTime fecha, boolean oculto) {
        this(id, nombre, destinoId, fecha, oculto, null, null, null);
    }

    /** Lanzamiento sin invitación. */
    public Lanzamiento(long id, String nombre, String destinoId, LocalDateTime fecha, boolean oculto,
                       String telefono) {
        this(id, nombre, destinoId, fecha, oculto, telefono, null, null);
    }
}
