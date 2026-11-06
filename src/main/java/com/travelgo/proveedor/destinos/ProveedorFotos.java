package com.travelgo.proveedor.destinos;

import java.util.Optional;

/**
 * Busca una foto real de un lugar, con su autor y su licencia.
 */
public interface ProveedorFotos extends FuenteExterna {

	/**
	 * @param lugar nombre del lugar, por ejemplo "Cartagena de Indias"
	 * @return la foto, o vacío si la fuente no tiene una foto para ese nombre
	 * @throws ProveedorExternoException si la API falla o no responde
	 */
	Optional<FotoDestino> buscarFoto(String lugar);
}
