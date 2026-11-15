package com.travelgo.proveedor.destinos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la caché que cuida las cuotas de las APIs.
 */
class CacheConVencimientoTest {

	/** Reloj que se puede adelantar a mano. */
	private static final class RelojManual extends Clock {
		private Instant ahora = Instant.parse("2026-11-06T12:00:00Z");

		void adelantar(Duration tiempo) {
			ahora = ahora.plus(tiempo);
		}

		@Override
		public Instant instant() {
			return ahora;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zona) {
			return this;
		}
	}

	@Test
	@DisplayName("Dentro del tiempo de vida no vuelve a llamar a la API")
	void usaElValorGuardado() {
		CacheConVencimiento<String, String> cache = new CacheConVencimiento<>(Duration.ofHours(1), new RelojManual());
		AtomicInteger llamadas = new AtomicInteger();

		cache.obtener("cusco", () -> "dato " + llamadas.incrementAndGet());
		String segundo = cache.obtener("cusco", () -> "dato " + llamadas.incrementAndGet());

		assertEquals("dato 1", segundo);
		assertEquals(1, llamadas.get());
	}

	@Test
	@DisplayName("Cuando vence, vuelve a consultar")
	void vence() {
		RelojManual reloj = new RelojManual();
		CacheConVencimiento<String, String> cache = new CacheConVencimiento<>(Duration.ofHours(1), reloj);
		AtomicInteger llamadas = new AtomicInteger();

		cache.obtener("cusco", () -> "dato " + llamadas.incrementAndGet());
		reloj.adelantar(Duration.ofMinutes(61));
		String despues = cache.obtener("cusco", () -> "dato " + llamadas.incrementAndGet());

		assertEquals("dato 2", despues);
		assertEquals(2, llamadas.get());
	}

	@Test
	@DisplayName("Un error de la API no se guarda: la siguiente vez se intenta de nuevo")
	void noGuardaErrores() {
		CacheConVencimiento<String, String> cache = new CacheConVencimiento<>(Duration.ofHours(1), new RelojManual());

		assertThrows(ProveedorExternoException.class, () -> cache.obtener("cusco", () -> {
			throw new ProveedorExternoException("Open-Meteo", "sin conexión", null);
		}));

		assertTrue(cache.buscar("cusco").isEmpty());
		assertEquals("ok", cache.obtener("cusco", () -> "ok"));
	}

	@Test
	@DisplayName("La duración debe ser mayor que cero")
	void duracionInvalida() {
		assertThrows(IllegalArgumentException.class, () -> new CacheConVencimiento<String, String>(Duration.ZERO));
	}
}
