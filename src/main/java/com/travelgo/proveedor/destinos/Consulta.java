package com.travelgo.proveedor.destinos;

import java.time.LocalDateTime;

/**
 * Resultado de consultar una fuente externa, con la fuente y la fecha de la consulta.
 * Regla del Sprint 4: todo dato real se muestra con su fuente y su fecha de consulta; nunca como algo garantizado.
 *
 * @param datos        lo que devolvió la fuente
 * @param fuente       nombre de la fuente (por ejemplo "Open-Meteo")
 * @param atribucion   texto de atribución que pide la fuente
 * @param enlaceFuente sitio web de la fuente
 * @param consultadoEn momento en que se llamó a la API (si viene de la caché, es la hora de la consulta original)
 * @param <T>          tipo de dato consultado
 */
public record Consulta<T>(T datos, String fuente, String atribucion, String enlaceFuente,
		LocalDateTime consultadoEn) {

	/**
	 * Arma la consulta con la información de atribución de la fuente y la hora actual.
	 */
	public static <T> Consulta<T> de(T datos, FuenteExterna fuente) {
		return new Consulta<>(datos, fuente.nombreFuente(), fuente.atribucion(), fuente.enlaceFuente(),
			LocalDateTime.now());
	}
}
