package com.travelgo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Página del mapa interactivo. */
@Controller
public class MapaVistaController {

    @GetMapping("/mapa")
    public String mapa() {
        return "mapa";
    }
}