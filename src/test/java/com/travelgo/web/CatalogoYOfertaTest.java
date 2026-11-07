package com.travelgo.web;

import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los filtros del catálogo, de la oferta de servicios de un paquete y de los textos de la web.
 */
class CatalogoYOfertaTest {

	private static PaqueteTuristico paquete(long id, String nombre, String precio, int dias, String ciudad, String pais) {
		Destino destino = new Destino(ciudad, "", pais, ciudad);
		destino.setId(id);
		PaqueteTuristico paquete = new PaqueteTuristico(nombre, "", new BigDecimal(precio), dias);
		paquete.setId(id);
		paquete.setDestinos(List.of(destino));
		return paquete;
	}

	private final List<PaqueteTuristico> paquetes = List.of(
		paquete(1, "Cartagena Esencial", "1200000", 4, "Cartagena", "Colombia"),
		paquete(2, "Bogotá Cultural", "800000", 3, "Bogotá", "Colombia"),
		paquete(3, "Machu Picchu Aventura", "3500000", 7, "Cusco", "Perú"));

	@Test
	@DisplayName("Filtra por destino sin importar tildes ni mayúsculas")
	void filtraPorDestino() {
		assertEquals(1, CatalogoWebController.filtrar(paquetes, "PERU", null, null, null).size());
		assertEquals(1, CatalogoWebController.filtrar(paquetes, "bogota", null, null, null).size());
		assertEquals(2, CatalogoWebController.filtrar(paquetes, "colombia", null, null, null).size());
	}

	@Test
	@DisplayName("Filtra por precio y por duración")
	void filtraPorPrecioYDuracion() {
		assertEquals(2, CatalogoWebController.filtrar(paquetes, null, null, new BigDecimal("1200000"), null).size());
		assertEquals(1, CatalogoWebController.filtrar(paquetes, null, new BigDecimal("2000000"), null, null).size());
		assertEquals(2, CatalogoWebController.filtrar(paquetes, null, null, null, 4).size());
	}

	@Test
	@DisplayName("La oferta del paquete solo trae los servicios de su ciudad")
	void ofertaPorCiudad() {
		LocalDateTime salida = LocalDateTime.of(2026, 12, 10, 8, 0);
		List<ServicioReservable> servicios = List.of(
			new Vuelo("v1", 450000, true, "Avianca", "Bogotá", "Cartagena", salida, salida.plusHours(2)),
			new Vuelo("v2", 520000, true, "LATAM", "Bogotá", "Cusco", salida, salida.plusHours(4)),
			new Hotel("h1", 200000, true, "Hotel Caribe", "Bocagrande, Cartagena", "Doble", 3),
			new Hotel("h2", 300000, true, "Hotel Andino", "Centro histórico, Cusco", "Suite", 2),
			new Transporte("t1", 80000, true, "Bus", "Cartagena", "Barranquilla", salida));

		OfertaPaquete oferta = OfertaPaquete.de(paquetes.get(0), servicios);

		assertEquals(List.of("v1"), oferta.vuelos().stream().map(Vuelo::getId).toList());
		assertEquals(List.of("h1"), oferta.hoteles().stream().map(Hotel::getId).toList());
		assertEquals(List.of("t1"), oferta.transportes().stream().map(Transporte::getId).toList());
		assertTrue(oferta.buscar("v2").isEmpty());
		assertTrue(oferta.buscar("h1").isPresent());
	}

	@Test
	@DisplayName("Precios en pesos colombianos y ids cortos")
	void textos() {
		FormatoWeb formato = new FormatoWeb();
		assertEquals("$ 1.200.000", formato.cop(new BigDecimal("1200000.00")));
		assertEquals("$ 0", formato.cop(null));
		assertEquals("r-vacia", FormatoWeb.idCorto("r-vacia"));
		assertEquals("123e4567", FormatoWeb.idCorto("123e4567-e89b-12d3-a456-426614174000"));
		assertEquals("Cartagena, Colombia", formato.destinos(paquetes.get(0)));
	}
}
