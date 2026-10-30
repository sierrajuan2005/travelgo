package com.travelgo.maqueta;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.FacturaDAOMemoria;
import com.travelgo.dao.PagoDAOMemoria;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dao.ReservaDAOMemoria;
import com.travelgo.dao.UsuarioDAOMemoria;
import com.travelgo.dto.PagoRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.FacturaService;
import com.travelgo.service.PagoService;
import com.travelgo.service.UsuarioService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

/**
 * Arma a mano, sin Spring, los DAO en memoria y los services reales de usuarios, reservas, pagos y facturas.
 * La consola y las páginas web de reservas, pagos y administración usan este mismo backend, así las dos
 * interfaces ven los mismos datos y aplican las mismas reglas que /api/pagos y /api/facturas.
 * Los datos duran mientras la consola o la aplicación web están abiertas.
 */
public final class BackendConsola {

	/** Contraseña de todos los usuarios de ejemplo. */
	public static final String PASSWORD_EJEMPLO = "123456";

	/** Correos de los usuarios de ejemplo, uno por rol y un segundo cliente. */
	public static final List<String> CORREOS_EJEMPLO = List.of(
		"cliente@travelgo.com", "ana@travelgo.com", "agente@travelgo.com", "admin@travelgo.com");

	/** El mismo codificador BCrypt que usa SecurityConfig para la web. */
	private static final PasswordEncoder ENCRIPTADOR = new BCryptPasswordEncoder();

	public static final UsuarioService USUARIOS = new UsuarioService(new UsuarioDAOMemoria());

	public static final ReservaDAO RESERVAS = new ReservaDAOMemoria();

	private static final FacturaDAO FACTURA_DAO = new FacturaDAOMemoria();

	public static final PagoService PAGOS = new PagoService(new PagoDAOMemoria(), FACTURA_DAO, RESERVAS);

	public static final FacturaService FACTURAS = new FacturaService(FACTURA_DAO);

	static {
		crearUsuariosDeEjemplo();
		crearReservasDeEjemplo();
	}

	private BackendConsola() {
	}

	/**
	 * Valida el correo y la contraseña igual que el login web: busca el usuario por correo con UsuarioService y
	 * compara la contraseña con BCrypt.
	 *
	 * @return el usuario si los datos son correctos; vacío si el correo no existe o la contraseña no coincide
	 */
	public static Optional<Usuario> iniciarSesion(String correo, String password) {
		return USUARIOS.buscarPorCorreo(correo.trim())
			.filter(usuario -> ENCRIPTADOR.matches(password, usuario.getPassword()));
	}

	/**
	 * Crea una reserva PENDIENTE del usuario que inició sesión y la guarda.
	 *
	 * @param usuario   dueño de la reserva (el usuario autenticado, no uno elegido)
	 * @param paquete   paquete elegido
	 * @param servicios vuelo, hotel y transporte elegidos (puede estar vacía)
	 * @throws IllegalStateException si un servicio no está disponible
	 */
	public static Reserva guardarReserva(Usuario usuario, PaqueteMaqueta paquete,
			List<ServicioReservableMaqueta> servicios) {
		Reserva reserva = new Reserva(usuario, paquete.aModelo());
		for (ServicioReservableMaqueta servicio : servicios) {
			reserva.agregarServicio(servicio.aModelo());
		}
		return RESERVAS.guardar(reserva);
	}

	/**
	 * Cliente y Agente reservan y pagan; para el Administrador "no aplica" (docs/matriz_permisos.md).
	 */
	public static boolean puedeReservar(Usuario usuario) {
		return usuario.getRol() == RolUsuario.CLIENTE || usuario.getRol() == RolUsuario.AGENTE;
	}

	/**
	 * El Cliente solo ve sus propias reservas; el Agente gestiona las de todos los clientes.
	 */
	public static List<Reserva> reservasVisibles(Usuario usuario) {
		if (usuario.getRol() == RolUsuario.AGENTE) {
			return RESERVAS.listarTodas();
		}
		return RESERVAS.listarPorUsuario(usuario.getId());
	}

	/**
	 * Busca una reserva que el usuario puede ver: el Agente cualquiera, el Cliente solo las suyas.
	 *
	 * @return la reserva, o vacío si no existe o es de otro cliente
	 */
	public static Optional<Reserva> buscarReservaVisible(Usuario usuario, String reservaId) {
		return reservasVisibles(usuario).stream()
			.filter(reserva -> reserva.getId().equals(reservaId))
			.findFirst();
	}

	/**
	 * Busca la factura de una reserva con FacturaService. Vacío si la reserva no está pagada.
	 */
	public static Optional<Factura> buscarFactura(Reserva reserva) {
		try {
			return Optional.of(FACTURAS.obtenerPorReserva(reserva.getId()));
		} catch (RecursoNoEncontradoException e) {
			return Optional.empty();
		}
	}

	/**
	 * Busca el pago de una reserva con PagoService. Vacío si la reserva no tiene pago.
	 */
	public static Optional<Pago> buscarPago(Reserva reserva) {
		try {
			return Optional.of(PAGOS.obtenerPorReserva(reserva.getId()));
		} catch (RecursoNoEncontradoException e) {
			return Optional.empty();
		}
	}

	/**
	 * Un usuario por rol y un segundo cliente, para comprobar que cada cliente solo ve sus reservas.
	 * Las contraseñas se guardan cifradas con BCrypt, como en el registro web.
	 */
	private static void crearUsuariosDeEjemplo() {
		crearUsuario("u1", "Cliente de prueba", "cliente@travelgo.com", RolUsuario.CLIENTE);
		crearUsuario("u2", "Ana Torres", "ana@travelgo.com", RolUsuario.CLIENTE);
		crearUsuario("u3", "Agente de prueba", "agente@travelgo.com", RolUsuario.AGENTE);
		crearUsuario("u4", "Administrador", "admin@travelgo.com", RolUsuario.ADMINISTRADOR);
	}

	private static void crearUsuario(String id, String nombre, String correo, RolUsuario rol) {
		USUARIOS.guardar(new Usuario(id, nombre, correo, ENCRIPTADOR.encode(PASSWORD_EJEMPLO), rol));
	}

	/**
	 * Para el cliente de prueba: una reserva pendiente, una cancelada y una ya pagada (con su pago y su factura),
	 * para probar las tres reglas del pago. Para Ana: una pendiente, que el cliente de prueba no debe ver.
	 */
	private static void crearReservasDeEjemplo() {
		Usuario cliente = usuario("cliente@travelgo.com");
		Usuario ana = usuario("ana@travelgo.com");

		PaqueteMaqueta machuPicchu = PaqueteMaqueta.buscar("2");
		guardarReserva(cliente, machuPicchu,
			List.of(ServiciosPaqueteMaqueta.de(machuPicchu).vuelosDisponibles().get(1)));

		PaqueteMaqueta cartagena = PaqueteMaqueta.buscar("1");
		Reserva cancelada = guardarReserva(cliente, cartagena, List.of());
		cancelada.cancelarReserva();
		RESERVAS.guardar(cancelada);

		PaqueteMaqueta cancun = PaqueteMaqueta.buscar("3");
		Reserva pagada = guardarReserva(cliente, cancun,
			List.of(ServiciosPaqueteMaqueta.de(cancun).hotelesDisponibles().get(0)));
		PAGOS.pagar(new PagoRequest(pagada.getId(), MetodoPago.TARJETA_CREDITO));

		guardarReserva(ana, cartagena, List.of(ServiciosPaqueteMaqueta.de(cartagena).hotelesDisponibles().get(0)));
	}

	private static Usuario usuario(String correo) {
		return USUARIOS.buscarPorCorreo(correo).orElseThrow();
	}

}
