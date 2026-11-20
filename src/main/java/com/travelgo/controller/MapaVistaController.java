package com.travelgo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página del mapa interactivo. */
@Controller
public class MapaVistaController {

    private final String estiloUrl;

    public MapaVistaController(
            @Value("${travelgo.mapa.estilo-url:https://tiles.openfreemap.org/styles/liberty}") String estiloUrl) {
        this.estiloUrl = estiloUrl;
    }

    @GetMapping("/mapa")
    public String mapa(Model model) {
        model.addAttribute("estiloUrl", estiloUrl);
        return "mapa";
    }
}