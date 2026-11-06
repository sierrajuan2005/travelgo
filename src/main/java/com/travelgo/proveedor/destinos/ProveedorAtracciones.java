package com.travelgo.proveedor.destinos;

import java.util.List;

/**
 * Busca atracciones reales cerca de un punto. Hay un adaptador por API (OpenTripMap, Wikipedia):
 * para usar otra API se crea otro adaptador sin tocar InfoDestinoService (principio abierto/cerrado).
 */
public interface ProveedorAtracciones extends FuenteExterna {

	/**
	 * @param centro punto alrededor del cual se busca (normalmente el centro de la ciudad)
	 * @param limite cantidad máxima de atracciones
	 * @return atracciones encontradas; lista vacía si no hay ninguna
	 * @throws ProveedorExternoException si la API falla, no responde o se agotó la cuota
	 */
	List<Atraccion> buscarAtracciones(Coordenadas centro, int limite);
}
