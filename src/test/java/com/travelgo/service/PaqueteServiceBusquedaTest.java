package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

/** Pruebas de la búsqueda y filtros de PaqueteService (Sprint 4, Integrante 4). Sin base de datos. */
@ExtendWith(MockitoExtension.class)
class PaqueteServiceBusquedaTest {

	@Mock
	private PaqueteDAO paqueteDAO;
	@Mock
	private DestinoDAO destinoDAO;

	private PaqueteService service;

	@BeforeEach
	void preparar() {
		service = new PaqueteService(paqueteDAO, destinoDAO);
		lenient().when(paqueteDAO.listarActivos()).thenReturn(List.of(
			paquete("Cartagena Esencial", "1200000", 4, "Colombia", "Cartagena"),
			paquete("Bogotá Cultural", "800000", 3, "Colombia", "Bogotá"),
			paquete("Machu Picchu Aventura", "3500000", 7, "Perú", "Cusco"),
			paquete("Cancún Todo Incluido", "2800000", 5, "México", "Cancún")));
	}

	private static PaqueteTuristico paquete(String nombre, String precio, int dias, String pais, String ciudad) {
		PaqueteTuristico p = new PaqueteTuristico(nombre, "desc", new BigDecimal(precio), dias);
		p.setDestinos(List.of(new Destino(ciudad, "desc", pais, ciudad)));
		return p;
	}

	private List<String> nombres(List<PaqueteTuristico> lista) {
		return lista.stream().map(PaqueteTuristico::getNombre).toList();
	}

	@Test
	@DisplayName("Sin filtros devuelve todos los paquetes activos")
	void sinFiltros() {
		assertEquals(4, service.buscar(null, null, null, null, null).size());
		assertEquals(4, service.buscar("  ", null, null, null, null).size());
	}

	@Test
	@DisplayName("Destino: ignora tildes y mayúsculas, y busca en ciudad y país")
	void porDestino() {
		assertEquals(List.of("Bogotá Cultural"), nombres(service.buscar("BOGOTA", null, null, null, null)));
		assertEquals(List.of("Machu Picchu Aventura"), nombres(service.buscar("peru", null, null, null, null)));
		assertEquals(2, service.buscar("colombia", null, null, null, null).size());
	}

	@Test
	@DisplayName("Precio: los extremos están incluidos")
	void porPrecio() {
		List<PaqueteTuristico> r = service.buscar(null, new BigDecimal("800000"), new BigDecimal("1200000"), null, null);
		assertEquals(List.of("Cartagena Esencial", "Bogotá Cultural"), nombres(r));
	}

	@Test
	@DisplayName("Duración: rango en días")
	void porDuracion() {
		assertEquals(List.of("Machu Picchu Aventura"), nombres(service.buscar(null, null, null, 6, null)));
		assertEquals(List.of("Bogotá Cultural"), nombres(service.buscar(null, null, null, null, 3)));
	}

	@Test
	@DisplayName("Los filtros se combinan con Y")
	void combinados() {
		assertEquals(List.of("Cartagena Esencial"),
			nombres(service.buscar("colombia", null, new BigDecimal("2000000"), 4, null)));
	}

	@Test
	@DisplayName("Sin coincidencias devuelve lista vacía")
	void sinResultados() {
		assertTrue(service.buscar("tokio", null, null, null, null).isEmpty());
	}

	@Test
	@DisplayName("Rangos al revés lanzan IllegalArgumentException")
	void rangosInvalidos() {
		assertThrows(IllegalArgumentException.class,
			() -> service.buscar(null, new BigDecimal("5"), new BigDecimal("1"), null, null));
		assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, 9, 2));
	}
}
