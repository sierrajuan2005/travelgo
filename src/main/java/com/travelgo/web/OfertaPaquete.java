package com.travelgo.web;

import com.travelgo.util.Textos;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;

import java.util.List;
import java.util.Optional;

/**
 * Servicios que se pueden agregar a la reserva de un paquete: los que llegan a la ciudad del paquete.
 * <ul>
 *   <li>Vuelo: su destino es una ciudad del paquete.</li>
 *   <li>Hotel: su dirección menciona una ciudad del paquete.</li>
 *   <li>Transporte: sale de una ciudad del paquete o llega a ella.</li>
 * </ul>
 * Hoy la base de datos no une paquetes con servicios; por eso se cruzan por ciudad. Cuando el constructor
 * de paquetes del Sprint 4 guarde el vuelo y el hotel de cada paquete, esta clase se reemplaza por esos datos.
 *
 * @param vuelos      vuelos hacia el destino
 * @param hoteles     hoteles del destino
 * @param transportes transportes desde o hacia el destino
 */
public record OfertaPaquete(List<Vuelo> vuelos, List<Hotel> hoteles, List<Transporte> transportes) {

	/**
	 * @param paquete   paquete elegido
	 * @param servicios servicios disponibles (normalmente {@code ServicioReservableService.listar(true)})
	 */
	public static OfertaPaquete de(PaqueteTuristico paquete, List<ServicioReservable> servicios) {
		List<String> ciudades = paquete.getDestinos().stream()
			.map(Destino::getCiudad)
			.map(Textos::normalizar)
			.filter(c -> !c.isEmpty())
			.toList();
		List<Vuelo> vuelos = servicios.stream()
			.filter(Vuelo.class::isInstance).map(Vuelo.class::cast)
			.filter(v -> ciudades.contains(Textos.normalizar(v.getDestino())))
			.toList();
		List<Hotel> hoteles = servicios.stream()
			.filter(Hotel.class::isInstance).map(Hotel.class::cast)
			.filter(h -> ciudades.stream().anyMatch(Textos.normalizar(h.getDireccion())::contains))
			.toList();
		List<Transporte> transportes = servicios.stream()
			.filter(Transporte.class::isInstance).map(Transporte.class::cast)
			.filter(t -> ciudades.contains(Textos.normalizar(t.getOrigen())) || ciudades.contains(Textos.normalizar(t.getDestino())))
			.toList();
		return new OfertaPaquete(vuelos, hoteles, transportes);
	}

	/** Busca un servicio de la oferta por id. */
	public Optional<ServicioReservable> buscar(String id) {
		return java.util.stream.Stream.of(vuelos, hoteles, transportes)
			.flatMap(List::stream)
			.map(ServicioReservable.class::cast)
			.filter(s -> s.getId().equals(id))
			.findFirst();
	}

}
