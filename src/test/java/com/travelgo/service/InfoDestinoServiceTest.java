package com.travelgo.service;

import com.travelgo.model.Destino;
import com.travelgo.proveedor.destinos.Atraccion;
import com.travelgo.proveedor.destinos.Clima;
import com.travelgo.proveedor.destinos.Coordenadas;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.proveedor.destinos.Geocodificador;
import com.travelgo.proveedor.destinos.InfoDestino;
import com.travelgo.proveedor.destinos.ProveedorAtracciones;
import com.travelgo.proveedor.destinos.ProveedorClima;
import com.travelgo.proveedor.destinos.ProveedorExternoException;
import com.travelgo.proveedor.destinos.ProveedorFotos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de InfoDestinoService con proveedores simulados: no se llama a internet.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InfoDestinoServiceTest {

	@Mock
	private DestinoService destinoService;
	@Mock
	private Geocodificador geocodificador;
	@Mock
	private ProveedorAtracciones proveedorAtracciones;
	@Mock
	private ProveedorClima proveedorClima;
	@Mock
	private ProveedorFotos proveedorFotos;

	private InfoDestinoService servicio;
	private Destino cusco;
	private final Coordenadas centroCusco = new Coordenadas(-13.53188, -71.96701);

	@BeforeEach
	void preparar() {
		servicio = new InfoDestinoService(destinoService, geocodificador, proveedorAtracciones, proveedorClima,
			proveedorFotos);
		cusco = new Destino("Cusco y Machu Picchu", "Ruta inca", "Perú", "Cusco");
		cusco.setId(3L);
		when(destinoService.obtener(3L)).thenReturn(cusco);
		when(geocodificador.ubicar("Cusco", "Perú")).thenReturn(Optional.of(centroCusco));
		when(geocodificador.nombreFuente()).thenReturn("Open-Meteo");
		when(proveedorClima.nombreFuente()).thenReturn("Open-Meteo");
		when(proveedorAtracciones.nombreFuente()).thenReturn("Wikipedia");
		when(proveedorFotos.nombreFuente()).thenReturn("Wikimedia Commons");
		when(proveedorFotos.buscarFoto(any())).thenReturn(Optional.empty());
	}

	private static Clima climaDePrueba() {
		return new Clima(20.8, "Nublado", "bi-clouds", List.of());
	}

	@Test
	@DisplayName("Junta clima, atracciones y foto con su fuente")
	void consultaTodo() {
		when(proveedorClima.consultarClima(centroCusco)).thenReturn(climaDePrueba());
		when(proveedorAtracciones.buscarAtracciones(centroCusco, InfoDestinoService.MAX_ATRACCIONES))
			.thenReturn(List.of(new Atraccion("Coricancha", "templo inca", null, centroCusco, "https://x")));
		when(proveedorFotos.buscarFoto("Cusco"))
			.thenReturn(Optional.of(new FotoDestino("https://foto", "Plaza.jpg", "Allison Bellido", "CC BY-SA 4.0", null, "https://c")));

		InfoDestino info = servicio.consultar(3L);

		assertEquals(20.8, info.clima().datos().temperaturaActual());
		assertEquals("Open-Meteo", info.clima().fuente());
		assertNotNull(info.clima().consultadoEn());
		assertEquals("Coricancha", info.atracciones().datos().get(0).nombre());
		assertEquals("Allison Bellido", info.foto().datos().autor());
		assertTrue(info.avisos().isEmpty());
	}

	@Test
	@DisplayName("Si la API del clima falla, las atracciones se muestran igual y queda un aviso")
	void apiCaidaNoTumbaLoDemas() {
		when(proveedorClima.consultarClima(any()))
			.thenThrow(new ProveedorExternoException("Open-Meteo", "Open-Meteo no respondió a tiempo.", null));
		when(proveedorAtracciones.buscarAtracciones(any(), anyInt())).thenReturn(List.of());

		InfoDestino info = servicio.consultar(3L);

		assertNull(info.clima());
		assertNotNull(info.atracciones());
		assertEquals(1, info.avisos().size());
		assertTrue(info.avisos().get(0).contains("el clima"));
	}

	@Test
	@DisplayName("Si no se encuentra la ciudad, no se pide clima ni atracciones")
	void ciudadNoEncontrada() {
		when(geocodificador.ubicar("Cusco", "Perú")).thenReturn(Optional.empty());

		InfoDestino info = servicio.consultar(3L);

		assertNull(info.ubicacion());
		assertNull(info.clima());
		assertNull(info.atracciones());
		assertEquals(1, info.avisos().size());
		verify(proveedorClima, never()).consultarClima(any());
		assertThrows(ProveedorExternoException.class, () -> servicio.clima(cusco));
	}

	@Test
	@DisplayName("La segunda consulta sale de la caché: no se gasta cuota")
	void usaCache() {
		when(proveedorClima.consultarClima(centroCusco)).thenReturn(climaDePrueba());

		servicio.clima(cusco);
		servicio.clima(cusco);

		verify(proveedorClima, times(1)).consultarClima(centroCusco);
		verify(geocodificador, times(1)).ubicar("Cusco", "Perú");
	}

	@Test
	@DisplayName("La foto se busca por el nombre del destino y, si no hay, por la ciudad")
	void fotoPorNombreYLuegoCiudad() {
		when(proveedorFotos.buscarFoto("Cusco"))
			.thenReturn(Optional.of(new FotoDestino("https://foto", "Plaza.jpg", "Autor", null, null, "https://c")));

		assertTrue(servicio.foto(cusco).isPresent());
		verify(proveedorFotos).buscarFoto("Cusco y Machu Picchu");
		verify(proveedorFotos).buscarFoto("Cusco");
	}

	@Test
	@DisplayName("Para las tarjetas del catálogo, un error de la API de fotos da vacío")
	void fotoSiSePuede() {
		when(proveedorFotos.buscarFoto(any()))
			.thenThrow(new ProveedorExternoException("Wikimedia Commons", "sin conexión", null));

		assertTrue(servicio.fotoSiSePuede(cusco).isEmpty());
	}
}
