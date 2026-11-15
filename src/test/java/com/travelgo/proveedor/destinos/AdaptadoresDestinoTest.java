package com.travelgo.proveedor.destinos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las partes de los adaptadores que no llaman a internet. Las pruebas con respuestas simuladas
 * de cada API (MockRestServiceServer) son de Integrante 4.
 */
class AdaptadoresDestinoTest {

	@Test
	@DisplayName("Wikipedia: saca el nombre del archivo de una URL normal")
	void nombreArchivoNormal() {
		assertEquals("Cartagena_skyline,_Colombia.jpg", WikipediaFotos.nombreArchivo(
			"https://upload.wikimedia.org/wikipedia/commons/c/cf/Cartagena_skyline%2C_Colombia.jpg?utm_source=x"));
	}

	@Test
	@DisplayName("Wikipedia: saca el nombre del archivo de una URL de miniatura")
	void nombreArchivoMiniatura() {
		assertEquals("Cancun_Strand_Luftbild_(22143397586).jpg", WikipediaFotos.nombreArchivo(
			"https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d8/Cancun_Strand_Luftbild_%2822143397586%29.jpg/"
				+ "3840px-Cancun_Strand_Luftbild_%2822143397586%29.jpg?utm_source=es.wikipedia.org"));
	}

	@Test
	@DisplayName("Wikipedia: deja los lugares turísticos y quita batallas, barrios y colegios")
	void filtraLugaresTuristicos() {
		assertTrue(WikipediaAtracciones.esTuristico("Coricancha", "templo inca y sitio arqueológico en Perú"));
		assertTrue(WikipediaAtracciones.esTuristico("Casa Yabar Peralta", "sitio de patrimonio cultural en Perú"));
		assertTrue(WikipediaAtracciones.esTuristico("Castillo San Felipe de Barajas", null));
		assertFalse(WikipediaAtracciones.esTuristico("Batalla de Cartagena de Indias (1586)", null));
		assertFalse(WikipediaAtracciones.esTuristico("Sitio de Cartagena (1815)", "asedio"));
		assertFalse(WikipediaAtracciones.esTuristico("Distrito de Wánchaq", "distrito en Cusco, Perú"));
		assertFalse(WikipediaAtracciones.esTuristico("Hospital Antonio Lorena", "hospital en Cusco, Perú"));
	}

	@Test
	@DisplayName("Distancia al centro en metros o en kilómetros")
	void distancia() {
		Coordenadas centro = new Coordenadas(-13.53188, -71.96701);
		assertEquals("A 0 m del centro", WikipediaAtracciones.distanciaTexto(centro, centro));
		assertTrue(WikipediaAtracciones.distanciaTexto(centro, new Coordenadas(-13.52, -71.95)).endsWith("km del centro"));
	}

	@Test
	@DisplayName("Open-Meteo: traduce los códigos WMO del clima")
	void codigosClima() {
		assertEquals("Despejado", OpenMeteoClima.descripcion(0));
		assertEquals("Lluvia", OpenMeteoClima.descripcion(63));
		assertEquals("Tormenta", OpenMeteoClima.descripcion(95));
		assertEquals("Sin dato", OpenMeteoClima.descripcion(-1));
		assertEquals("bi-cloud-rain", OpenMeteoClima.icono(61));
	}

	@Test
	@DisplayName("OpenTripMap: toma la primera categoría conocida")
	void categoriaOpenTripMap() {
		assertEquals("Museo", OpenTripMapAtracciones.categoria("museums,cultural,interesting_places"));
		assertEquals("Fortificación", OpenTripMapAtracciones.categoria("fortifications,historic,interesting_places"));
		assertNull(OpenTripMapAtracciones.categoria("other"));
	}

	@Test
	@DisplayName("Las coordenadas fuera de rango no se aceptan")
	void coordenadasInvalidas() {
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new Coordenadas(91, 0));
		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new Coordenadas(0, 181));
	}
}
