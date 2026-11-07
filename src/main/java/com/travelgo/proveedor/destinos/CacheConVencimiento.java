package com.travelgo.proveedor.destinos;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caché en memoria donde cada valor vence después de un tiempo. Sirve para cuidar las cuotas gratuitas de
 * las APIs: si alguien vuelve a ver el mismo destino, se usa lo que ya se consultó.
 * Los errores no se guardan: si la API falla, la siguiente visita lo vuelve a intentar.
 *
 * @param <K> tipo de la clave
 * @param <V> tipo del valor
 */
public class CacheConVencimiento<K, V> {

	private record Entrada<V>(V valor, Instant vence) {
	}

	private final Map<K, Entrada<V>> entradas = new ConcurrentHashMap<>();
	private final Duration duracion;
	private final Clock reloj;

	/**
	 * @param duracion cuánto tiempo vale cada valor guardado
	 */
	public CacheConVencimiento(Duration duracion) {
		this(duracion, Clock.systemUTC());
	}

	/**
	 * Constructor para las pruebas: permite usar un reloj fijo.
	 */
	public CacheConVencimiento(Duration duracion, Clock reloj) {
		if (duracion == null || duracion.isNegative() || duracion.isZero()) {
			throw new IllegalArgumentException("La duración de la caché debe ser mayor que cero.");
		}
		this.duracion = duracion;
		this.reloj = reloj;
	}

	/**
	 * Devuelve el valor guardado si no ha vencido; si no hay, lo calcula con {@code cargar} y lo guarda.
	 * Si {@code cargar} lanza una excepción, no se guarda nada y la excepción sigue su camino.
	 */
	public V obtener(K clave, Supplier<V> cargar) {
		Optional<V> guardado = buscar(clave);
		if (guardado.isPresent()) {
			return guardado.get();
		}
		V valor = cargar.get();
		entradas.put(clave, new Entrada<>(valor, reloj.instant().plus(duracion)));
		return valor;
	}

	/**
	 * Busca un valor sin calcularlo.
	 */
	public Optional<V> buscar(K clave) {
		Entrada<V> entrada = entradas.get(clave);
		if (entrada == null) {
			return Optional.empty();
		}
		if (!reloj.instant().isBefore(entrada.vence())) {
			entradas.remove(clave);
			return Optional.empty();
		}
		return Optional.of(entrada.valor());
	}

	/** Borra todo lo guardado. */
	public void limpiar() {
		entradas.clear();
	}
}
