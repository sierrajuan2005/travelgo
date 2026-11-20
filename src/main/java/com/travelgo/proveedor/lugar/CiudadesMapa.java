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
        return Optional.ofNullable(id == null ? null : ciudades.get(id.toLowerCase()));
    }
}