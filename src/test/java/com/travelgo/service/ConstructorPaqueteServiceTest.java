package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dto.ArmarPaqueteRequest;
import com.travelgo.dto.PaqueteRealRespuestaDTO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.hotel.TarifaAgencia;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import com.travelgo.proveedor.moneda.TasaCambio;
import com.travelgo.service.ConstructorPaqueteService.Resultado;
import com.travelgo.service.ConstructorPaqueteService.Solicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas del constructor de paquetes de la web (Sprint 4, Integrante 3). Sin APIs ni base de datos:
 * los proveedores y PaqueteRealService son simulados.
 */
@ExtendWith(MockitoExtension.class)
class ConstructorPaqueteServiceTest {

	private static final LocalDate IDA = LocalDate.now().plusDays(30);
	private static final LocalDate VUELTA = IDA.plusDays(4);
	private static final Solicitud SOLICITUD = new Solicitud(1L, "BOG", "CTG", "g297476", IDA, VUELTA);

	@Mock private ProveedorVuelos proveedorVuelos;
	@Mock private ProveedorHoteles proveedorHoteles;
	@Mock private ProveedorTasaCambio proveedorTasa;
	@Mock private PaqueteRealService paqueteRealService;
	@Mock private DestinoDAO destinoDAO;
	@Mock private ServicioReservableDAO servicioDAO;
	@Mock private PaqueteServicioDAO paqueteServicioDAO;

	private ConstructorPaqueteService service;

	private final Vuelo vueloCaro = vuelo("v-caro", 600000);
	private final Vuelo vueloBarato = vuelo("v-barato", 350000);

	@BeforeEach
	void preparar() {
		service = new ConstructorPaqueteService(proveedorVuelos, proveedorHoteles, proveedorTasa, paqueteRealService,
			destinoDAO, servicioDAO, paqueteServicioDAO);
		Destino cartagena = new Destino("Cartagena de Indias", "Playas", "Colombia", "Cartagena");
		cartagena.setId(1L);
		lenient().when(destinoDAO.buscarPorId(1L)).thenReturn(Optional.of(cartagena));
		lenient().when(proveedorTasa.obtenerUsdACop())
			.thenReturn(new TasaCambio(4000, "open.er-api.com", Instant.now()));
		lenient().when(proveedorVuelos.buscarVuelos("BOG", "CTG", IDA, null)).thenReturn(List.of(vueloCaro, vueloBarato));
		lenient().when(proveedorHoteles.buscarHoteles("g297476", ConstructorPaqueteService.LIMITE_HOTELES))
			.thenReturn(List.of(hotel("g297476-d1", "Hotel Caribe"), hotel("g297476-d2", "Hotel Sin Tarifa")));
		lenient().when(proveedorHoteles.consultarTarifa(eq("g297476-d1"), eq(IDA), eq(VUELTA), anyInt(), anyInt()))
			.thenReturn(Optional.of(oferta("g297476-d1", 100.0)));
		lenient().when(proveedorHoteles.consultarTarifa(eq("g297476-d2"), eq(IDA), eq(VUELTA), anyInt(), anyInt()))
			.thenReturn(Optional.empty());
	}

	private static Vuelo vuelo(String id, double precio) {
		LocalDateTime salida = IDA.atTime(8, 0);
		return new Vuelo(id, precio, true, "Avianca", "BOG", "CTG", salida, salida.plusMinutes(95),
			"IGNAV", "ignav-" + id, LocalDateTime.now(), null);
	}

	private static HotelResumen hotel(String clave, String nombre) {
		return new HotelResumen(clave, nombre, "Hotel", null, "https://tripadvisor.com/" + clave, 10.4, -75.5, 4.5);
	}

	private static OfertaHotel oferta(String clave, double usdPorNoche) {
		return new OfertaHotel(clave, new TarifaAgencia("Booking.com", usdPorNoche), List.of(), "USD", IDA, VUELTA,
			4, 1, 2, Instant.now(), "Xotelo (precios de TripAdvisor)");
	}

	private static PaqueteRealRespuestaDTO respuesta() {
		return new PaqueteRealRespuestaDTO(
				10L,
				"Cartagena 4 noches",
				"desc",
				1950000.0,
				"COP",
				5,
				true,
				List.of("Cartagena de Indias"),
				List.of(),
				null
		);
	}

	@Test
	@DisplayName("La búsqueda ordena los vuelos por precio y pasa los hoteles de USD a COP con la tasa real")
	void buscaYConvierte() {
		Resultado resultado = service.buscar(SOLICITUD);

		assertEquals(List.of("v-barato", "v-caro"), resultado.vuelos().stream().map(Vuelo::getId).toList());
		assertEquals(1, resultado.hoteles().size());
		assertEquals(400000, resultado.hoteles().get(0).precioNocheCop());   // 100 USD x 4.000
		assertEquals(1600000, resultado.hoteles().get(0).totalCop());        // 4 noches
		assertEquals(4000, resultado.tasa().copPorUsd());
		assertTrue(resultado.avisos().get(0).contains("1 de 2 hoteles"));
	}

	@Test
	@DisplayName("Sin llave de Ignav la búsqueda no falla: avisa y sigue con los hoteles")
	void sinLlaveDeIgnav() {
		when(proveedorVuelos.buscarVuelos("BOG", "CTG", IDA, null))
			.thenThrow(new IllegalStateException("La API Key de Ignav no está configurada."));

		Resultado resultado = service.buscar(SOLICITUD);

		assertTrue(resultado.vuelos().isEmpty());
		assertEquals(1, resultado.hoteles().size());
		assertTrue(resultado.avisos().stream().anyMatch(a -> a.contains("IGNAV_API_KEY")));
	}

	@Test
	@DisplayName("Si Xotelo está caído, la búsqueda avisa y devuelve los vuelos")
	void xoteloCaido() {
		when(proveedorHoteles.buscarHoteles("g297476", ConstructorPaqueteService.LIMITE_HOTELES))
			.thenThrow(new ProveedorNoDisponibleException("timeout"));

		Resultado resultado = service.buscar(SOLICITUD);

		assertEquals(2, resultado.vuelos().size());
		assertTrue(resultado.hoteles().isEmpty());
		assertTrue(resultado.avisos().stream().anyMatch(a -> a.contains("Xotelo")));
	}

	@Test
	@DisplayName("Sin tasa de cambio no se muestran hoteles (no hay cómo dar su precio en pesos)")
	void sinTasaDeCambio() {
		when(proveedorTasa.obtenerUsdACop()).thenThrow(new ProveedorNoDisponibleException("sin conexión"));

		Resultado resultado = service.buscar(SOLICITUD);

		assertEquals(2, resultado.vuelos().size());
		assertTrue(resultado.hoteles().isEmpty());
		assertNull(resultado.tasa());
		assertTrue(resultado.avisos().stream().anyMatch(a -> a.contains("tasa de cambio")));
	}

	@Test
	@DisplayName("Armar guarda la copia del vuelo y delega el hotel en PaqueteRealService con el vuelo como extra")
	void armaElPaquete() {
		when(proveedorVuelos.obtenerEnlaceReserva("ignav-v-barato")).thenReturn("https://avianca.com/reserva");
		when(paqueteRealService.armar(any())).thenReturn(respuesta());

		PaqueteRealRespuestaDTO paquete = service.armar(SOLICITUD, "v-barato", "g297476-d1", "Cartagena 4 noches", "");

		assertEquals(10L, paquete.id());
		ArgumentCaptor<Vuelo> vuelo = ArgumentCaptor.forClass(Vuelo.class);
		verify(servicioDAO).guardar(vuelo.capture());
		assertEquals("https://avianca.com/reserva", vuelo.getValue().getEnlaceReserva());
		assertEquals("ignav-v-barato", vuelo.getValue().getIdExterno());
		assertEquals(350000, vuelo.getValue().getPrecio());
		assertTrue(vuelo.getValue().isDisponible());

		ArgumentCaptor<ArmarPaqueteRequest> request = ArgumentCaptor.forClass(ArmarPaqueteRequest.class);
		verify(paqueteRealService).armar(request.capture());
		assertEquals(List.of(1L), request.getValue().destinosIds());
		assertEquals("Cartagena", request.getValue().ciudadNombre());
		assertEquals("g297476-d1", request.getValue().hotelClave());
		assertEquals(IDA, request.getValue().entrada());
		assertEquals(VUELTA, request.getValue().salida());
		assertEquals(List.of(vuelo.getValue().getId()), request.getValue().serviciosExtraIds());
		assertTrue(request.getValue().descripcion().contains("Avianca"));
	}

	@Test
	@DisplayName("Se puede armar solo con hotel")
	void soloHotel() {
		when(paqueteRealService.armar(any())).thenReturn(respuesta());

		service.armar(SOLICITUD, null, "g297476-d1", "Solo hotel", null);

		verify(servicioDAO, never()).guardar(any());
		ArgumentCaptor<ArmarPaqueteRequest> request = ArgumentCaptor.forClass(ArmarPaqueteRequest.class);
		verify(paqueteRealService).armar(request.capture());
		assertTrue(request.getValue().serviciosExtraIds().isEmpty());
	}

	@Test
	@DisplayName("Si la búsqueda de vuelos venció, no se guarda nada")
	void busquedaVencida() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
			() -> service.armar(SOLICITUD, "id-que-ya-no-existe", "g297476-d1", "Paquete", null));

		assertTrue(e.getMessage().contains("venció"));
		verify(servicioDAO, never()).guardar(any());
		verify(paqueteRealService, never()).armar(any());
	}

	@Test
	@DisplayName("El hotel y el nombre son obligatorios")
	void hotelYNombreObligatorios() {
		assertThrows(IllegalArgumentException.class, () -> service.armar(SOLICITUD, "v-barato", "", "Paquete", null));
		assertThrows(IllegalArgumentException.class, () -> service.armar(SOLICITUD, null, "g297476-d1", " ", null));
		verify(paqueteRealService, never()).armar(any());
	}

	@Test
	@DisplayName("Las fechas se validan: ida futura y vuelta después de la ida")
	void validaFechas() {
		LocalDate ayer = LocalDate.now().minusDays(1);
		assertThrows(IllegalArgumentException.class,
			() -> service.buscar(new Solicitud(1L, "BOG", "CTG", "g297476", ayer, IDA)));
		assertThrows(IllegalArgumentException.class,
			() -> service.buscar(new Solicitud(1L, "BOG", "CTG", "g297476", IDA, IDA)));
		assertThrows(IllegalArgumentException.class,
			() -> service.buscar(new Solicitud(1L, "BOG", "bog", "g297476", IDA, VUELTA)));
	}

	@Test
	@DisplayName("Los servicios del paquete salen ordenados: primero el vuelo y luego el hotel")
	void serviciosOrdenados() {
		Hotel hotel = new Hotel("h", 400000, true, "Hotel Caribe", "Cartagena", "Doble", 4);
		when(paqueteServicioDAO.listarServicios(10L)).thenReturn(List.of(hotel, vueloBarato));

		List<ServicioReservable> servicios = service.serviciosDe(10L);

		assertEquals(List.of(vueloBarato, hotel), servicios);
	}
}
