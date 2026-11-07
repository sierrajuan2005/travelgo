package com.travelgo.proveedor.destinos;

/**
 * Consulta el clima actual y el pronóstico de un lugar.
 */
public interface ProveedorClima extends FuenteExterna {

	/**
	 * @param lugar coordenadas del lugar
	 * @return clima actual y pronóstico de los próximos días
	 * @throws ProveedorExternoException si la API falla o no responde
	 */
	Clima consultarClima(Coordenadas lugar);
}
