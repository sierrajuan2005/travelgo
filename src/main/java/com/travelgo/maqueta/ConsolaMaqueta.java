package com.travelgo.maqueta;

import com.travelgo.model.RolUsuario;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Versión en consola de las pantallas de catálogo, detalle, reserva, pago, factura y panel de administración,
 * con los datos de ejemplo.
 * No usa Spring ni base de datos.
 */
public class ConsolaMaqueta {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
			if (puedePagar()) {
				System.out.println("5. Pagar una reserva");
			}
			if (esAdministrador()) {
				System.out.println("6. Panel de administración");
			}
			System.out.println("0. Salir");
			opcion = leerOpcionMenu();

			switch (opcion) {
				case 1 -> mostrarCatalogo();
				case 2 -> mostrarDetalle();
				case 3 -> armarReserva();
				case 4 -> buscarPaquetes();
				case 5 -> pagarReserva();
				case 6 -> panelAdministracion();
				default -> System.out.println("Hasta pronto.");
			}
		} while (opcion != 0);
	}

	/**
	 * Lee una opción del menú principal. Repite la pregunta si la opción no existe para el rol.
	 */
	private int leerOpcionMenu() {
		while (true) {
			int opcion = leerNumero("Opción: ", 0, 6);
			if (opcion <= 4 || (opcion == 5 && puedePagar()) || (opcion == 6 && esAdministrador())) {
				return opcion;
			}
			System.out.println("Esa opción no está disponible para el rol " + rol + ".");
		}
	}

	/**
	 * Cliente y Agente pagan reservas; el Administrador gestiona el catálogo.
	 */
	private boolean puedePagar() {
		return rol == RolUsuario.CLIENTE || rol == RolUsuario.AGENTE;
	}

	/**
	 * Solo el Administrador ve el panel de administración.
	 */
	private boolean esAdministrador() {
		return rol == RolUsuario.ADMINISTRADOR;
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
	 * Muestra las reservas, pide una y la paga con el método elegido. Si se puede pagar, muestra la factura.
	 */
	private void pagarReserva() {
		System.out.println();
		System.out.println("--- Reservas ---");
		for (ReservaMaqueta reserva : ReservaMaqueta.todas()) {
			System.out.printf("[%s] %s - %s - %s%n", reserva.getId(), reserva.getPaquete().getNombre(),
				formatear(reserva.calcularTotal()), reserva.getEstado());
		}

		ReservaMaqueta reserva = elegirReserva();
		try {
			PagoMaqueta.validar(reserva);
		} catch (IllegalStateException e) {
			System.out.println(e.getMessage());
			return;
		}

		System.out.println();
		System.out.println("Total a pagar: " + formatear(reserva.calcularTotal()));
		System.out.println("1. " + PagoMaqueta.TARJETA);
		System.out.println("2. " + PagoMaqueta.TRANSFERENCIA);
		String metodo = leerNumero("Elige el método de pago: ", 1, 2) == 1
			? PagoMaqueta.TARJETA : PagoMaqueta.TRANSFERENCIA;

		PagoMaqueta pago = PagoMaqueta.registrar(reserva, metodo);
		System.out.println("Pago registrado.");
		imprimirFactura(pago);
	}

	/**
	 * Pide el id de una reserva hasta que exista.
	 */
	private ReservaMaqueta elegirReserva() {
		while (true) {
			System.out.print("Id de la reserva: ");
			ReservaMaqueta reserva = ReservaMaqueta.buscar(scanner.nextLine().trim());
			if (reserva != null) {
				return reserva;
			}
			System.out.println("No existe una reserva con ese id.");
		}
	}

	/**
	 * Muestra la factura del pago: datos de la reserva, del cliente, cada servicio y los totales.
	 */
	private void imprimirFactura(PagoMaqueta pago) {
		FacturaMaqueta factura = pago.getFactura();
		ReservaMaqueta reserva = factura.getReserva();
		System.out.println();
		System.out.println("--- Factura " + factura.getNumero() + " ---");
		System.out.println("Fecha de emisión: " + factura.getFechaEmision().format(FECHA_CORTA));
		System.out.println("Reserva:          " + reserva.getId() + " (" + reserva.getEstado() + ")");
		System.out.println("Cliente:          " + reserva.getCliente() + " - " + reserva.getCorreo());
		System.out.println("Servicios:");
		imprimirLineaFactura("  Paquete " + reserva.getPaquete().getNombre(), reserva.getPaquete().getPrecio());
		for (ServicioReservableMaqueta servicio : reserva.getServicios()) {
			imprimirLineaFactura("  " + servicio.describir(), servicio.calcularPrecio());
		}
		imprimirLineaFactura("Subtotal:", factura.getSubtotal());
		imprimirLineaFactura("Total:", factura.getTotal());
		System.out.println("Pago: " + pago.getMetodo() + " - " + pago.getFechaPago().format(FECHA_CORTA) + " - "
			+ pago.getEstado());
	}

	private void imprimirLineaFactura(String texto, double valor) {
		System.out.printf(Locale.US, "%-40s USD %,10.2f%n", texto, valor);
	}

	/**
	 * Menú del panel de administración. Se repite hasta que el Administrador vuelve al menú principal.
	 */
	private void panelAdministracion() {
		int opcion;
		do {
			System.out.println();
			System.out.println("=== Panel de administración ===");
			System.out.println("1. Destinos");
			System.out.println("2. Paquetes");
			System.out.println("0. Volver");
			opcion = leerNumero("Opción: ", 0, 2);
			switch (opcion) {
				case 1 -> gestionar("Destinos", this::listarDestinos, this::agregarDestino, this::eliminarDestino);
				case 2 -> gestionar("Paquetes", this::listarPaquetes, this::agregarPaquete, this::eliminarPaquete);
				default -> { }
			}
		} while (opcion != 0);
	}

	/**
	 * Submenú Listar / Agregar / Eliminar, igual para cada parte del catálogo.
	 * Si una regla no se cumple (dato inválido o registro en uso), muestra el motivo y sigue en el submenú.
	 */
	private void gestionar(String titulo, Runnable listar, Runnable agregar, Runnable eliminar) {
		int opcion;
		do {
			System.out.println();
			System.out.println("--- " + titulo + " ---");
			System.out.println("1. Listar");
			System.out.println("2. Agregar");
			System.out.println("3. Eliminar");
			System.out.println("0. Volver");
			opcion = leerNumero("Opción: ", 0, 3);
			try {
				switch (opcion) {
					case 1 -> listar.run();
					case 2 -> agregar.run();
					case 3 -> eliminar.run();
					default -> { }
				}
			} catch (IllegalArgumentException | IllegalStateException e) {
				System.out.println(e.getMessage());
			}
		} while (opcion != 0);
	}

	private void listarDestinos() {
		for (DestinoMaqueta destino : DestinoMaqueta.todos()) {
			System.out.printf("[%s] %s - %s, %s%n", destino.getId(), destino.getNombre(), destino.getCiudad(),
				destino.getPais());
		}
	}

	private void agregarDestino() {
		String nombre = leerTexto("Nombre: ");
		String descripcion = leerTexto("Descripción: ");
		String pais = leerTexto("País: ");
		String ciudad = leerTexto("Ciudad: ");
		DestinoMaqueta destino = DestinoMaqueta.agregar(nombre, descripcion, pais, ciudad);
		System.out.println("Destino agregado con id " + destino.getId() + ".");
	}

	private void eliminarDestino() {
		listarDestinos();
		String id = leerTexto("Id del destino a eliminar: ");
		if (DestinoMaqueta.buscar(id) == null) {
			System.out.println("No existe un destino con ese id.");
		} else if (confirmar("¿Eliminar el destino " + id + "? (s/n): ")) {
			DestinoMaqueta.eliminar(id);
			System.out.println("Destino eliminado.");
		}
	}

	private void listarPaquetes() {
		for (PaqueteMaqueta paquete : PaqueteMaqueta.todos()) {
			System.out.printf("[%s] %s - %s - %s - %d días - %s%n", paquete.getId(), paquete.getNombre(),
				paquete.getDestino().getCiudad(), formatear(paquete.getPrecio()), paquete.getDuracionDias(),
				paquete.isActivo() ? "activo" : "inactivo");
		}
	}

	private void agregarPaquete() {
		String nombre = leerTexto("Nombre: ");
		String descripcion = leerTexto("Descripción: ");
		double precio = leerPrecio("Precio en USD: ");
		int duracionDias = leerNumero("Duración en días: ", 1, 60);
		listarDestinos();
		DestinoMaqueta destino = elegirDestino();
		boolean activo = confirmar("¿Activo? (s/n): ");
		PaqueteMaqueta paquete = PaqueteMaqueta.agregar(nombre, descripcion, precio, duracionDias, activo, destino);
		System.out.println("Paquete agregado con id " + paquete.getId() + ".");
	}

	private void eliminarPaquete() {
		listarPaquetes();
		String id = leerTexto("Id del paquete a eliminar: ");
		boolean existe = PaqueteMaqueta.todos().stream().anyMatch(p -> p.getId().equals(id));
		if (!existe) {
			System.out.println("No existe un paquete con ese id.");
		} else if (confirmar("¿Eliminar el paquete " + id + "? (s/n): ")) {
			PaqueteMaqueta.eliminar(id);
			System.out.println("Paquete eliminado.");
		}
	}

	/**
	 * Pide el id de un destino hasta que exista.
	 */
	private DestinoMaqueta elegirDestino() {
		while (true) {
			DestinoMaqueta destino = DestinoMaqueta.buscar(leerTexto("Id del destino: "));
			if (destino != null) {
				return destino;
			}
			System.out.println("No existe un destino con ese id.");
		}
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

	/**
	 * Lee un texto que no esté vacío. Repite la pregunta si el usuario solo presiona Enter.
	 */
	private String leerTexto(String mensaje) {
		while (true) {
			System.out.print(mensaje);
			String linea = scanner.nextLine().trim();
			if (!linea.isEmpty()) {
				return linea;
			}
			System.out.println("Este dato es obligatorio.");
		}
	}

	/**
	 * Lee un precio mayor que cero.
	 */
	private double leerPrecio(String mensaje) {
		while (true) {
			System.out.print(mensaje);
			try {
				double precio = Double.parseDouble(scanner.nextLine().trim());
				if (precio > 0) {
					return precio;
				}
			} catch (NumberFormatException e) {
				// Se repite la pregunta
			}
			System.out.println("Escribe un número mayor que 0.");
		}
	}

	/**
	 * Hace una pregunta de sí o no. Repite la pregunta hasta recibir s o n.
	 */
	private boolean confirmar(String mensaje) {
		while (true) {
			System.out.print(mensaje);
			String respuesta = scanner.nextLine().trim().toLowerCase();
			if (respuesta.equals("s") || respuesta.equals("n")) {
				return respuesta.equals("s");
			}
			System.out.println("Escribe s o n.");
		}
	}

	private String formatear(double valor) {
		return String.format(Locale.US, "USD %,.2f", valor);
	}

}
