package com.travelgo.proveedor.lugar;

/** Ciudad disponible en el mapa, con su centro y la clave de TripAdvisor que usa Xotelo. */
public record CiudadMapa(String id, String nombre, String pais, double latitud, double longitud,
                         String xoteloClave, int zoom) {
}