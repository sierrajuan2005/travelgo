package com.travelgo.dto;

import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.proveedor.hotel.GamaHotel;

import java.util.List;

/** Paquete con sus servicios reales, su precio de referencia (en pesos) y la gama de su hotel. */
public record PaqueteRealRespuestaDTO(Long id, String nombre, String descripcion, double precioReferencia,
                                      String moneda, int duracionDias, boolean activo, List<String> destinos,
                                      List<ServicioReferenciaDTO> servicios, GamaHotel gama) {

    public static PaqueteRealRespuestaDTO desde(PaqueteTuristico p, List<ServicioReferenciaDTO> servicios,
                                                GamaHotel gama) {
        return new PaqueteRealRespuestaDTO(p.getId(), p.getNombre(), p.getDescripcion(),
                p.getPrecio().doubleValue(), "COP", p.getDuracionDias(), p.isActivo(),
                p.getDestinos().stream().map(Destino::getNombre).toList(), servicios, gama);
    }

    /** Versión sin gama, para quien no la necesita. */
    public static PaqueteRealRespuestaDTO desde(PaqueteTuristico p, List<ServicioReservable> servicios) {
        return desde(p, servicios.stream().map(ServicioReferenciaDTO::desde).toList(), null);
    }
}