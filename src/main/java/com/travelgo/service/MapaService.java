package com.travelgo.service;

import com.travelgo.dto.MapaRespuestaDTO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.CiudadMapa;
import com.travelgo.proveedor.lugar.CiudadesMapa;
import com.travelgo.proveedor.lugar.LugarMapa;
import com.travelgo.proveedor.lugar.ProveedorLugares;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reúne los lugares del mapa: hoteles reales (Xotelo) y el resto de categorías (Geoapify).
 * Si una fuente falla, devuelve lo que sí pudo cargar y un aviso, en vez de romper todo el mapa.
 */
@Service
public class MapaService {

    private final CiudadesMapa ciudades;
    private final ProveedorLugares proveedorLugares;
    private final ProveedorHoteles proveedorHoteles;
    private static final int LIMITE_HOTELES = 100;

    public MapaService(CiudadesMapa ciudades, ProveedorLugares proveedorLugares,
                       ProveedorHoteles proveedorHoteles) {
        this.ciudades = ciudades;
        this.proveedorLugares = proveedorLugares;
        this.proveedorHoteles = proveedorHoteles;
    }

    public CiudadMapa ciudad(String id) {
        return ciudades.buscar(id).orElseThrow(() -> new RecursoNoEncontradoException("Ciudad no disponible: " + id));
    }

    public List<CiudadMapa> ciudades() {
        return ciudades.todas();
    }

    public MapaRespuestaDTO lugares(String ciudadId, Collection<CategoriaMapa> categorias, int radio, int limite) {
        CiudadMapa ciudad = ciudades.buscar(ciudadId).orElseThrow(
                () -> new RecursoNoEncontradoException("Ciudad no disponible en el mapa: " + ciudadId));
        Set<CategoriaMapa> pedidas = (categorias == null || categorias.isEmpty())
                ? EnumSet.of(CategoriaMapa.RESTAURANTE, CategoriaMapa.CAFE, CategoriaMapa.HOTEL, CategoriaMapa.SITIO_TURISTICO, CategoriaMapa.MUSEO) : EnumSet.copyOf(categorias);

        List<LugarMapa> lugares = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        for (CategoriaMapa categoria : pedidas) {
            try {
                if (categoria == CategoriaMapa.HOTEL) {
                    if (ciudad.xoteloClave() != null) {
                        lugares.addAll(hoteles(ciudad, LIMITE_HOTELES));
                    } else {
                        lugares.addAll(proveedorLugares.buscar(categoria, ciudad.latitud(), ciudad.longitud(), radio, limite));
                    }
                } else {
                    lugares.addAll(proveedorLugares.buscar(categoria, ciudad.latitud(), ciudad.longitud(),
                            radio, limite));
                }
            } catch (ProveedorNoDisponibleException e) {
                avisos.add(categoria.etiqueta() + ": " + e.getMessage());
            }
        }
        java.util.Map<String, LugarMapa> unicos = new java.util.LinkedHashMap<>();
        for (LugarMapa l : lugares) {
            if (!Double.isFinite(l.latitud()) || !Double.isFinite(l.longitud())) continue;
            // Los hoteles conservan el catálogo completo para localizar el hotel del paquete.
            if (l.categoria() != CategoriaMapa.HOTEL && LugarMapa.distanciaMetros(ciudad.latitud(), ciudad.longitud(), l.latitud(), l.longitud()) > radio) continue;
            String clave = l.fuente() + "|" + l.id();
            unicos.merge(clave, l, (a,b) -> a.categoria() == CategoriaMapa.SITIO_TURISTICO ? b : a);
        }
        return new MapaRespuestaDTO(ciudad, List.copyOf(unicos.values()), avisos);
    }

    private List<LugarMapa> hoteles(CiudadMapa ciudad, int limite) {
        return proveedorHoteles.buscarHoteles(ciudad.xoteloClave(), limite).stream()
                .filter(h -> h.latitud() != null && h.longitud() != null)
                .map(h -> new LugarMapa(h.clave(), h.nombre(), CategoriaMapa.HOTEL, h.latitud(), h.longitud(),
                        ciudad.nombre(), h.enlace(), h.calificacion(), "Xotelo (TripAdvisor)"))
                .toList();
    }
}