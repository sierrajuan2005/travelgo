package com.travelgo.integracion;

import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Pruebas del adaptador de Ignav con respuestas simuladas (Sprint 4, Integrante 4).
 * No llaman a internet y no gastan la cuota gratuita de la llave.
 */
class IgnavProveedorVuelosTest {

	private static final String BASE = "https://ignav.com/api";
	private static final String LLAVE = "llave-de-prueba";
	private static final LocalDate FECHA = LocalDate.of(2026, 12, 10);

	private static final String RESPUESTA_OK = """
		{"origin":"BOG","destination":"CTG","departure_date":"2026-12-10",
		 "observed_at":"2026-11-12T15:00:00Z",
		 "itineraries":[
		  {"ignav_id":"ign-1",
		   "price":{"amount":350000,"currency":"COP","status":"verified"},
		   "outbound":{"carrier":"Avianca","duration_minutes":75,"segments":[
		     {"marketing_carrier_code":"AV","flight_number":"9301","operating_carrier_name":"Avianca",
		      "departure_airport":"BOG","departure_time_local":"2026-12-10T07:00:00",
		      "arrival_airport":"CTG","arrival_time_local":"2026-12-10T08:15:00","duration_minutes":75}]}},
		  {"ignav_id":"ign-usd",
		   "price":{"amount":120,"currency":"USD","status":"verified"},
		   "outbound":{"carrier":"Otra","duration_minutes":75,"segments":[
		     {"departure_airport":"BOG","departure_time_local":"2026-12-10T09:00:00",
		      "arrival_airport":"CTG","arrival_time_local":"2026-12-10T10:15:00","duration_minutes":75}]}},
		  {"ignav_id":"ign-escala",
		   "price":{"amount":300000,"currency":"COP","status":"verified"},
		   "outbound":{"carrier":"Avianca","duration_minutes":240,"segments":[
		     {"departure_airport":"BOG","departure_time_local":"2026-12-10T06:00:00",
		      "arrival_airport":"MDE","arrival_time_local":"2026-12-10T07:00:00","duration_minutes":60},
		     {"departure_airport":"MDE","departure_time_local":"2026-12-10T08:00:00",
		      "arrival_airport":"CTG","arrival_time_local":"2026-12-10T09:30:00","duration_minutes":90}]}},
		  {"ignav_id":"ign-sinverificar",
		   "price":{"amount":280000,"currency":"COP","status":"estimated"},
		   "outbound":{"carrier":"Avianca","duration_minutes":75,"segments":[
		     {"departure_airport":"BOG","departure_time_local":"2026-12-10T12:00:00",
		      "arrival_airport":"CTG","arrival_time_local":"2026-12-10T13:15:00","duration_minutes":75}]}}
		 ]}
		""";

	private MockRestServiceServer servidor;
	private IgnavProveedorVuelos proveedor;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
		servidor = MockRestServiceServer.bindTo(builder).build();
		proveedor = new IgnavProveedorVuelos(builder.build(), LLAVE, "CO");
	}

	@Test
	@DisplayName("Convierte la respuesta de Ignav en vuelos con sus datos de referencia")
	void conviertePrimerVuelo() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andExpect(method(org.springframework.http.HttpMethod.POST))
			.andExpect(header("X-Api-Key", LLAVE))
			.andExpect(content().string(containsString("\"market\":\"CO\"")))
			.andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

		List<Vuelo> vuelos = proveedor.buscarVuelos("bog", "ctg", FECHA, "av");

		Vuelo v = vuelos.get(0);
		assertEquals("Avianca", v.getAerolinea());
		assertEquals("BOG", v.getOrigen());
		assertEquals("CTG", v.getDestino());
		assertEquals(350000.0, v.getPrecio(), 0.001);
		assertTrue(v.isDisponible());
		assertEquals("IGNAV", v.getFuente());
		assertEquals("ign-1", v.getIdExterno());
		// observed_at 15:00 UTC = 10:00 en Bogotá
		assertEquals(LocalDateTime.of(2026, 11, 12, 10, 0), v.getFechaConsulta());
		assertEquals(LocalDateTime.of(2026, 12, 10, 7, 0), v.getFechaSalida());
		servidor.verify();
	}

	@Test
	@DisplayName("Descarta los precios que no están en COP y los itinerarios con escalas")
	void descartaUsdYEscalas() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

		List<Vuelo> vuelos = proveedor.buscarVuelos("BOG", "CTG", FECHA, null);

		assertEquals(List.of("ign-1", "ign-sinverificar"), vuelos.stream().map(Vuelo::getIdExterno).toList());
	}

	@Test
	@DisplayName("Un precio que Ignav no verificó queda como no disponible")
	void precioSinVerificar() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

		Vuelo sinVerificar = proveedor.buscarVuelos("BOG", "CTG", FECHA, null).get(1);

		assertFalse(sinVerificar.isDisponible());
	}

	@Test
	@DisplayName("La segunda búsqueda igual sale de la caché y no gasta cuota")
	void usaLaCache() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

		proveedor.buscarVuelos("BOG", "CTG", FECHA, "AV");
		proveedor.buscarVuelos("bog", "ctg", FECHA, "av");

		servidor.verify(); // la segunda llamada habría fallado: no se esperaba otra petición
	}

	@Test
	@DisplayName("Cuota agotada (429): devuelve lista vacía y el fallo no se guarda en la caché")
	void cuotaAgotada() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withSuccess(RESPUESTA_OK, MediaType.APPLICATION_JSON));

		assertTrue(proveedor.buscarVuelos("BOG", "CTG", FECHA, null).isEmpty());
		assertFalse(proveedor.buscarVuelos("BOG", "CTG", FECHA, null).isEmpty());
		servidor.verify();
	}

	@Test
	@DisplayName("API caída (500): devuelve lista vacía y no lanza error")
	void apiCaida() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertTrue(proveedor.buscarVuelos("BOG", "CTG", FECHA, null).isEmpty());
	}

	@Test
	@DisplayName("Respuesta sin itinerarios devuelve lista vacía")
	void sinItinerarios() {
		servidor.expect(requestTo(BASE + "/fares/one-way"))
			.andRespond(withSuccess("{\"origin\":\"BOG\"}", MediaType.APPLICATION_JSON));

		assertTrue(proveedor.buscarVuelos("BOG", "CTG", FECHA, null).isEmpty());
	}

	@Test
	@DisplayName("Sin llave configurada lanza IllegalStateException sin llamar a la API")
	void sinLlave() {
		IgnavProveedorVuelos sinLlave = new IgnavProveedorVuelos(RestClient.builder().baseUrl(BASE).build(), "", "CO");

		assertThrows(IllegalStateException.class, () -> sinLlave.buscarVuelos("BOG", "CTG", FECHA, null));
		assertThrows(IllegalStateException.class, () -> sinLlave.obtenerEnlaceReserva("ign-1"));
	}

	@Test
	@DisplayName("Datos obligatorios vacíos lanzan IllegalArgumentException")
	void datosInvalidos() {
		assertThrows(IllegalArgumentException.class, () -> proveedor.buscarVuelos(" ", "CTG", FECHA, null));
		assertThrows(IllegalArgumentException.class, () -> proveedor.buscarVuelos("BOG", null, FECHA, null));
		assertThrows(IllegalArgumentException.class, () -> proveedor.buscarVuelos("BOG", "CTG", null, null));
		assertThrows(IllegalArgumentException.class, () -> proveedor.obtenerEnlaceReserva(" "));
	}

	@Test
	@DisplayName("Enlace de reserva: prefiere el de la aerolínea")
	void enlacePrefiereAerolinea() {
		servidor.expect(requestTo(BASE + "/fares/booking-links"))
			.andExpect(header("X-Api-Key", LLAVE))
			.andRespond(withSuccess("""
				{"booking_options":[{"links":[
				  {"provider_name":"Agencia","provider_type":"ota","url":"https://ota.example/v"},
				  {"provider_name":"Avianca","provider_type":"airline","url":"https://avianca.example/v"}]}]}
				""", MediaType.APPLICATION_JSON));

		assertEquals("https://avianca.example/v", proveedor.obtenerEnlaceReserva("ign-1"));
	}

	@Test
	@DisplayName("Enlace de reserva: si no hay de aerolínea usa el primero disponible")
	void enlaceSinAerolinea() {
		servidor.expect(requestTo(BASE + "/fares/booking-links"))
			.andRespond(withSuccess("""
				{"booking_options":[{"links":[
				  {"provider_name":"Agencia","provider_type":"ota","url":"https://ota.example/v"}]}]}
				""", MediaType.APPLICATION_JSON));

		assertEquals("https://ota.example/v", proveedor.obtenerEnlaceReserva("ign-1"));
	}

	@Test
	@DisplayName("Enlace de reserva: 404 o error devuelven null")
	void enlaceNoEncontrado() {
		servidor.expect(requestTo(BASE + "/fares/booking-links")).andRespond(withStatus(HttpStatus.NOT_FOUND));
		servidor.expect(requestTo(BASE + "/fares/booking-links")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertNull(proveedor.obtenerEnlaceReserva("ign-x"));
		assertNull(proveedor.obtenerEnlaceReserva("ign-y"));
	}
}
