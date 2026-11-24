package com.travelgo.proveedor.lugar;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ciudades de la demostración. Las claves de Bogotá, Cusco y Cancún son de TripAdvisor:
 * confírmalas con la verificación de Xotelo (la de Cartagena ya está probada).
 */
@Component
public class CiudadesMapa {

    private final Map<String, CiudadMapa> ciudades = new LinkedHashMap<>();

    private com.travelgo.service.DestinoService destinos;
    private com.travelgo.proveedor.destinos.Geocodificador geocodificador;
    private final com.travelgo.proveedor.destinos.CacheConVencimiento<String, CiudadMapa> cache =
            new com.travelgo.proveedor.destinos.CacheConVencimiento<>(java.time.Duration.ofDays(7));

    @org.springframework.beans.factory.annotation.Autowired
    public CiudadesMapa(com.travelgo.service.DestinoService destinos, com.travelgo.proveedor.destinos.Geocodificador geocodificador) {
        this();
        this.destinos = destinos;
        this.geocodificador = geocodificador;
    }

    public CiudadesMapa() {
        agregar(new CiudadMapa("cartagena", "Cartagena", "Colombia", 10.3910, -75.4794, "g297476", 13));
        agregar(new CiudadMapa("bogota", "Bogotá", "Colombia", 4.7110, -74.0721, "g294074", 12));
        agregar(new CiudadMapa("cusco", "Cusco", "Perú", -13.5319, -71.9675, "g294314", 13));
        agregar(new CiudadMapa("cancun", "Cancún", "México", 21.1619, -86.8515, "g150807", 12));
    }

    private void agregar(CiudadMapa ciudad) {
        ciudades.put(ciudad.id(), ciudad);
    }

    public List<CiudadMapa> todas() {
        return List.copyOf(ciudades.values());
    }

    public Optional<CiudadMapa> buscar(String id) {
        if (id != null && id.matches("destino-[0-9]+") && destinos != null) {
            var d = destinos.obtener(Long.parseLong(id.substring(8)));
            String clave = id + "|" + d.getCiudad() + "|" + d.getPais();
            return Optional.of(cache.obtener(clave, () -> {
                var centro = geocodificador.ubicar(d.getCiudad(), d.getPais()).orElseThrow(() ->
                        new com.travelgo.exception.RecursoNoEncontradoException("No se pudo localizar " + d.getCiudad()));
                String hotel = com.travelgo.service.CodigosDestino.de(d.getCiudad())
                        .map(com.travelgo.service.CodigosDestino.Codigos::claveHoteles).orElse(null);
                return new CiudadMapa(id, d.getCiudad(), d.getPais(), centro.latitud(), centro.longitud(), hotel, 13);
            }));
        }
        return Optional.ofNullable(id == null ? null : ciudades.get(id.toLowerCase(java.util.Locale.ROOT)));
    }
}