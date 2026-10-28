package com.travelgo.maqueta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vuelos, hoteles y transportes de ejemplo de un paquete.
 * Se crean la primera vez que se piden y se guardan, para que los cambios de disponibilidad del panel de
 * administración se mantengan mientras la consola está abierta.
 * Se elimina cuando la consola use ServicioReservableService.
 */
public class ServiciosPaqueteMaqueta {

	/** Servicios guardados por id de paquete. */
	private static final Map<String, ServiciosPaqueteMaqueta> POR_PAQUETE = new HashMap<>();

	private final List<VueloMaqueta> vuelos;
	private final List<HotelMaqueta> hoteles;
	private final List<TransporteMaqueta> transportes;

	private ServiciosPaqueteMaqueta(PaqueteMaqueta paquete) {
		String ciudad = paquete.getDestino().getCiudad();
		this.vuelos = VueloMaqueta.deEjemplo(ciudad);
		this.hoteles = HotelMaqueta.deEjemplo(ciudad, paquete.getDuracionDias());
		this.transportes = TransporteMaqueta.deEjemplo(ciudad);
	}

	/**
	 * Devuelve los servicios del paquete. La primera vez los crea con los datos de ejemplo de su ciudad.
	 */
	public static ServiciosPaqueteMaqueta de(PaqueteMaqueta paquete) {
		return POR_PAQUETE.computeIfAbsent(paquete.getId(), id -> new ServiciosPaqueteMaqueta(paquete));
	}

	/**
	 * Todos los servicios del paquete, disponibles o no: primero vuelos, luego hoteles y transportes.
	 */
	public List<ServicioReservableMaqueta> todos() {
		List<ServicioReservableMaqueta> todos = new ArrayList<>(vuelos);
		todos.addAll(hoteles);
		todos.addAll(transportes);
		return todos;
	}

	/**
	 * Busca un servicio del paquete por su id, sin distinguir mayúsculas. Devuelve null si no existe.
	 */
	public ServicioReservableMaqueta buscar(String id) {
		return todos().stream()
			.filter(s -> s.getId().equalsIgnoreCase(id))
			.findFirst()
			.orElse(null);
	}

	public List<VueloMaqueta> vuelosDisponibles() {
		return soloDisponibles(vuelos);
	}

	public List<HotelMaqueta> hotelesDisponibles() {
		return soloDisponibles(hoteles);
	}

	public List<TransporteMaqueta> transportesDisponibles() {
		return soloDisponibles(transportes);
	}

	private static <T extends ServicioReservableMaqueta> List<T> soloDisponibles(List<T> servicios) {
		return servicios.stream()
			.filter(ServicioReservableMaqueta::isDisponible)
			.toList();
	}

}
