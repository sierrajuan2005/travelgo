package com.travelgo.controller;

import com.travelgo.dto.MapaRespuestaDTO;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.CiudadMapa;
import com.travelgo.service.MapaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Datos del mapa: ciudades disponibles y lugares por categoría. */
@RestController
@RequestMapping("/api/mapa")
public class MapaController {

    private final MapaService service;
    private static final int RADIO_METROS = 5000;
    private static final int LIMITE = 30;

    public MapaController(MapaService service) {
        this.service = service;
    }

    @GetMapping("/ciudades")
    public List<CiudadMapa> ciudades() {
        return service.ciudades();
    }

    @GetMapping("/ciudad")
    public CiudadMapa ciudad(@RequestParam String id) { return service.ciudad(id); }

    /** Ejemplo: /api/mapa/lugares?ciudad=cartagena&categorias=RESTAURANTE,CAFE. Sin categorías trae todas. */
    @GetMapping("/lugares")
    public MapaRespuestaDTO lugares(@RequestParam String ciudad,
                                    @RequestParam(required = false) List<CategoriaMapa> categorias,
                                    @RequestParam(defaultValue = "5000") int radio,
                                    @RequestParam(defaultValue = "30") int limite) {
        return service.lugares(ciudad, categorias, Math.max(100, Math.min(20000, radio)), Math.max(1, Math.min(50, limite)));
    }
}