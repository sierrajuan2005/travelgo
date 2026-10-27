package com.travelgo.maqueta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Reserva de ejemplo con los atributos de Reserva del diagrama de clases, más el paquete elegido.
 * Las reservas se guardan en una lista en memoria mientras la consola está abierta.
 * Se elimina cuando la consola use Reserva y ReservaService.
 */
public class ReservaMaqueta {

	public static final String PENDIENTE = "PENDIENTE";
	public static final String CONFIRMADA = "CONFIRMADA";
	public static final String CANCELADA = "CANCELADA";

	private static final String CLIENTE = "Cliente de prueba";
	private static final String CORREO = "cliente@travelgo.com";

	private static final List<ReservaMaqueta> RESERVAS = new ArrayList<>(List.of(
		new ReservaMaqueta("R-001", PaqueteMaqueta.buscar("2"),
			List.of(VueloMaqueta.deEjemplo("Cusco").get(1)), LocalDate.of(2026, 10, 20), PENDIENTE),
		new ReservaMaqueta("R-002", PaqueteMaqueta.buscar("1"),
			List.of(), LocalDate.of(2026, 10, 18), CANCELADA),
		new ReservaMaqueta("R-003", PaqueteMaqueta.buscar("3"),
			List.of(HotelMaqueta.deEjemplo("Cancún", 7).get(0)), LocalDate.of(2026, 10, 15), CONFIRMADA)
	));

	private final String id;
	private final PaqueteMaqueta paquete;
	private final List<ServicioReservableMaqueta> servicios;
	private final LocalDate fechaReserva;
	private final String cliente;
	private final String correo;
	private String estado;

	public ReservaMaqueta(String id, PaqueteMaqueta paquete, List<ServicioReservableMaqueta> servicios,
			LocalDate fechaReserva, String estado) {
		this.id = id;
		this.paquete = paquete;
		this.servicios = new ArrayList<>(servicios);
		this.fechaReserva = fechaReserva;
		this.cliente = CLIENTE;
		this.correo = CORREO;
		this.estado = estado;
	}

	/**
	 * Crea una reserva PENDIENTE con la fecha de hoy y la guarda en la lista.
	 *
	 * @param paquete   paquete elegido
	 * @param servicios vuelo, hotel y transporte elegidos (puede estar vacía)
	 */
	public static ReservaMaqueta guardar(PaqueteMaqueta paquete, List<ServicioReservableMaqueta> servicios) {
		String id = String.format("R-%03d", RESERVAS.size() + 1);
		ReservaMaqueta reserva = new ReservaMaqueta(id, paquete, servicios, LocalDate.now(), PENDIENTE);
		RESERVAS.add(reserva);
		return reserva;
	}

	/**
	 * Devuelve todas las reservas guardadas, en el orden en que se crearon.
	 */
	public static List<ReservaMaqueta> todas() {
		return List.copyOf(RESERVAS);
	}

	/**
	 * Busca una reserva por su id, sin distinguir mayúsculas. Devuelve null si no existe.
	 */
	public static ReservaMaqueta buscar(String id) {
		return RESERVAS.stream()
			.filter(r -> r.getId().equalsIgnoreCase(id))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Precio del paquete más calcularPrecio() de cada servicio (polimorfismo).
	 */
	public double calcularTotal() {
		double total = paquete.getPrecio();
		for (ServicioReservableMaqueta servicio : servicios) {
			total += servicio.calcularPrecio();
		}
		return total;
	}

	public String getId() {
		return id;
	}

	public PaqueteMaqueta getPaquete() {
		return paquete;
	}

	public List<ServicioReservableMaqueta> getServicios() {
		return List.copyOf(servicios);
	}

	public LocalDate getFechaReserva() {
		return fechaReserva;
	}

	public String getCliente() {
		return cliente;
	}

	public String getCorreo() {
		return correo;
	}

	public String getEstado() {
		return estado;
	}

}
