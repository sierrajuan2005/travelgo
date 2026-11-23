package com.travelgo.expo;

import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.destinos.InfoDestino;
import com.travelgo.service.InfoDestinoService;
import com.travelgo.service.PaqueteService;
import com.travelgo.util.Textos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Datos reales para la página del destino que ve el visitante cuando su avión aterriza (/expo/viaje/{id}).
 * No inventa nada nuevo: reutiliza lo que ya tiene TravelGo.
 *
 * - Foto, clima y atracciones: {@link InfoDestinoService} (Wikipedia, Open-Meteo), con su caché.
 * - Paquetes: los paquetes activos del catálogo que van a esa ciudad (mismo criterio del filtro del catálogo).
 */
@Service
public class DestinoExpoService {

    private static final Logger LOG = LoggerFactory.getLogger(DestinoExpoService.class);

    private final InfoDestinoService infoDestinoService;
    private final PaqueteService paqueteService;

    public DestinoExpoService(InfoDestinoService infoDestinoService, PaqueteService paqueteService) {
        this.infoDestinoService = infoDestinoService;
        this.paqueteService = paqueteService;
    }

    /**
     * Foto, clima y atracciones. Si una API falla, esa parte queda vacía y la página muestra lo demás
     * (InfoDestinoService deja el motivo en {@code avisos}). Si falla algo inesperado, se devuelve solo lo básico.
     */
    public InfoDestino info(DestinoSonado destino) {
        // Destino de paso, no se guarda: InfoDestinoService busca por nombre, ciudad y país
        Destino consulta = new Destino(destino.ciudad(), null, destino.pais(), destino.ciudad());
        try {
            return infoDestinoService.consultar(consulta);
        } catch (RuntimeException e) {
            // En la expo la página del destino nunca debe romperse: sin datos reales se muestra lo básico
            LOG.warn("No se pudo consultar la información de {}: {}", destino.ciudad(), e.getMessage());
            return new InfoDestino(null, destino.ciudad(), destino.ciudad(), destino.pais(), null, null, null, null,
                    List.of("No se pudo consultar la información del destino en este momento."));
        }
    }

    /** Paquetes activos que van a ese destino, del más barato al más caro. Vacío si TravelGo no vende allí. */
    public List<PaqueteTuristico> paquetes(DestinoSonado destino) {
        if (!destino.tienePaquetes()) {
            return List.of();
        }
        String texto = Textos.normalizar(destino.filtroCatalogo());
        return paqueteService.listar(true).stream()
                .filter(p -> p.getDestinos().stream().anyMatch(d ->
                        Textos.normalizar(d.getCiudad()).contains(texto)
                                || Textos.normalizar(d.getNombre()).contains(texto)))
                .sorted(Comparator.comparing(PaqueteTuristico::getPrecio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** Los paquetes activos más baratos de TravelGo (para sugerir cuando no hay paquetes al destino soñado). */
    public List<PaqueteTuristico> masBaratos(int cuantos) {
        return paqueteService.listar(true).stream()
                .filter(p -> p.getPrecio() != null)
                .sorted(Comparator.comparing(PaqueteTuristico::getPrecio))
                .limit(cuantos)
                .toList();
    }
}
