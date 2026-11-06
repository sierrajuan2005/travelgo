package com.travelgo.proveedor.destinos;

import java.util.List;

/**
 * Toda la información real de un destino: foto, clima y atracciones, cada una con su fuente y su fecha.
 * Si una API falla, esa parte queda en null y en {@code avisos} queda el motivo; lo demás se muestra igual.
 *
 * @param destinoId   id del destino en TravelGo
 * @param nombre      nombre del destino
 * @param ciudad      ciudad del destino
 * @param pais        país del destino
 * @param ubicacion   coordenadas del centro de la ciudad (null si no se pudo ubicar)
 * @param foto        foto del destino (null si no hay o la API falló)
 * @param clima       clima actual y pronóstico (null si la API falló)
 * @param atracciones atracciones cercanas (null si la API falló)
 * @param avisos      mensajes claros de lo que no se pudo consultar
 */
public record InfoDestino(Long destinoId, String nombre, String ciudad, String pais, Coordenadas ubicacion,
		Consulta<FotoDestino> foto, Consulta<Clima> clima, Consulta<List<Atraccion>> atracciones,
		List<String> avisos) {

	public InfoDestino {
		avisos = avisos == null ? List.of() : List.copyOf(avisos);
	}
}
