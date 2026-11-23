package com.travelgo.chatbot;

import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.proveedor.hotel.ClasificadorGama;
import com.travelgo.proveedor.hotel.GamaHotel;
import com.travelgo.service.PaqueteService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Implementación con lo que existe hoy: {@code PaqueteService.buscar} para destino y precio, y la gama de los
 * hoteles de cada paquete ({@code ClasificadorGama}, del Integrante 2). Todavía no filtra por fechas ni por número
 * de personas: eso depende de la búsqueda del Integrante 1.
 */
@Component
public class BuscadorPaquetesBasico implements BuscadorPaquetes {

	private final PaqueteService paqueteService;
	private final PaqueteServicioDAO paqueteServicioDAO;
	private final ClasificadorGama clasificador;

	public BuscadorPaquetesBasico(PaqueteService paqueteService, PaqueteServicioDAO paqueteServicioDAO,
			ClasificadorGama clasificador) {
		this.paqueteService = paqueteService;
		this.paqueteServicioDAO = paqueteServicioDAO;
		this.clasificador = clasificador;
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

	/** Paquetes del destino con al menos un hotel de esa gama, del más barato al más caro. */
	@Override
	public Optional<List<PaqueteTuristico>> porGama(String destino, Gama gama) {
		if (gama == null) {
			return Optional.of(List.of());
		}
		GamaHotel buscada = GamaHotel.valueOf(gama.name());
		List<PaqueteTuristico> encontrados = porDestino(destino).stream()
			.filter(paquete -> tieneHotelDeGama(paquete, buscada))
			.sorted(Comparator.comparing(PaqueteTuristico::getPrecio))
			.toList();
		return Optional.of(encontrados);
	}

	private boolean tieneHotelDeGama(PaqueteTuristico paquete, GamaHotel buscada) {
		for (ServicioReservable servicio : paqueteServicioDAO.listarServicios(paquete.getId())) {
			if (servicio instanceof Hotel hotel && clasificador.gamaDe(hotel) == buscada) {
				return true;
			}
		}
		return false;
	}
}
