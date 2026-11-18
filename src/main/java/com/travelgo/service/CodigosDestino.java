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
    }

    private CodigosDestino() {
    }

    /** Códigos conocidos de una ciudad (sin importar tildes ni mayúsculas). */
    public static Optional<Codigos> de(String ciudad) {
        return Optional.ofNullable(POR_CIUDAD.get(Textos.normalizar(ciudad)));
    }
}
