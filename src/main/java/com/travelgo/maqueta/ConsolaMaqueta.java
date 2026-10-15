package com.travelgo.maqueta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Versión en consola de las pantallas de catálogo, detalle y reserva, con los datos de ejemplo.
 * No usa Spring ni base de datos.
 */
public class ConsolaMaqueta {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		new ConsolaMaqueta().iniciar();
	}

	/**
	 * Muestra el menú principal hasta que el usuario elige salir.
	 */
	public void iniciar() {
		int opcion;
		do {
			System.out.println();
			System.out.println("=== TravelGo ===");
			System.out.println("1. Ver catálogo");
			System.out.println("2. Ver detalle de un paquete");
			System.out.println("3. Armar una reserva");
			System.out.println("0. Salir");
			opcion = leerNumero("Opción: ", 0, 3);

			switch (opcion) {
				case 1 -> mostrarCatalogo();
				case 2 -> mostrarDetalle();
				case 3 -> armarReserva();
				default -> System.out.println("Hasta pronto.");
			}
		} while (opcion != 0);
	}

	/**
	 * Lista los paquetes activos.
	 */
	private void mostrarCatalogo() {
		System.out.println();
		System.out.println("--- Paquetes turísticos ---");
		for (PaqueteMaqueta paquete : PaqueteMaqueta.activos()) {
			System.out.printf("[%s] %s - %s, %s - %d días - %s%n", paquete.getId(), paquete.getNombre(),
				paquete.getDestino().getCiudad(), paquete.getDestino().getPais(), paquete.getDuracionDias(),
				formatear(paquete.getPrecio()));
		}
	}

	/**
	 * Muestra los datos de un paquete y de su destino.
	 */
	private void mostrarDetalle() {
		PaqueteMaqueta paquete = elegirPaquete();
		DestinoMaqueta destino = paquete.getDestino();
		System.out.println();
		System.out.println("--- " + paquete.getNombre() + " ---");
		System.out.println("Destino: " + destino.getCiudad() + ", " + destino.getPais());
		System.out.println(paquete.getDescripcion());
		System.out.println("Sobre el destino: " + destino.getNombre() + ". " + destino.getDescripcion());
		System.out.println("Duración: " + paquete.getDuracionDias() + " días");
		System.out.println("Precio: " + formatear(paquete.getPrecio()));
	}

	/**
	 * Pide un paquete y un vuelo, hotel y transporte, y muestra el total.
	 * El total usa calcularPrecio() de cada servicio (polimorfismo).
	 */
	private void armarReserva() {
		PaqueteMaqueta paquete = elegirPaquete();
		String ciudad = paquete.getDestino().getCiudad();
		List<ServicioReservableMaqueta> servicios = new ArrayList<>();

		System.out.println();
		System.out.println("--- Vuelos ---");
		List<VueloMaqueta> vuelos = VueloMaqueta.deEjemplo(ciudad);
		for (int i = 0; i < vuelos.size(); i++) {
			VueloMaqueta vuelo = vuelos.get(i);
			System.out.printf("%d. %s, %s -> %s, sale %s, llega %s - %s%n", i + 1, vuelo.getAerolinea(),
				vuelo.getOrigen(), vuelo.getDestino(), vuelo.getFechaSalida().format(FECHA),
				vuelo.getFechaLlegada().format(FECHA), formatear(vuelo.calcularPrecio()));
		}
		agregarElegido(servicios, vuelos, "vuelo");

		System.out.println();
		System.out.println("--- Hoteles ---");
		List<HotelMaqueta> hoteles = HotelMaqueta.deEjemplo(ciudad, paquete.getDuracionDias());
		for (int i = 0; i < hoteles.size(); i++) {
			HotelMaqueta hotel = hoteles.get(i);
			System.out.printf("%d. %s, %s, %s - %s por noche x %d noches = %s%n", i + 1, hotel.getNombre(),
				hotel.getTipoHabitacion(), hotel.getDireccion(), formatear(hotel.getPrecio()), hotel.getNoches(),
				formatear(hotel.calcularPrecio()));
		}
		agregarElegido(servicios, hoteles, "hotel");

		System.out.println();
		System.out.println("--- Transportes ---");
		List<TransporteMaqueta> transportes = TransporteMaqueta.deEjemplo(ciudad);
		for (int i = 0; i < transportes.size(); i++) {
			TransporteMaqueta transporte = transportes.get(i);
			System.out.printf("%d. %s, %s -> %s, %s - %s%n", i + 1, transporte.getTipo(), transporte.getOrigen(),
				transporte.getDestino(), transporte.getFecha().format(FECHA), formatear(transporte.calcularPrecio()));
		}
		agregarElegido(servicios, transportes, "transporte");

		double extras = 0;
		for (ServicioReservableMaqueta servicio : servicios) {
			extras += servicio.calcularPrecio();
		}

		System.out.println();
		System.out.println("--- Resumen de la reserva ---");
		System.out.println("Paquete:         " + paquete.getNombre() + " - " + formatear(paquete.getPrecio()));
		System.out.println("Servicios extra: " + servicios.size() + " - " + formatear(extras));
		System.out.println("Total:           " + formatear(paquete.getPrecio() + extras));
		System.out.println("(Maqueta: la reserva no se guarda)");
	}

	/**
	 * Pide el número de una opción de la lista (0 = ninguna) y la agrega a los servicios elegidos.
	 */
	private void agregarElegido(List<ServicioReservableMaqueta> servicios,
			List<? extends ServicioReservableMaqueta> opciones, String nombre) {
		int elegido = leerNumero("Elige un " + nombre + " (0 = sin " + nombre + "): ", 0, opciones.size());
		if (elegido > 0) {
			servicios.add(opciones.get(elegido - 1));
		}
	}

	/**
	 * Muestra el catálogo y pide el id de un paquete hasta que exista.
	 */
	private PaqueteMaqueta elegirPaquete() {
		mostrarCatalogo();
		while (true) {
			System.out.print("Id del paquete: ");
			PaqueteMaqueta paquete = PaqueteMaqueta.buscar(scanner.nextLine().trim());
			if (paquete != null) {
				return paquete;
			}
			System.out.println("No existe un paquete con ese id.");
		}
	}

	/**
	 * Lee un número entre min y max. Repite la pregunta si el valor no es válido.
	 */
	private int leerNumero(String mensaje, int min, int max) {
		while (true) {
			System.out.print(mensaje);
			try {
				int numero = Integer.parseInt(scanner.nextLine().trim());
				if (numero >= min && numero <= max) {
					return numero;
				}
			} catch (NumberFormatException e) {
				// Se repite la pregunta
			}
			System.out.println("Escribe un número entre " + min + " y " + max + ".");
		}
	}

	private String formatear(double valor) {
		return String.format(Locale.US, "USD %,.2f", valor);
	}

}
