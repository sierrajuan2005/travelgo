package com.travelgo.proveedor.destinos;

/**
 * Latitud y longitud de un lugar, en grados decimales.
 *
 * @param latitud  entre -90 y 90
 * @param longitud entre -180 y 180
 */
public record Coordenadas(double latitud, double longitud) {

	/**
	 * @throws IllegalArgumentException si la latitud o la longitud están fuera de rango
	 */
	public Coordenadas {
		if (latitud < -90 || latitud > 90) {
			throw new IllegalArgumentException("La latitud debe estar entre -90 y 90.");
		}
		if (longitud < -180 || longitud > 180) {
			throw new IllegalArgumentException("La longitud debe estar entre -180 y 180.");
		}
	}
}
