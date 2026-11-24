package com.travelgo.expo;

import java.time.LocalDateTime;

/**
 * Un viaje lanzado, en el JSON que leen el celular y la pantalla del stand.
 * {@code invitadoPor} es el nombre de quien le compartió la postal ("se unió al viaje de Jaime"); null si no.
 */
public record ViajeLanzadoDTO(long id, String nombre, String destinoId, String ciudad, String pais,
                              double latitud, double longitud, LocalDateTime fecha, String filtroCatalogo,
                              String aeropuerto, String invitadoPor) {

    public static ViajeLanzadoDTO de(LanzamientoService.ViajeLanzado viaje) {
        Lanzamiento l = viaje.lanzamiento();
        DestinoSonado d = viaje.destino();
        return new ViajeLanzadoDTO(l.id(), l.nombre(), d.id(), d.ciudad(), d.pais(), d.latitud(), d.longitud(),
                l.fecha(), d.filtroCatalogo(), d.aeropuerto(), l.invitadoPorNombre());
    }
}
