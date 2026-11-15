package com.travelgo.proveedor.destinos;

import java.util.Optional;

/**
 * Convierte el nombre de una ciudad en coordenadas. Los destinos de TravelGo solo guardan ciudad y país,
 * y las APIs de atracciones y de clima piden latitud y longitud.
 */
public interface Geocodificador extends FuenteExterna {

	/**
	 * @param ciudad nombre de la ciudad, por ejemplo "Cusco"
	 * @param pais   país de la ciudad, para no confundir Cartagena (Colombia) con Cartagena (España)
	 * @return coordenadas del centro de la ciudad, o vacío si no se encontró
	 * @throws ProveedorExternoException si la API falla o no responde
	 */
	Optional<Coordenadas> ubicar(String ciudad, String pais);
}
