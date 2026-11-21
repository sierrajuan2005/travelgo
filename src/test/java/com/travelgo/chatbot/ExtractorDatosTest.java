package com.travelgo.chatbot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Month;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del extractor de datos del mensaje (sin Spring ni base de datos). */
class ExtractorDatosTest {

	private final ExtractorDatos extractor = new ExtractorDatos();
	private final List<String> destinos = List.of("Bogotá", "Cartagena", "Colombia", "Cusco", "Perú", "Cancún");

	@Test
	@DisplayName("Reconoce el destino sin importar tildes ni mayúsculas")
	void destino() {
		assertEquals("Bogotá", extractor.extraer("Paquetes baratos para BOGOTA", destinos).destino());
		assertEquals("Cancún", extractor.extraer("quiero ir a cancun", destinos).destino());
	}

	@Test
	@DisplayName("Si no nombra un destino conocido, queda en null")
	void sinDestino() {
		assertNull(extractor.extraer("hola, buenas tardes", destinos).destino());
		assertNull(extractor.extraer("viajar a Tokio", destinos).destino());
	}

	@Test
	@DisplayName("Solo reconoce el destino como palabra completa")
	void palabraCompleta() {
		assertNull(extractor.extraer("el perulero", destinos).destino());
	}

	@Test
	@DisplayName("Detecta que pide algo barato")
	void barato() {
		assertTrue(extractor.extraer("paquetes baratos para Bogotá", destinos).barato());
		assertTrue(extractor.extraer("algo económico", destinos).barato());
		assertFalse(extractor.extraer("paquetes para Bogotá", destinos).barato());
	}

	@Test
	@DisplayName("Detecta la gama de lujo y la media")
	void gama() {
		assertEquals(Gama.LUJO, extractor.extraer("paquetes con hoteles de lujo en Cartagena", destinos).gama());
		assertEquals(Gama.MEDIO, extractor.extraer("hotel de gama media", destinos).gama());
		assertNull(extractor.extraer("paquetes para Cartagena", destinos).gama());
	}

	@Test
	@DisplayName("Detecta el mes y el número de personas")
	void mesYPersonas() {
		DatosMensaje datos = extractor.extraer("Algo para 2 personas en diciembre", destinos);
		assertEquals(Month.DECEMBER, datos.mes());
		assertEquals(2, datos.personas());
	}

	@Test
	@DisplayName("Detecta la categoría de lugar")
	void categoria() {
		assertEquals("restaurantes", extractor.extraer("restaurantes en Cusco", destinos).categoria());
		assertEquals("cafes", extractor.extraer("un café en Bogotá", destinos).categoria());
	}

	@Test
	@DisplayName("Un mensaje vacío no rompe el extractor")
	void vacio() {
		DatosMensaje datos = extractor.extraer(null, destinos);
		assertEquals("", datos.texto());
		assertFalse(datos.barato());
		assertNull(datos.destino());
	}
}
