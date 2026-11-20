package com.travelgo.dto;

import com.travelgo.proveedor.lugar.CiudadMapa;
import com.travelgo.proveedor.lugar.LugarMapa;

import java.util.List;

/** Lugares de una ciudad para dibujar en el mapa, más los avisos de las fuentes que fallaron. */
public record MapaRespuestaDTO(CiudadMapa ciudad, List<LugarMapa> lugares, List<String> avisos) {
}