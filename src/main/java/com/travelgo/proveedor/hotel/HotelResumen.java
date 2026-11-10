package com.travelgo.proveedor.hotel;

/** Datos básicos de un hotel real, tal como los entrega la fuente. */
public record HotelResumen(String clave, String nombre, String tipo, String imagen,
                           String enlace, Double latitud, Double longitud, Double calificacion) {
}