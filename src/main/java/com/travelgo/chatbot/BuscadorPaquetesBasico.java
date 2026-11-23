package com.travelgo.chatbot;

import com.travelgo.model.PaqueteTuristico;
import com.travelgo.service.PaqueteService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación con lo que existe hoy: {@code PaqueteService.buscar}. No sabe filtrar por gama
 * (depende de la gama de los hoteles del Integrante 2), así que {@code porGama} responde "no disponible".
 */
@Component
public class BuscadorPaquetesBasico implements BuscadorPaquetes {

	private final PaqueteService paqueteService;

	public BuscadorPaquetesBasico(PaqueteService paqueteService) {
		this.paqueteService = paqueteService;
	}

	@Override
	public List<PaqueteTuristico> porDestino(String destino) {
		return paqueteService.buscar(destino, null, null, null, null);
	}

	@Override
	public List<PaqueteTuristico> masBaratos(String destino) {
		List<PaqueteTuristico> ordenados = porDestino(destino).stream()
			.sorted(Comparator.comparing(PaqueteTuristico::getPrecio))
			.toList();
		int tercio = (int) Math.ceil(ordenados.size() / 3.0);
		return ordenados.subList(0, tercio);
	}

	@Override
	public Optional<List<PaqueteTuristico>> porGama(String destino, Gama gama) {
		return Optional.empty();
	}
}
