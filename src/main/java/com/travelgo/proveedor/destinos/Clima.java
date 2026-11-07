package com.travelgo.proveedor.destinos;

import java.time.LocalDate;
import java.util.List;

/**
 * Clima actual y pronóstico de los próximos días de un destino. Temperaturas en grados Celsius.
 *
 * @param temperaturaActual temperatura en este momento
 * @param descripcion       estado del cielo en español (por ejemplo "Lluvia")
 * @param icono             nombre del ícono de Bootstrap Icons (por ejemplo "bi-cloud-rain")
 * @param dias              pronóstico por día, empezando por hoy
 */
public record Clima(double temperaturaActual, String descripcion, String icono, List<Dia> dias) {

	public Clima {
		dias = dias == null ? List.of() : List.copyOf(dias);
	}

	/**
	 * Pronóstico de un día.
	 *
	 * @param fecha       día del pronóstico
	 * @param minima      temperatura mínima
	 * @param maxima      temperatura máxima
	 * @param descripcion estado del cielo en español
	 * @param icono       nombre del ícono de Bootstrap Icons
	 */
	public record Dia(LocalDate fecha, double minima, double maxima, String descripcion, String icono) {
	}
}
