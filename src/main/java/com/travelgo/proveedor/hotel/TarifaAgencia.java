package com.travelgo.proveedor.hotel;

/** Precio por habitación y por noche que muestra una agencia (Booking.com, Trip.com...). */
public record TarifaAgencia(String agencia, double precioPorNoche) {
}