package com.travelgo.proveedor.hotel;

import com.travelgo.exception.ProveedorNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Casos de fallo de Xotelo (Sprint 4, Integrante 4): cuota agotada, API caída y respuesta vacía. */
class XoteloProveedorHotelesFallosTest {

	private static final String TARIFAS = """
		{"error":null,"result":{"currency":"USD","rates":[{"code":"Agoda","name":"Agoda.com","rate":100,"tax":null}]}}
		""";

	private final LocalDate entrada = LocalDate.now().plusDays(30);
	private final LocalDate salida = entrada.plusDays(2);

	private MockRestServiceServer servidor;
	private XoteloProveedorHoteles proveedor;

	@BeforeEach
	void preparar() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://data.xotelo.com/api");
		servidor = MockRestServiceServer.bindTo(builder).build();
		proveedor = new XoteloProveedorHoteles(builder.build());
	}

	@Test
	@DisplayName("Cuota agotada (429) lanza ProveedorNoDisponibleException")
	void cuotaAgotada() {
		servidor.expect(requestTo(containsString("/rates"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThrows(ProveedorNoDisponibleException.class,
			() -> proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2));
	}

	@Test
	@DisplayName("API caída (500) lanza ProveedorNoDisponibleException")
	void apiCaida() {
		servidor.expect(requestTo(containsString("/rates"))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThrows(ProveedorNoDisponibleException.class,
			() -> proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2));
	}

	@Test
	@DisplayName("Un fallo no se guarda en la caché: el siguiente intento vuelve a llamar a la API")
	void falloNoSeCachea() {
		servidor.expect(requestTo(containsString("/rates"))).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
		servidor.expect(requestTo(containsString("/rates")))
			.andRespond(withSuccess(TARIFAS, MediaType.APPLICATION_JSON));

		assertThrows(ProveedorNoDisponibleException.class,
			() -> proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2));
		assertEquals("Agoda.com",
			proveedor.consultarTarifa("g1-d1", entrada, salida, 1, 2).orElseThrow().mejorTarifa().agencia());
		servidor.verify();
	}

	@Test
	@DisplayName("Una búsqueda de hoteles con la API caída lanza ProveedorNoDisponibleException")
	void listaConApiCaida() {
		servidor.expect(requestTo(containsString("/list"))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		assertThrows(ProveedorNoDisponibleException.class, () -> proveedor.buscarHoteles("g297476", 5));
	}

	@Test
	@DisplayName("Clave de ciudad vacía lanza IllegalArgumentException sin llamar a la API")
	void ciudadVacia() {
		assertThrows(IllegalArgumentException.class, () -> proveedor.buscarHoteles(" ", 5));
	}
}
