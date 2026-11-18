package com.travelgo.dto;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import java.util.List;

/** Paquete con sus servicios reales y el precio de referencia (en pesos colombianos). */
public record PaqueteRealRespuestaDTO(Long id, String nombre, String descripcion, double precioReferencia,
                                      String moneda, int duracionDias, boolean activo, List<String> destinos,
                                      List<ServicioReferenciaDTO> servicios) {

    public static PaqueteRealRespuestaDTO desde(PaqueteTuristico p, List<ServicioReservable> servicios) {
        return new PaqueteRealRespuestaDTO(p.getId(), p.getNombre(), p.getDescripcion(),
                p.getPrecio().doubleValue(), "COP", p.getDuracionDias(), p.isActivo(),
                p.getDestinos().stream().map(Destino::getNombre).toList(),
                servicios.stream().map(ServicioReferenciaDTO::desde).toList());
    }
}