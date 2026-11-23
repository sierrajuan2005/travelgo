package com.travelgo.proveedor.lugar;

/** Un lugar para dibujar en el mapa, venga de la fuente que venga. */
public record LugarMapa(String id, String nombre, CategoriaMapa categoria, double latitud, double longitud,
                        String direccion, String enlace, Double calificacion, String fuente) {
}