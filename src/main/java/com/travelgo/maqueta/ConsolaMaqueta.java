package com.travelgo.maqueta;

import com.travelgo.dto.PagoRequest;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

import static com.travelgo.maqueta.FormatoMaqueta.describir;
import static com.travelgo.maqueta.FormatoMaqueta.idCorto;
import static com.travelgo.maqueta.FormatoMaqueta.nombreMetodo;
import static com.travelgo.maqueta.FormatoMaqueta.nombrePaquete;

/**
 * Versión en consola de las pantallas de login, catálogo, detalle, reserva, pago, factura y panel de
 * administración. El catálogo y el panel usan los datos de ejemplo; el login, las reservas, los pagos y las
 * facturas usan las clases y los services reales (ver BackendConsola).
 * No usa Spring ni base de datos.
 */
public class ConsolaMaqueta {

	private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final Scanner scanner = new Scanner(System.in);

	/** Usuario que inició sesión. Su rol decide qué opciones aparecen en el menú (docs/matriz_permisos.md). */
	private Usuario usuario;

	public static void main(String[] args) {
		new ConsolaMaqueta().iniciar();
	}

	/**
	 * Pide el login y muestra el menú principal del usuario. Al cerrar sesión vuelve al login, para poder entrar
	 * con otro rol; en el login, Enter sin correo sale de la consola.
	 */
	public void iniciar() {
		while ((usuario = iniciarSesion()) != null) {
			mostrarMenuPrincipal();
		}
		System.out.println("Hasta pronto.");
	}

	/**
	 * Pide correo y contraseña hasta que sean correctos. Usa UsuarioService y BCrypt, igual que el login web.
	 *
	 * @return el usuario autenticado, o null si se deja el correo vacío para salir
	 */
	private Usuario iniciarSesion() {
		System.out.println();
		System.out.println("=== TravelGo: iniciar sesión ===");
		System.out.println("Usuarios de prueba (contraseña " + BackendConsola.PASSWORD_EJEMPLO + "): "
			+ "cliente@travelgo.com, ana@travelgo.com, agente@travelgo.com, admin@travelgo.com");
		while (true) {
			System.out.print("Correo (Enter = salir): ");
			String correo = scanner.nextLine().trim();
			if (correo.isEmpty()) {
				return null;
			}
			String password = leerTexto("Contraseña: ");
			Optional<Usuario> autenticado = BackendConsola.iniciarSesion(correo, password);
			if (autenticado.isPresent()) {
				System.out.println("Bienvenido, " + autenticado.get().getNombre() + ".");
				return autenticado.get();
			}
			System.out.println("Correo o contraseña incorrectos.");
		}
	}

	/**
	 * Muestra el menú principal hasta que el usuario cierra sesión.
	 */
	private void mostrarMenuPrincipal() {
		int opcion;
		do {
			System.out.println();
			System.out.println("=== TravelGo (" + usuario.getNombre() + " - " + usuario.getRol() + ") ===");
			System.out.println("1. Ver catálogo");
			System.out.println("2. Ver detalle de un paquete");
			if (puedeReservar()) {
				System.out.println("3. Armar una reserva");
			}
			System.out.println("4. Buscar paquetes por destino o precio");
			if (puedeReservar()) {
				System.out.println("5. Pagar una reserva");
			}
			if (esAdministrador()) {
				System.out.println("6. Panel de administración");
			}
			System.out.println("0. Cerrar sesión");
			opcion = leerOpcionMenu();

			switch (opcion) {
				case 1 -> mostrarCatalogo();
				case 2 -> mostrarDetalle();
				case 3 -> armarReserva();
				case 4 -> buscarPaquetes();
				case 5 -> pagarReserva();
				case 6 -> panelAdministracion();
				default -> System.out.println("Sesión cerrada.");
			}
		} while (opcion != 0);
	}

	/**
	 * Lee una opción del menú principal. Repite la pregunta si la opción no existe para el rol.
	 */
	private int leerOpcionMenu() {
		while (true) {
			int opcion = leerNumero("Opción: ", 0, 6);
			boolean permitida = switch (opcion) {
				case 3, 5 -> puedeReservar();
				case 6 -> esAdministrador();
				default -> true;
			};
			if (permitida) {
				return opcion;
			}
			System.out.println("Esa opción no está disponible para el rol " + usuario.getRol() + ".");
		}
	}

	/**
	 * Cliente y Agente reservan y pagan; para el Administrador "no aplica" (matriz de permisos).
	 */
	private boolean puedeReservar() {
		return BackendConsola.puedeReservar(usuario);
	}

	/**
	 * Solo el Administrador ve el panel de administración.
	 */
	private boolean esAdministrador() {
		return usuario.getRol() == RolUsuario.ADMINISTRADOR;
	}

	/**
	 * El Cliente solo ve y paga sus propias reservas; el Agente gestiona las de todos los clientes.
	 */
	private List<Reserva> reservasVisibles() {
		return BackendConsola.reservasVisibles(usuario);
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
	 * Pide un paquete y un vuelo, hotel y transporte disponibles, guarda una Reserva real como PENDIENTE y muestra
	 * el total. El total lo calcula Reserva con calcularCosto() de cada servicio (polimorfismo).
	 */
	private void armarReserva() {
		PaqueteMaqueta paquete = elegirPaquete();
		ServiciosPaqueteMaqueta serviciosPaquete = ServiciosPaqueteMaqueta.de(paquete);
		List<ServicioReservableMaqueta> servicios = new ArrayList<>();

		System.out.println();
		System.out.println("--- Vuelos ---");
		List<VueloMaqueta> vuelos = serviciosPaquete.vuelosDisponibles();
		for (int i = 0; i < vuelos.size(); i++) {
			VueloMaqueta vuelo = vuelos.get(i);
			System.out.printf("%d. %s, %s -> %s, sale %s, llega %s - %s%n", i + 1, vuelo.getAerolinea(),
				vuelo.getOrigen(), vuelo.getDestino(), vuelo.getFechaSalida().format(FECHA),
				vuelo.getFechaLlegada().format(FECHA), formatear(vuelo.calcularPrecio()));
		}
		agregarElegido(servicios, vuelos, "vuelo");

		System.out.println();
		System.out.println("--- Hoteles ---");
		List<HotelMaqueta> hoteles = serviciosPaquete.hotelesDisponibles();
		for (int i = 0; i < hoteles.size(); i++) {
			HotelMaqueta hotel = hoteles.get(i);
			System.out.printf("%d. %s, %s, %s - %s por noche x %d noches = %s%n", i + 1, hotel.getNombre(),
				hotel.getTipoHabitacion(), hotel.getDireccion(), formatear(hotel.getPrecio()), hotel.getNoches(),
				formatear(hotel.calcularPrecio()));
		}
		agregarElegido(servicios, hoteles, "hotel");

		System.out.println();
		System.out.println("--- Transportes ---");
		List<TransporteMaqueta> transportes = serviciosPaquete.transportesDisponibles();
		for (int i = 0; i < transportes.size(); i++) {
			TransporteMaqueta transporte = transportes.get(i);
			System.out.printf("%d. %s, %s -> %s, %s - %s%n", i + 1, transporte.getTipo(), transporte.getOrigen(),
				transporte.getDestino(), transporte.getFecha().format(FECHA), formatear(transporte.calcularPrecio()));
		}
		agregarElegido(servicios, transportes, "transporte");

		Reserva reserva;
		try {
			reserva = BackendConsola.guardarReserva(usuario, paquete, servicios);
		} catch (IllegalArgumentException | IllegalStateException e) {
			System.out.println("No se pudo guardar la reserva: " + e.getMessage());
			return;
		}

		System.out.println();
		System.out.println("--- Resumen de la reserva ---");
		System.out.println("Paquete:         " + paquete.getNombre() + " - " + formatear(paquete.getPrecio()));
		System.out.println("Servicios extra: " + servicios.size());
		System.out.println("Total a pagar:   " + formatear(reserva.getTotal()));
		// Quitar esta línea cuando Reserva.calcularTotal() sume también el precio del paquete
		System.out.println("(Por ahora el total solo suma los servicios, no el precio del paquete.)");
		System.out.println("Reserva guardada con id " + idCorto(reserva.getId()) + " (" + reserva.getEstado() + ").");
	}

	/**
	 * Muestra las reservas, pide una y la paga con PagoService, con las mismas reglas que /api/pagos:
	 * una reserva se paga una sola vez, no se paga una reserva cancelada, y el pago confirma la reserva y genera
	 * la factura. Si la reserva ya está pagada, muestra su factura.
	 * El Cliente solo ve sus reservas; el Agente ve las de todos, con el nombre del cliente.
	 */
	private void pagarReserva() {
		List<Reserva> reservas = reservasVisibles();
		boolean esAgente = usuario.getRol() == RolUsuario.AGENTE;
		System.out.println();
		System.out.println("--- " + (esAgente ? "Reservas de todos los clientes" : "Mis reservas") + " ---");
		if (reservas.isEmpty()) {
			System.out.println("No hay reservas. Arma una con la opción 3.");
			return;
		}
		for (int i = 0; i < reservas.size(); i++) {
			Reserva reserva = reservas.get(i);
			String cliente = esAgente ? " - " + reserva.getUsuario().getNombre() : "";
			System.out.printf("%d. [%s] %s - %s - %s%s%n", i + 1, idCorto(reserva.getId()), nombrePaquete(reserva),
				formatear(reserva.getTotal()), reserva.getEstado(), cliente);
		}
		Reserva reserva = reservas.get(leerNumero("Elige una reserva: ", 1, reservas.size()) - 1);

		Factura facturaAnterior = buscarFactura(reserva);
		if (facturaAnterior != null) {
			System.out.println("La reserva ya está pagada. Esta es su factura:");
			imprimirFactura(facturaAnterior);
			return;
		}

		System.out.println();
		System.out.println("Total a pagar: " + formatear(reserva.getTotal()));
		MetodoPago[] metodos = MetodoPago.values();
		for (int i = 0; i < metodos.length; i++) {
			System.out.println((i + 1) + ". " + nombreMetodo(metodos[i]));
		}
		MetodoPago metodo = metodos[leerNumero("Elige el método de pago: ", 1, metodos.length) - 1];

		try {
			Pago pago = BackendConsola.PAGOS.pagar(new PagoRequest(reserva.getId(), metodo));
			System.out.println("Pago registrado.");
			imprimirFactura(pago.getFactura());
		} catch (IllegalArgumentException | IllegalStateException e) {
			System.out.println("No se pudo pagar: " + e.getMessage());
		}
	}

	/**
	 * Busca la factura de una reserva con FacturaService. Devuelve null si la reserva no está pagada.
	 */
	private Factura buscarFactura(Reserva reserva) {
		return BackendConsola.buscarFactura(reserva).orElse(null);
	}

	/**
	 * Busca el pago de una reserva con PagoService. Devuelve null si la reserva no tiene pago.
	 */
	private Pago buscarPago(Reserva reserva) {
		return BackendConsola.buscarPago(reserva).orElse(null);
	}

	/**
	 * Muestra la factura: datos de la reserva, del cliente, cada servicio, el total y el pago.
	 */
	private void imprimirFactura(Factura factura) {
		Reserva reserva = factura.getReserva();
		Pago pago = buscarPago(reserva);		System.out.println();
		System.out.println("--- Factura " + factura.getNumero() + " ---");
		System.out.println("Fecha de emisión: " + factura.getFechaEmision().format(FECHA_CORTA));
		System.out.println("Reserva:          " + idCorto(reserva.getId()) + " (" + reserva.getEstado() + ")");
		System.out.println("Cliente:          " + reserva.getUsuario().getNombre() + " - "
			+ reserva.getUsuario().getCorreo());
		System.out.println("Paquete:          " + nombrePaquete(reserva));
		System.out.println("Servicios:");
		for (ServicioReservable servicio : reserva.getServicios()) {
			imprimirLineaFactura("  " + describir(servicio), servicio.calcularCosto());
		}
		imprimirLineaFactura("Total:", factura.getTotal());
		System.out.println("Estado de la factura: " + factura.getEstado());
		if (pago != null) {
			System.out.println("Pago: " + nombreMetodo(pago.getMetodo()) + " - " + pago.getFechaPago().format(FECHA_CORTA));
		}	}

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
			System.out.println("3. Servicios");
			System.out.println("0. Volver");
			opcion = leerNumero("Opción: ", 0, 3);
			switch (opcion) {
				case 1 -> gestionar("Destinos", this::listarDestinos, this::agregarDestino, this::eliminarDestino);
				case 2 -> gestionar("Paquetes", this::listarPaquetes, this::agregarPaquete, this::eliminarPaquete);
				case 3 -> gestionarServicios();
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
	 * Muestra los servicios de un paquete y cambia la disponibilidad del que elija el Administrador.
	 * Los servicios no disponibles no aparecen al armar una reserva.
	 */
	private void gestionarServicios() {
		PaqueteMaqueta paquete = elegirPaquete();
		ServiciosPaqueteMaqueta servicios = ServiciosPaqueteMaqueta.de(paquete);
		while (true) {
			System.out.println();
			System.out.println("--- Servicios de " + paquete.getNombre() + " ---");
			for (ServicioReservableMaqueta servicio : servicios.todos()) {
				System.out.printf("[%s] %s - %s - %s%n", servicio.getId(), servicio.describir(),
					formatear(servicio.calcularPrecio()), servicio.isDisponible() ? "disponible" : "no disponible");
			}
			System.out.print("Id del servicio para cambiar su disponibilidad (Enter = volver): ");
			String id = scanner.nextLine().trim();
			if (id.isEmpty()) {
				return;
			}
			ServicioReservableMaqueta servicio = servicios.buscar(id);
			if (servicio == null) {
				System.out.println("No existe un servicio con ese id en este paquete.");
			} else {
				servicio.cambiarDisponibilidad();
				System.out.println("El servicio " + servicio.getId() + " ahora está "
					+ (servicio.isDisponible() ? "disponible." : "no disponible."));
			}
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
		if (opciones.isEmpty()) {
			System.out.println("No hay servicios de tipo " + nombre + " disponibles.");
			return;
		}
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
