package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.hotel.TarifaAgencia;
import com.travelgo.service.ConstructorPaqueteService.Resultado;
import com.travelgo.service.ConstructorPaqueteService.Solicitud;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas del constructor de paquetes con datos reales (Sprint 4, Integrante 3). Sin APIs ni base de datos. */
@ExtendWith(MockitoExtension.class)
class ConstructorPaqueteServiceTest {

	private static final LocalDate IDA = LocalDate.now().plusDays(30);
	private static final LocalDate VUELTA = IDA.plusDays(4);
	private static final Solicitud SOLICITUD = new Solicitud(1L, "BOG", "CTG", "g297476", IDA, VUELTA);

	@Mock private ProveedorVuelos proveedorVuelos;
	@Mock private ProveedorHoteles proveedorHoteles;
	@Mock private DestinoDAO destinoDAO;
	@Mock private PaqueteDAO paqueteDAO;
	@Mock private ServicioReservableDAO servicioDAO;
	@Mock private PaqueteServicioDAO paqueteServicioDAO;

	private ConstructorPaqueteService service;

	private final Vuelo vueloCaro = vuelo("v-caro", 600000);
	private final Vuelo vueloBarato = vuelo("v-barato", 350000);

	@BeforeEach
	void preparar() {
		service = new ConstructorPaqueteService(proveedorVuelos, proveedorHoteles, destinoDAO, paqueteDAO,
			servicioDAO, paqueteServicioDAO, new BigDecimal("4000"));
		Destino cartagena = new Destino("Cartagena de Indias", "Playas", "Colombia", "Cartagena");
		cartagena.setId(1L);
		lenient().when(destinoDAO.buscarPorId(1L)).thenReturn(Optional.of(cartagena));
		lenient().when(proveedorVuelos.buscarVuelos("BOG", "CTG", IDA, null)).thenReturn(List.of(vueloCaro, vueloBarato));
		lenient().when(proveedorHoteles.buscarHoteles("g297476", ConstructorPaqueteService.LIMITE_HOTELES))
			.thenReturn(List.of(hotel("h-1", "Hotel Caribe"), hotel("h-2", "Hotel Sin Tarifa")));
		lenient().when(proveedorHoteles.consultarTarifa(eq("h-1"), eq(IDA), eq(VUELTA), anyInt(), anyInt()))
			.thenReturn(Optional.of(oferta("h-1", 100.0)));
		lenient().when(proveedorHoteles.consultarTarifa(eq("h-2"), eq(IDA), eq(VUELTA), anyInt(), anyInt()))
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

	@Test
	@DisplayName("La búsqueda ordena los vuelos por precio y convierte los hoteles de USD a COP")
	void buscaYConvierte() {
		Resultado resultado = service.buscar(SOLICITUD);

		assertEquals(List.of("v-barato", "v-caro"), resultado.vuelos().stream().map(Vuelo::getId).toList());
		assertEquals(1, resultado.hoteles().size());
		assertEquals(400000, resultado.hoteles().get(0).precioNocheCop());   // 100 USD x 4.000
		assertEquals(1600000, resultado.hoteles().get(0).totalCop());        // 4 noches
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
	@DisplayName("Armar guarda la copia del vuelo y del hotel, calcula el precio y los une al paquete")
	void armaElPaquete() {
		when(proveedorVuelos.obtenerEnlaceReserva("ignav-v-barato")).thenReturn("https://avianca.com/reserva");
		when(paqueteDAO.guardar(any())).thenAnswer(inv -> {
			PaqueteTuristico p = inv.getArgument(0);
			p.setId(10L);
			return p;
		});

		PaqueteTuristico paquete = service.armar(SOLICITUD, "v-barato", "h-1", "Cartagena 4 noches", "");

		// 350.000 del vuelo + 400.000 x 4 noches
		assertEquals(0, new BigDecimal("1950000").compareTo(paquete.getPrecio()));
		assertEquals(5, paquete.getDuracionDias());
		assertEquals("Cartagena", paquete.getDestinos().get(0).getCiudad());
		assertTrue(paquete.getDescripcion().contains("Hotel Caribe"));

		ArgumentCaptor<ServicioReservable> guardados = ArgumentCaptor.forClass(ServicioReservable.class);
		verify(servicioDAO, times(2)).guardar(guardados.capture());
		Vuelo vuelo = (Vuelo) guardados.getAllValues().get(0);
		Hotel hotel = (Hotel) guardados.getAllValues().get(1);
		assertEquals("https://avianca.com/reserva", vuelo.getEnlaceReserva());
		assertEquals("ignav-v-barato", vuelo.getIdExterno());
		assertTrue(vuelo.isDisponible());
		assertEquals(400000, hotel.getPrecio());
		assertEquals("XOTELO", hotel.getFuente());
		assertEquals("https://tripadvisor.com/h-1", hotel.getEnlaceReserva());
		verify(paqueteServicioDAO, times(2)).asociar(eq(10L), any());
	}

	@Test
	@DisplayName("Si la búsqueda de vuelos venció, no se guarda nada")
	void busquedaVencida() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
			() -> service.armar(SOLICITUD, "id-que-ya-no-existe", "h-1", "Paquete", null));

		assertTrue(e.getMessage().contains("venció"));
		verify(paqueteDAO, never()).guardar(any());
		verify(paqueteServicioDAO, never()).asociar(any(), any());
	}

	@Test
	@DisplayName("Hay que elegir al menos un vuelo o un hotel, y poner nombre")
	void sinEleccion() {
		assertThrows(IllegalArgumentException.class, () -> service.armar(SOLICITUD, "", null, "Paquete", null));
		assertThrows(IllegalArgumentException.class, () -> service.armar(SOLICITUD, "v-barato", null, " ", null));
		verify(paqueteDAO, never()).guardar(any());
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
	@DisplayName("Solo convierte COP y USD")
	void conversion() {
		assertEquals(Optional.of(250000.0), service.aCop(250000.4, "COP"));
		assertEquals(Optional.of(402000.0), service.aCop(100.5, "USD"));
		assertTrue(service.aCop(100, "EUR").isEmpty());
	}
}
