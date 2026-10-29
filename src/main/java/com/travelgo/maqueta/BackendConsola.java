package com.travelgo.maqueta;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.FacturaDAOMemoria;
import com.travelgo.dao.PagoDAOMemoria;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dao.ReservaDAOMemoria;
import com.travelgo.dto.PagoRequest;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.FacturaService;
import com.travelgo.service.PagoService;

import java.util.List;

/**
 * Arma a mano, sin Spring, los DAO en memoria y los services reales de reservas, pagos y facturas.
 * Así la consola usa las mismas reglas que /api/pagos y /api/facturas.
 * Los datos duran mientras la consola está abierta.
 */
public final class BackendConsola {

	/** Cliente de ejemplo dueño de las reservas. Lo reemplaza el usuario del login cuando esté listo. */
	public static final Usuario CLIENTE = new Usuario("u1", "Cliente de prueba", "cliente@travelgo.com", "123456",
		RolUsuario.CLIENTE);

	public static final ReservaDAO RESERVAS = new ReservaDAOMemoria();

	private static final FacturaDAO FACTURA_DAO = new FacturaDAOMemoria();

	public static final PagoService PAGOS = new PagoService(new PagoDAOMemoria(), FACTURA_DAO, RESERVAS);

	public static final FacturaService FACTURAS = new FacturaService(FACTURA_DAO);

	static {
		crearReservasDeEjemplo();
	}

	private BackendConsola() {
	}

	/**
	 * Crea una reserva PENDIENTE del cliente de ejemplo y la guarda.
	 *
	 * @param paquete   paquete elegido
	 * @param servicios vuelo, hotel y transporte elegidos (puede estar vacía)
	 * @throws IllegalStateException si un servicio no está disponible
	 */
	public static Reserva guardarReserva(PaqueteMaqueta paquete, List<ServicioReservableMaqueta> servicios) {
		Reserva reserva = new Reserva(CLIENTE, paquete.aModelo());
		for (ServicioReservableMaqueta servicio : servicios) {
			reserva.agregarServicio(servicio.aModelo());
		}
		return RESERVAS.guardar(reserva);
	}

	/**
	 * Una reserva pendiente, una cancelada y una ya pagada (con su pago y su factura), para probar las tres
	 * reglas del pago.
	 */
	private static void crearReservasDeEjemplo() {
		PaqueteMaqueta machuPicchu = PaqueteMaqueta.buscar("2");
		guardarReserva(machuPicchu, List.of(ServiciosPaqueteMaqueta.de(machuPicchu).vuelosDisponibles().get(1)));

		PaqueteMaqueta cartagena = PaqueteMaqueta.buscar("1");
		Reserva cancelada = guardarReserva(cartagena, List.of());
		cancelada.cancelarReserva();
		RESERVAS.guardar(cancelada);

		PaqueteMaqueta cancun = PaqueteMaqueta.buscar("3");
		Reserva pagada = guardarReserva(cancun, List.of(ServiciosPaqueteMaqueta.de(cancun).hotelesDisponibles().get(0)));
		PAGOS.pagar(new PagoRequest(pagada.getId(), MetodoPago.TARJETA_CREDITO));
	}

}
