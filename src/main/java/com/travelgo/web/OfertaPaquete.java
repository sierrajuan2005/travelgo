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
 * Si el paquete se armó con el constructor de paquetes (Sprint 4), la oferta son su vuelo y su hotel guardados
 * en la tabla paquete_servicio ({@link #incluidos}). Los paquetes creados a mano no tienen servicios propios,
 * así que se cruzan por ciudad ({@link #de}).
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

	/**
	 * Oferta de un paquete armado con el constructor: exactamente los servicios guardados con él.
	 *
	 * @param servicios servicios del paquete (de la tabla paquete_servicio)
	 */
	public static OfertaPaquete incluidos(List<ServicioReservable> servicios) {
		return new OfertaPaquete(
				servicios.stream().filter(Vuelo.class::isInstance).map(Vuelo.class::cast).toList(),
				servicios.stream().filter(Hotel.class::isInstance).map(Hotel.class::cast).toList(),
				servicios.stream().filter(Transporte.class::isInstance).map(Transporte.class::cast).toList());
	}

	/** Busca un servicio de la oferta por id. */
	public Optional<ServicioReservable> buscar(String id) {
		return java.util.stream.Stream.of(vuelos, hoteles, transportes)
				.flatMap(List::stream)
				.map(ServicioReservable.class::cast)
				.filter(s -> s.getId().equals(id))
				.findFirst();
	}

	/** Los servicios incluidos se reservan una sola vez; los manuales se eligen por tipo. */
	public List<String> seleccionar(boolean incluidos, String vueloId, String hotelId, String transporteId) {
		if (incluidos) {
			return java.util.stream.Stream.of(vuelos, hoteles, transportes)
					.flatMap(List::stream).map(ServicioReservable::getId).distinct().toList();
		}
		return java.util.stream.Stream.of(
						vuelos.stream().filter(v -> v.getId().equals(vueloId)).map(Vuelo::getId),
						hoteles.stream().filter(h -> h.getId().equals(hotelId)).map(Hotel::getId),
						transportes.stream().filter(t -> t.getId().equals(transporteId)).map(Transporte::getId))
				.flatMap(java.util.function.Function.identity()).distinct().toList();
	}

}
