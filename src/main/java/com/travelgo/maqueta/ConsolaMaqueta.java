package com.travelgo.maqueta;

import com.travelgo.model.RolUsuario;

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

	/** Rol con el que se entra a la consola. Decide qué opciones aparecen en el menú. */
	private RolUsuario rol;

	public static void main(String[] args) {
		new ConsolaMaqueta().iniciar();
	}

	/**
	 * Pide el rol y muestra el menú principal hasta que el usuario elige salir.
	 */
	public void iniciar() {
		rol = elegirRol();
		int opcion;
		do {
			System.out.println();
			System.out.println("=== TravelGo (" + rol + ") ===");
			System.out.println("1. Ver catálogo");
			System.out.println("2. Ver detalle de un paquete");
			System.out.println("3. Armar una reserva");
			System.out.println("4. Buscar paquetes por destino o precio");
			System.out.println("0. Salir");
			opcion = leerNumero("Opción: ", 0, 4);

			switch (opcion) {
				case 1 -> mostrarCatalogo();
				case 2 -> mostrarDetalle();
				case 3 -> armarReserva();
				case 4 -> buscarPaquetes();
				default -> System.out.println("Hasta pronto.");
			}
		} while (opcion != 0);
	}

	/**
	 * Pregunta con qué rol se entra. Reemplaza al login por rol mientras no esté listo.
	 */
	private RolUsuario elegirRol() {
		System.out.println();
		System.out.println("=== TravelGo ===");
		System.out.println("¿Con qué rol entras?");
		System.out.println("1. Cliente");
		System.out.println("2. Agente");
		System.out.println("3. Administrador");
		return switch (leerNumero("Opción: ", 1, 3)) {
			case 1 -> RolUsuario.CLIENTE;
			case 2 -> RolUsuario.AGENTE;
			default -> RolUsuario.ADMINISTRADOR;
		};
	}

	/**
	 * Lista los paquetes activos.
	 */
	private void mostrarCatalogo() {
		System.out.println();
		System.out.println("--- Paquetes turísticos ---");
		imprimirPaquetes(PaqueteMaqueta.activos());
	}

	/**
	 * Pide destino y rango de precios y muestra los paquetes que cumplen (Historia 3).
	 * Dejar un dato vacío significa no filtrar por él.
	 */
	private void buscarPaquetes() {
		System.out.println();
		System.out.print("Destino (ciudad o país, Enter = todos): ");
		String destino = scanner.nextLine();
		Double precioMin = leerPrecioOpcional("Precio mínimo en USD (Enter = sin mínimo): ");
		Double precioMax = leerPrecioOpcional("Precio máximo en USD (Enter = sin máximo): ");

		if (precioMin != null && precioMax != null && precioMin > precioMax) {
			System.out.println("El precio mínimo no puede ser mayor que el máximo.");
			return;
		}

		List<PaqueteMaqueta> resultado = PaqueteMaqueta.filtrar(destino, precioMin, precioMax);
		System.out.println();
		System.out.println("--- Resultados (" + resultado.size() + ") ---");
		if (resultado.isEmpty()) {
			System.out.println("No hay paquetes con esos filtros.");
		} else {
			imprimirPaquetes(resultado);
		}
	}

	private void imprimirPaquetes(List<PaqueteMaqueta> paquetes) {
		for (PaqueteMaqueta paquete : paquetes) {
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
	 * Pide un paquete y un vuelo, hotel y transporte, muestra el total y guarda la reserva como PENDIENTE.
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

		ReservaMaqueta reserva = ReservaMaqueta.guardar(paquete, servicios);
		System.out.println("Reserva guardada con id " + reserva.getId() + " (" + reserva.getEstado() + ").");
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

	/**
	 * Lee un precio mayor o igual a cero. Devuelve null si el usuario solo presiona Enter.
	 */
	private Double leerPrecioOpcional(String mensaje) {
		while (true) {
			System.out.print(mensaje);
			String linea = scanner.nextLine().trim();
			if (linea.isEmpty()) {
				return null;
			}
			try {
				double precio = Double.parseDouble(linea);
				if (precio >= 0) {
					return precio;
				}
			} catch (NumberFormatException e) {
				// Se repite la pregunta
			}
			System.out.println("Escribe un número mayor o igual a 0, o presiona Enter.");
		}
	}

	private String formatear(double valor) {
		return String.format(Locale.US, "USD %,.2f", valor);
	}

}
