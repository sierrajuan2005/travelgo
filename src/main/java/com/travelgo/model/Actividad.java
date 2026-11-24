package com.travelgo.model;

/**
 * Experiencia opcional con precio que se puede agregar a un viajero (tour, entrada, noche de bares...).
 * Pertenece a un destino y a un gusto, para sugerirla solo a quien le interesa.
 *
 * @param id            identificador (por ejemplo "roma-trastevere-noche")
 * @param destinoId     destino donde se hace
 * @param nombre        nombre corto
 * @param descripcion   qué incluye
 * @param gusto         gusto al que pertenece
 * @param precio        precio por persona en COP
 * @param duracionHoras duración aproximada
 */
public record Actividad(String id, Long destinoId, String nombre, String descripcion, Gusto gusto,
                        double precio, int duracionHoras) {

    public Actividad {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El identificador de la actividad es obligatorio.");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la actividad es obligatorio.");
        }
        if (gusto == null) {
            throw new IllegalArgumentException("La actividad debe tener un gusto.");
        }
        if (!Double.isFinite(precio) || precio < 0) {
            throw new IllegalArgumentException("El precio de la actividad no puede ser negativo.");
        }
        if (duracionHoras < 1) {
            throw new IllegalArgumentException("La actividad debe durar al menos una hora.");
        }
    }
}
