package com.travelgo.estado;

import java.util.List;
import java.util.Optional;

/**
 * Una API externa que vigila el panel de estado (/admin/estado).
 *
 * @param id           clave corta para la URL ("open-meteo-clima")
 * @param nombre       nombre para mostrar ("Open-Meteo · clima")
 * @param clase        nombre simple de la clase del adaptador; así se reconoce el bean que hay que vigilar
 * @param paraQueSirve qué parte de TravelGo usa esta API
 * @param siFalla      qué hace la app cuando la API se cae (lo que se le muestra al profesor)
 * @param cuota        límite de uso de la API (texto)
 * @param forma        cómo avisa este adaptador cuando falla (para simular la caída igual que la real)
 * @param sePuedeProbar false si una prueba gasta algo que no vuelve (la cuota de Ignav)
 * @param cacheInterna  true si el adaptador tiene su propia caché adentro (Xotelo, Geoapify, Ignav): entonces el
 *                      proxy también ve las respuestas de esa caché, y las que tardan casi nada se marcan como caché
 */
public record FuenteMonitoreada(String id, String nombre, String clase, String paraQueSirve, String siFalla,
                                String cuota, FormaDeFallar forma, boolean sePuedeProbar, boolean cacheInterna) {

    /** Cómo falla cada familia de adaptadores. La caída simulada se comporta igual que una caída real. */
    public enum FormaDeFallar {
        /** Adaptadores de destinos (clima, fotos, atracciones): lanzan ProveedorExternoException. */
        EXCEPCION_DESTINOS,
        /** Hoteles, lugares y tasa de cambio: lanzan ProveedorNoDisponibleException. */
        EXCEPCION_PROVEEDOR,
        /** Ignav: no lanza nada, escribe en el log y devuelve una lista vacía o null. */
        RESPUESTA_VACIA
    }

    /** Las APIs de TravelGo, en el orden del panel. */
    public static final List<FuenteMonitoreada> TODAS = List.of(
            new FuenteMonitoreada("open-meteo-clima", "Open-Meteo · clima", "OpenMeteoClima",
                    "Clima actual y pronóstico de cada destino (catálogo, página del destino, postal).",
                    "La página se ve sin clima y con un aviso. Lo consultado en la última hora sigue en caché.",
                    "Gratis, sin llave (uso no comercial).", FormaDeFallar.EXCEPCION_DESTINOS, true, false),
            new FuenteMonitoreada("open-meteo-ubicacion", "Open-Meteo · ubicación", "OpenMeteoGeocodificador",
                    "Coordenadas de cada ciudad, para pedir el clima y las atracciones.",
                    "Las ciudades nuevas quedan sin clima ni atracciones. Las ya ubicadas siguen en caché 7 días.",
                    "Gratis, sin llave.", FormaDeFallar.EXCEPCION_DESTINOS, true, false),
            new FuenteMonitoreada("wikipedia-fotos", "Wikipedia · fotos", "WikipediaFotos",
                    "Foto real de cada destino, con su autor y licencia.",
                    "Las tarjetas muestran el ícono de siempre y la postal se dibuja sin foto. Caché de 24 h.",
                    "Gratis, sin llave.", FormaDeFallar.EXCEPCION_DESTINOS, true, false),
            new FuenteMonitoreada("wikipedia-atracciones", "Wikipedia · atracciones", "WikipediaAtracciones",
                    "Lugares para visitar cerca del centro de cada destino.",
                    "La página del destino se ve sin la lista de lugares. Caché de 24 h.",
                    "Gratis, sin llave.", FormaDeFallar.EXCEPCION_DESTINOS, true, false),
            new FuenteMonitoreada("opentripmap", "OpenTripMap · atracciones", "OpenTripMapAtracciones",
                    "Atracciones (solo si existe OPENTRIPMAP_API_KEY; si no, se usa Wikipedia).",
                    "La página del destino se ve sin la lista de lugares.",
                    "Gratis con llave.", FormaDeFallar.EXCEPCION_DESTINOS, true, false),
            new FuenteMonitoreada("xotelo", "Xotelo · hoteles", "XoteloProveedorHoteles",
                    "Hoteles reales y sus precios (constructor de paquetes y mapa).",
                    "El constructor avisa que no hay hoteles y el mapa muestra los demás lugares.",
                    "Gratis, sin llave (no oficial).", FormaDeFallar.EXCEPCION_PROVEEDOR, true, true),
            new FuenteMonitoreada("geoapify", "Geoapify · lugares", "GeoapifyProveedorLugares",
                    "Restaurantes, cafés, museos y sitios del mapa.",
                    "El mapa muestra lo que sí cargó y un aviso por cada categoría que falló. Caché en la base.",
                    "3.000 consultas gratis al día, con llave.", FormaDeFallar.EXCEPCION_PROVEEDOR, true, true),
            new FuenteMonitoreada("tasa-cambio", "open.er-api · dólar a pesos", "OpenErApiProveedorTasaCambio",
                    "Tasa real de USD a COP para los precios de los hoteles.",
                    "El constructor sigue sin convertir y avisa que no hay tasa de cambio.",
                    "Gratis, sin llave (se actualiza una vez al día).", FormaDeFallar.EXCEPCION_PROVEEDOR, true, false),
            new FuenteMonitoreada("ignav", "Ignav · vuelos", "IgnavProveedorVuelos",
                    "Vuelos reales y su enlace de reserva (constructor de paquetes).",
                    "El constructor arma el paquete sin vuelo (el vuelo es opcional; el hotel no).",
                    "1.000 consultas compartidas que no se recuperan: el panel nunca la prueba.",
                    FormaDeFallar.RESPUESTA_VACIA, false, true));

    public static Optional<FuenteMonitoreada> porClase(Class<?> clase) {
        // Los beans pueden venir envueltos (CGLIB): se compara con la clase y sus padres
        for (Class<?> c = clase; c != null && c != Object.class; c = c.getSuperclass()) {
            String nombre = c.getSimpleName();
            Optional<FuenteMonitoreada> f = TODAS.stream().filter(x -> x.clase().equals(nombre)).findFirst();
            if (f.isPresent()) {
                return f;
            }
        }
        return Optional.empty();
    }

    public static Optional<FuenteMonitoreada> porId(String id) {
        return TODAS.stream().filter(f -> f.id().equals(id)).findFirst();
    }
}
