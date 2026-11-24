package com.travelgo.service;

import com.travelgo.util.Textos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Códigos que piden las APIs para cada ciudad: el código IATA del aeropuerto (Ignav) y la clave de la ciudad
 * en TripAdvisor (Xotelo). La tabla destino solo guarda ciudad y país, así que el constructor de paquetes
 * propone estos códigos y el administrador los puede corregir en el formulario (para una ciudad nueva).
 */
public final class CodigosDestino {

    /** Código IATA y clave de ciudad de Xotelo. */
    public record Codigos(String iata, String claveHoteles) {
    }

    /** Ciudades de salida que ofrece el formulario (código IATA → nombre). */
    public static final Map<String, String> ORIGENES = new LinkedHashMap<>();

    private static final Map<String, Codigos> POR_CIUDAD = new LinkedHashMap<>();

    static {
        ORIGENES.put("BOG", "Bogotá");
        ORIGENES.put("MDE", "Medellín");
        ORIGENES.put("CLO", "Cali");
        ORIGENES.put("CTG", "Cartagena");
        ORIGENES.put("BAQ", "Barranquilla");
        ORIGENES.put("BGA", "Bucaramanga");

        // Los destinos de data.sql
        POR_CIUDAD.put("cartagena", new Codigos("CTG", "g297476"));
        POR_CIUDAD.put("bogota", new Codigos("BOG", "g294074"));
        POR_CIUDAD.put("cusco", new Codigos("CUZ", "g294314"));
        POR_CIUDAD.put("cancun", new Codigos("CUN", "g150807"));
        // Agregados con el constructor el 2026-11-23 (claves de Xotelo verificadas con su API)
        POR_CIUDAD.put("santa marta", new Codigos("SMR", "g297484"));
        POR_CIUDAD.put("medellin", new Codigos("MDE", "g297478"));
        POR_CIUDAD.put("punta cana", new Codigos("PUJ", "g147293"));
        POR_CIUDAD.put("miami", new Codigos("MIA", "g34438"));
        POR_CIUDAD.put("madrid", new Codigos("MAD", "g187514"));
        POR_CIUDAD.put("dubai", new Codigos("DXB", "g295424"));
        POR_CIUDAD.put("santa marta", new Codigos("SMR", "g297484"));
        POR_CIUDAD.put("medellin", new Codigos("MDE", "g297478"));
        POR_CIUDAD.put("ciudad de mexico", new Codigos("MEX", "g150800"));
        POR_CIUDAD.put("punta cana", new Codigos("PUJ", "g147293"));
        POR_CIUDAD.put("miami", new Codigos("MIA", "g34438"));
        POR_CIUDAD.put("nueva york", new Codigos("JFK", "g60763"));
        POR_CIUDAD.put("rio de janeiro", new Codigos("GIG", "g303506"));
        POR_CIUDAD.put("buenos aires", new Codigos("EZE", "g312741"));
        POR_CIUDAD.put("madrid", new Codigos("MAD", "g187514"));
        POR_CIUDAD.put("paris", new Codigos("CDG", "g187147"));
        POR_CIUDAD.put("londres", new Codigos("LHR", "g186338"));
        POR_CIUDAD.put("roma", new Codigos("FCO", "g187791"));
        POR_CIUDAD.put("el cairo", new Codigos("CAI", "g294201"));
        POR_CIUDAD.put("tokio", new Codigos("HND", "g298184"));
        POR_CIUDAD.put("seul", new Codigos("ICN", "g294197"));
        POR_CIUDAD.put("sidney", new Codigos("SYD", "g255060"));
        POR_CIUDAD.put("san andres", new Codigos("ADZ", "g297482"));
    }

    private CodigosDestino() {
    }

    /** Códigos conocidos de una ciudad (sin importar tildes ni mayúsculas). */
    public static Optional<Codigos> de(String ciudad) {
        return Optional.ofNullable(POR_CIUDAD.get(Textos.normalizar(ciudad)));
    }
}
