package com.travelgo.chatbot;

import com.travelgo.chatbot.estrategia.ComoPagarEstrategia;
import com.travelgo.chatbot.estrategia.CuponesEstrategia;
import com.travelgo.chatbot.estrategia.EstadoReservaEstrategia;
import com.travelgo.chatbot.estrategia.NoEntendidaEstrategia;
import com.travelgo.chatbot.estrategia.PagosPendientesEstrategia;
import com.travelgo.chatbot.estrategia.PaquetesBaratosEstrategia;
import com.travelgo.chatbot.estrategia.PaquetesLujoEstrategia;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.model.Cupon;
import com.travelgo.model.EstadoFactura;
import com.travelgo.model.Factura;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.TipoCupon;
import com.travelgo.model.Usuario;
import com.travelgo.service.CuponService;
import com.travelgo.service.FacturaService;
import com.travelgo.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de cada estrategia del chatbot, con los servicios simulados. */
@ExtendWith(MockitoExtension.class)
class EstrategiasChatbotTest {

	private static final ContextoChat VISITANTE = new ContextoChat(null);
	private static final ContextoChat ANA = new ContextoChat("ana@correo.com");

	private final ExtractorDatos extractor = new ExtractorDatos();

	@Mock
	private ReservaService reservaService;
	@Mock
	private FacturaService facturaService;
	@Mock
	private UsuarioDAO usuarioDAO;
	@Mock
	private CuponService cuponService;
	@Mock
	private BuscadorPaquetes buscador;

	private DatosMensaje datos(String mensaje) {
		return extractor.extraer(mensaje, List.of("Bogotá", "Cartagena"));
	}

	private PaqueteTuristico paquete(String nombre, String precio) {
		return new PaqueteTuristico(nombre, "desc", new BigDecimal(precio), 4);
	}

	// ---------- Consultas privadas ----------

	@Test
	@DisplayName("Mi reserva sin sesión pide iniciar sesión y no consulta nada")
	void estadoReservaSinSesion() {
		EstadoReservaEstrategia estrategia = new EstadoReservaEstrategia(reservaService);
		DatosMensaje d = datos("¿Cómo va mi reserva?");

		assertTrue(estrategia.aplica(d));
		assertTrue(estrategia.responder(d, VISITANTE).texto().contains("iniciar sesión"));
		verifyNoInteractions(reservaService);
	}

	@Test
	@DisplayName("Mi reserva con sesión consulta solo las reservas del propio usuario")
	void estadoReservaConSesion() {
		Reserva reserva = mock(Reserva.class);
		when(reserva.getPaqueteTuristico()).thenReturn(paquete("Cartagena Esencial", "1200000"));
		when(reserva.getEstado()).thenReturn("PENDIENTE");
		when(reserva.getTotal()).thenReturn(1200000.0);
		when(reserva.getFechaReserva()).thenReturn(LocalDate.of(2026, 11, 10));
		when(reservaService.listarPropias("ana@correo.com")).thenReturn(List.of(reserva));

		String texto = new EstadoReservaEstrategia(reservaService).responder(datos("mi reserva"), ANA).texto();

		assertTrue(texto.contains("Cartagena Esencial"));
		assertTrue(texto.contains("PENDIENTE"));
		assertTrue(texto.contains("$1.200.000 COP"));
		verify(reservaService).listarPropias("ana@correo.com");
		verify(reservaService, never()).listar();
	}

	@Test
	@DisplayName("Mis pagos pendientes sin sesión no consulta nada")
	void pagosSinSesion() {
		PagosPendientesEstrategia estrategia = new PagosPendientesEstrategia(facturaService, usuarioDAO);
		DatosMensaje d = datos("¿Tengo pagos pendientes?");

		assertTrue(estrategia.aplica(d));
		assertTrue(estrategia.responder(d, VISITANTE).texto().contains("iniciar sesión"));
		verifyNoInteractions(facturaService, usuarioDAO);
	}

	@Test
	@DisplayName("Mis pagos pendientes muestra solo las facturas PENDIENTE del usuario")
	void pagosPendientes() {
		Usuario ana = new Usuario("u-ana", "Ana", "ana@correo.com", "hash", com.travelgo.model.RolUsuario.CLIENTE);
		Factura pendiente = mock(Factura.class);
		when(pendiente.getEstado()).thenReturn(EstadoFactura.PENDIENTE);
		when(pendiente.getNumero()).thenReturn("FAC-00000007");
		when(pendiente.getTotal()).thenReturn(900000.0);
		when(pendiente.getFechaEmision()).thenReturn(LocalDate.of(2026, 11, 12));
		Factura pagada = mock(Factura.class);
		when(pagada.getEstado()).thenReturn(EstadoFactura.PAGADA);
		when(usuarioDAO.buscarPorCorreo("ana@correo.com")).thenReturn(Optional.of(ana));
		when(facturaService.listarPorUsuario("u-ana")).thenReturn(List.of(pendiente, pagada));

		String texto = new PagosPendientesEstrategia(facturaService, usuarioDAO)
			.responder(datos("pagos pendientes"), ANA).texto();

		assertTrue(texto.contains("FAC-00000007"));
		assertTrue(texto.contains("$900.000 COP"));
		assertFalse(texto.contains("PAGADA"));
	}

	@Test
	@DisplayName("Sin facturas pendientes lo dice")
	void sinPendientes() {
		Usuario ana = new Usuario("u-ana", "Ana", "ana@correo.com", "hash", com.travelgo.model.RolUsuario.CLIENTE);
		when(usuarioDAO.buscarPorCorreo("ana@correo.com")).thenReturn(Optional.of(ana));
		when(facturaService.listarPorUsuario("u-ana")).thenReturn(List.of());

		assertTrue(new PagosPendientesEstrategia(facturaService, usuarioDAO)
			.responder(datos("pagos pendientes"), ANA).texto().contains("No tienes pagos pendientes"));
	}

	// ---------- Consultas públicas ----------

	@Test
	@DisplayName("Cupones: lista solo los vigentes")
	void cupones() {
		LocalDate hoy = LocalDate.now();
		Cupon vigente = new Cupon("VERANO10", TipoCupon.PORCENTAJE, 10, hoy.minusDays(1), hoy.plusDays(30), 5);
		Cupon vencido = new Cupon("VIEJO5", TipoCupon.PORCENTAJE, 5, hoy.minusDays(60), hoy.minusDays(30), 5);
		when(cuponService.listar()).thenReturn(List.of(vigente, vencido));

		CuponesEstrategia estrategia = new CuponesEstrategia(cuponService);
		DatosMensaje d = datos("¿Hay cupones?");

		assertTrue(estrategia.aplica(d));
		String texto = estrategia.responder(d, VISITANTE).texto();
		assertTrue(texto.contains("VERANO10"));
		assertTrue(texto.contains("10 % de descuento"));
		assertFalse(texto.contains("VIEJO5"));
	}

	@Test
	@DisplayName("Cupones: sin vigentes lo dice")
	void cuponesVacios() {
		when(cuponService.listar()).thenReturn(List.of());

		assertTrue(new CuponesEstrategia(cuponService).responder(datos("cupones"), VISITANTE).texto()
			.contains("no hay cupones vigentes"));
	}

	@Test
	@DisplayName("Cómo pago: explica el flujo")
	void comoPagar() {
		ComoPagarEstrategia estrategia = new ComoPagarEstrategia();
		DatosMensaje d = datos("¿Cómo pago?");

		assertTrue(estrategia.aplica(d));
		assertTrue(estrategia.responder(d, VISITANTE).texto().contains("simulado"));
	}

	@Test
	@DisplayName("Paquetes baratos: usa el destino y muestra el precio como referencia")
	void baratos() {
		when(buscador.masBaratos("Bogotá")).thenReturn(List.of(paquete("Bogotá Cultural", "800000")));
		PaquetesBaratosEstrategia estrategia = new PaquetesBaratosEstrategia(buscador);
		DatosMensaje d = datos("Paquetes baratos para Bogotá");

		assertTrue(estrategia.aplica(d));
		String texto = estrategia.responder(d, VISITANTE).texto();
		assertTrue(texto.contains("Bogotá Cultural"));
		assertTrue(texto.contains("$800.000 COP (precio de referencia)"));
	}

	@Test
	@DisplayName("Paquetes de lujo: mientras no exista la gama, lo dice con claridad")
	void lujoSinGama() {
		when(buscador.porGama("Cartagena", Gama.LUJO)).thenReturn(Optional.empty());
		PaquetesLujoEstrategia estrategia = new PaquetesLujoEstrategia(buscador);
		DatosMensaje d = datos("Paquetes con hoteles de lujo en Cartagena");

		assertTrue(estrategia.aplica(d));
		assertTrue(estrategia.responder(d, VISITANTE).texto().contains("Todavía no puedo filtrar"));
	}

	@Test
	@DisplayName("Paquetes de lujo: cuando hay gama, muestra los paquetes")
	void lujoConGama() {
		when(buscador.porGama("Cartagena", Gama.LUJO))
			.thenReturn(Optional.of(List.of(paquete("Cartagena Premium", "4500000"))));

		String texto = new PaquetesLujoEstrategia(buscador)
			.responder(datos("hoteles de lujo en Cartagena"), VISITANTE).texto();

		assertTrue(texto.contains("Cartagena Premium"));
	}

	@Test
	@DisplayName("Si no entiende, ofrece lo que sí maneja")
	void noEntendida() {
		NoEntendidaEstrategia estrategia = new NoEntendidaEstrategia();
		DatosMensaje d = datos("asdf qwerty");

		assertTrue(estrategia.aplica(d));
		RespuestaChatbot r = estrategia.responder(d, VISITANTE);
		assertTrue(r.texto().contains("No entendí"));
		assertFalse(r.sugerencias().isEmpty());
	}
}
