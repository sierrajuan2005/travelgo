package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Pruebas de la búsqueda y filtros de PaqueteService (Sprint 4, Integrante 4). Sin base de datos. */
@ExtendWith(MockitoExtension.class)
class PaqueteServiceBusquedaTest {

	@Mock
	private PaqueteDAO paqueteDAO;
	@Mock
	private DestinoDAO destinoDAO;
	@Mock
	private PaqueteServicioDAO paqueteServicioDAO;

	private PaqueteService service;

	@BeforeEach
	void preparar() {
    service = new PaqueteService(paqueteDAO, destinoDAO, paqueteServicioDAO);

    List<PaqueteTuristico> paquetes = new ArrayList<>(List.of(
            paquete("Cartagena Esencial", "1200000", 4, "Colombia", "Cartagena"),
            paquete("Bogotá Cultural", "800000", 3, "Colombia", "Bogotá"),
            paquete("Machu Picchu Aventura", "3500000", 7, "Perú", "Cusco"),
            paquete("Cancún Todo Incluido", "2800000", 5, "México", "Cancún")
    ));

    for (int i = 0; i < paquetes.size(); i++) {
        paquetes.get(i).setId((long) i + 1);
    }

    lenient().when(paqueteDAO.listarActivos()).thenReturn(paquetes);
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
		assertEquals(4, service.buscar(null, null, null, null, null, null, null, false).size());
		assertEquals(4, service.buscar("  ", null, null, null, null, null, null, false).size());
	}

	@Test
	@DisplayName("Destino: ignora tildes y mayúsculas, y busca en ciudad y país")
	void porDestino() {
		assertEquals(List.of("Bogotá Cultural"), nombres(service.buscar("BOGOTA", null, null, null, null, null, null, false)));
		assertEquals(List.of("Machu Picchu Aventura"), nombres(service.buscar("peru", null, null, null, null, null, null, false)));
		assertEquals(2, service.buscar("colombia", null, null, null, null, null, null, false).size());
	}

	@Test
	@DisplayName("Precio: los extremos están incluidos")
	void porPrecio() {
		List<PaqueteTuristico> r = service.buscar(null, new BigDecimal("800000"), new BigDecimal("1200000"), null, null, null, null, false);
		assertEquals(List.of("Cartagena Esencial", "Bogotá Cultural"), nombres(r));
	}

	@Test
	@DisplayName("Duración: rango en días")
	void porDuracion() {
		assertEquals(List.of("Machu Picchu Aventura"), nombres(service.buscar(null, null, null, 6, null, null, null, false)));
		assertEquals(List.of("Bogotá Cultural"), nombres(service.buscar(null, null, null, null, 3, null, null, false)));
	}

	@Test
	@DisplayName("Los filtros se combinan con Y")
	void combinados() {
		assertEquals(List.of("Cartagena Esencial"),
			nombres(service.buscar("colombia", null, new BigDecimal("2000000"), 4, null, null, null, false)));
	}

	@Test
	@DisplayName("Sin coincidencias devuelve lista vacía")
	void sinResultados() {
		assertTrue(service.buscar("tokio", null, null, null, null, null, null, false).isEmpty());
	}

	@Test
	@DisplayName("Rangos al revés lanzan IllegalArgumentException")
	void rangosInvalidos() {
		assertThrows(IllegalArgumentException.class,
			() -> service.buscar(null, new BigDecimal("5"), new BigDecimal("1"), null, null, null, null, false));
		assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, 9, 2, null, null, false));
	}

	@Test
	@DisplayName("Orden ASC: paquetes de menor a mayor precio")
	void ordenarPrecioAscendente() {
    	assertEquals(
            List.of(
                    "Bogotá Cultural",
                    "Cartagena Esencial",
                    "Cancún Todo Incluido",
                    "Machu Picchu Aventura"
            ),
            nombres(service.buscar(null, null, null, null, null, null, "ASC", false))
    	);
	}

	@Test
	@DisplayName("Orden DESC: paquetes de mayor a menor precio")
	void ordenarPrecioDescendente() {
    	assertEquals(
            List.of(
                    "Machu Picchu Aventura",
                    "Cancún Todo Incluido",
                    "Cartagena Esencial",
                    "Bogotá Cultural"
            ),
            nombres(service.buscar(null, null, null, null, null, null, "DESC", false))
    	);
	}

	@Test
	@DisplayName("Orden inválido lanza IllegalArgumentException")
	void ordenInvalido() {
    	assertThrows(
            IllegalArgumentException.class,
            () -> service.buscar(null, null, null, null, null, null, "INVALIDO", false)
    	);
	}

}
