package com.travelgo.chatbot;

import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.ClasificadorGama;
import com.travelgo.proveedor.hotel.GamaHotel;
import com.travelgo.service.PaqueteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Pruebas del buscador del chatbot: "el más barato" y la gama de los hoteles del paquete. */
@ExtendWith(MockitoExtension.class)
class BuscadorPaquetesBasicoTest {

	@Mock
	private PaqueteService paqueteService;
	@Mock
	private PaqueteServicioDAO paqueteServicioDAO;
	@Mock
	private ClasificadorGama clasificador;

	private BuscadorPaquetesBasico buscador;

	@BeforeEach
	void preparar() {
		buscador = new BuscadorPaquetesBasico(paqueteService, paqueteServicioDAO, clasificador);
	}

	private PaqueteTuristico paquete(Long id, String nombre, String precio) {
		PaqueteTuristico p = mock(PaqueteTuristico.class);
		lenient().when(p.getId()).thenReturn(id);
		lenient().when(p.getNombre()).thenReturn(nombre);
		lenient().when(p.getPrecio()).thenReturn(new BigDecimal(precio));
		return p;
	}

	@Test
	@DisplayName("Los más baratos son el tercio de menor precio, en orden")
	void masBaratos() {
		List<PaqueteTuristico> seis = List.of(paquete(1L, "A", "600"), paquete(2L, "B", "100"),
			paquete(3L, "C", "500"), paquete(4L, "D", "200"), paquete(5L, "E", "300"), paquete(6L, "F", "400"));
		when(paqueteService.buscar("Bogotá", null, null, null, null)).thenReturn(seis);

		List<PaqueteTuristico> baratos = buscador.masBaratos("Bogotá");

		assertEquals(List.of("B", "D"), baratos.stream().map(PaqueteTuristico::getNombre).toList());
	}

	@Test
	@DisplayName("Con un solo paquete, ese es el más barato")
	void unSoloPaquete() {
		when(paqueteService.buscar(null, null, null, null, null)).thenReturn(List.of(paquete(1L, "A", "100")));

		assertEquals(1, buscador.masBaratos(null).size());
	}

	@Test
	@DisplayName("Sin paquetes, devuelve lista vacía")
	void sinPaquetes() {
		when(paqueteService.buscar("Tokio", null, null, null, null)).thenReturn(List.of());

		assertTrue(buscador.masBaratos("Tokio").isEmpty());
	}

	@Test
	@DisplayName("Por gama: solo los paquetes con un hotel de esa gama, del más barato al más caro")
	void porGama() {
		PaqueteTuristico premium = paquete(1L, "Premium", "4500000");
		PaqueteTuristico basico = paquete(2L, "Basico", "900000");
		PaqueteTuristico lujoBarato = paquete(3L, "LujoCompacto", "3000000");
		Hotel hotelLujo = mock(Hotel.class);
		Hotel hotelLujo2 = mock(Hotel.class);
		Hotel hotelEconomico = mock(Hotel.class);
		when(clasificador.gamaDe(hotelLujo)).thenReturn(GamaHotel.LUJO);
		when(clasificador.gamaDe(hotelLujo2)).thenReturn(GamaHotel.LUJO);
		when(clasificador.gamaDe(hotelEconomico)).thenReturn(GamaHotel.ECONOMICO);
		when(paqueteServicioDAO.listarServicios(1L)).thenReturn(List.of(mock(Vuelo.class), hotelLujo));
		when(paqueteServicioDAO.listarServicios(2L)).thenReturn(List.of(hotelEconomico));
		when(paqueteServicioDAO.listarServicios(3L)).thenReturn(List.of(hotelLujo2));
		when(paqueteService.buscar("Cartagena", null, null, null, null)).thenReturn(List.of(premium, basico, lujoBarato));

		Optional<List<PaqueteTuristico>> lujo = buscador.porGama("Cartagena", Gama.LUJO);

		assertEquals(List.of("LujoCompacto", "Premium"),
			lujo.orElseThrow().stream().map(PaqueteTuristico::getNombre).toList());
	}

	@Test
	@DisplayName("Un paquete sin hoteles o con hotel sin precio no cuenta para ninguna gama")
	void sinHotelDeLaGama() {
		PaqueteTuristico soloVuelo = paquete(1L, "SoloVuelo", "500000");
		PaqueteTuristico hotelSinPrecio = paquete(2L, "SinPrecio", "700000");
		Hotel sinPrecio = mock(Hotel.class);
		when(clasificador.gamaDe(sinPrecio)).thenReturn(null);
		when(paqueteServicioDAO.listarServicios(1L)).thenReturn(List.of(mock(Vuelo.class)));
		when(paqueteServicioDAO.listarServicios(2L)).thenReturn(List.of(sinPrecio));
		when(paqueteService.buscar(null, null, null, null, null)).thenReturn(List.of(soloVuelo, hotelSinPrecio));

		assertTrue(buscador.porGama(null, Gama.LUJO).orElseThrow().isEmpty());
	}

	@Test
	@DisplayName("Una gama sin definir devuelve una lista vacía, no un error")
	void gamaNula() {
		assertTrue(buscador.porGama("Bogotá", null).orElseThrow().isEmpty());
	}
}
