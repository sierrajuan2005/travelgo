package com.travelgo.estado;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guarda en memoria las últimas llamadas reales a cada API y decide su semáforo.
 * También recuerda qué APIs tiene "caídas" el administrador (simulación desde el panel).
 *
 * En memoria a propósito: es un panel en vivo para el stand, no un historial; al reiniciar empieza de cero.
 *
 * Semáforo:
 * - SIMULADA: el administrador simuló la caída.
 * - SIN_DATOS: nadie la ha usado desde que arrancó la app.
 * - ROJO: la última llamada falló.
 * - AMARILLO: la última salió bien, pero fue lenta (más de {@value #LENTA_MS} ms), hubo fallas entre las
 *   últimas {@value #VENTANA_SEMAFORO}, o la última vino vacía en una API que esconde sus errores (Ignav).
 * - VERDE: todo bien.
 * Las respuestas de caché (CACHE) no cuentan para la latencia ni para el semáforo: no fueron a internet.
 */
@Component
public class RegistroLlamadas {

    static final int GUARDADAS = 50;
    static final int VENTANA_SEMAFORO = 10;
    static final long LENTA_MS = 2500;
    static final int EN_GRAFICO = 20;

    private final Map<String, Deque<Llamada>> llamadas = new ConcurrentHashMap<>();
    // {llamadas, errores, msSumados de las que fueron a internet, cuántas fueron a internet}
    private final Map<String, long[]> totales = new ConcurrentHashMap<>();
    private final Set<String> caidas = ConcurrentHashMap.newKeySet();
    private final Clock reloj;

    public RegistroLlamadas() {
        this(Clock.systemUTC());
    }

    RegistroLlamadas(Clock reloj) {
        this.reloj = reloj;
    }

    Instant ahora() {
        return reloj.instant();
    }

    public void registrar(String fuenteId, Llamada llamada) {
        Deque<Llamada> cola = llamadas.computeIfAbsent(fuenteId, k -> new ArrayDeque<>());
        synchronized (cola) {
            cola.addFirst(llamada);
            while (cola.size() > GUARDADAS) {
                cola.removeLast();
            }
        }
        long[] t = totales.computeIfAbsent(fuenteId, k -> new long[4]);
        synchronized (t) {
            t[0]++;
            if (llamada.resultado() == Llamada.Resultado.ERROR || llamada.resultado() == Llamada.Resultado.SIMULADA) {
                t[1]++;
            }
            if (llamada.resultado() != Llamada.Resultado.CACHE) {
                t[2] += llamada.ms();
                t[3]++;
            }
        }
    }

    public boolean estaCaida(String fuenteId) {
        return caidas.contains(fuenteId);
    }

    public void simularCaida(String fuenteId, boolean caida) {
        if (caida) {
            caidas.add(fuenteId);
        } else {
            caidas.remove(fuenteId);
        }
    }

    /** "Sin internet": cae (o vuelve) todas las APIs a la vez. */
    public void simularSinInternet(Collection<String> activas, boolean sinInternet) {
        activas.forEach(id -> simularCaida(id, sinInternet));
    }

    public boolean sinInternet(Collection<String> activas) {
        return !activas.isEmpty() && caidas.containsAll(activas);
    }

    /** Estado de las APIs que usa la app, en el orden del panel. */
    public List<EstadoFuente> estados(Collection<String> activas) {
        return FuenteMonitoreada.TODAS.stream().filter(f -> activas.contains(f.id())).map(this::estado).toList();
    }

    EstadoFuente estado(FuenteMonitoreada f) {
        List<Llamada> recientes;
        Deque<Llamada> cola = llamadas.get(f.id());
        if (cola == null) {
            recientes = List.of();
        } else {
            synchronized (cola) {
                recientes = new ArrayList<>(cola);
            }
        }
        long[] t = totales.getOrDefault(f.id(), new long[4]);
        long total, errores, msSumados, reales;
        synchronized (t) {
            total = t[0];
            errores = t[1];
            msSumados = t[2];
            reales = t[3];
        }

        Llamada ultima = recientes.isEmpty() ? null : recientes.get(0);
        Llamada ultimaReal = recientes.stream().filter(l -> l.resultado() != Llamada.Resultado.CACHE)
                .findFirst().orElse(null);
        Long haceSegundos = ultima == null ? null : Duration.between(ultima.fin(), ahora()).toSeconds();
        String ultimoError = recientes.stream()
                .filter(l -> l.resultado() != Llamada.Resultado.OK && l.detalle() != null)
                .map(Llamada::detalle).findFirst().orElse(null);
        List<EstadoFuente.Punto> grafico = recientes.stream().limit(EN_GRAFICO)
                .map(l -> new EstadoFuente.Punto(l.ms(), l.resultado().name())).toList().reversed();
        int exito = recientes.isEmpty() ? 0 : (int) Math.round(100.0 * recientes.stream()
                .filter(l -> l.resultado() == Llamada.Resultado.OK || l.resultado() == Llamada.Resultado.VACIA
                        || l.resultado() == Llamada.Resultado.CACHE)
                .count() / recientes.size());

        return new EstadoFuente(f.id(), f.nombre(), f.paraQueSirve(), f.siFalla(), f.cuota(), f.sePuedeProbar(),
                semaforo(f, recientes), estaCaida(f.id()), total, errores,
                reales == 0 ? null : msSumados / reales, ultimaReal == null ? null : ultimaReal.ms(), haceSegundos,
                exito, ultimoError, grafico);
    }

    String semaforo(FuenteMonitoreada f, List<Llamada> recientes) {
        if (estaCaida(f.id())) {
            return EstadoFuente.SIMULADA;
        }
        if (recientes.isEmpty()) {
            return EstadoFuente.SIN_DATOS;
        }
        recientes = recientes.stream().filter(l -> l.resultado() != Llamada.Resultado.CACHE).toList();
        if (recientes.isEmpty()) {
            return EstadoFuente.VERDE; // solo respondió desde su caché: no hay señales de problema
        }
        Llamada ultima = recientes.get(0);
        if (ultima.resultado() == Llamada.Resultado.ERROR || ultima.resultado() == Llamada.Resultado.SIMULADA) {
            return EstadoFuente.ROJO;
        }
        boolean fallasRecientes = recientes.stream().limit(VENTANA_SEMAFORO).anyMatch(l ->
                l.resultado() == Llamada.Resultado.ERROR || l.resultado() == Llamada.Resultado.SIMULADA);
        boolean vaciaSospechosa = ultima.resultado() == Llamada.Resultado.VACIA
                && f.forma() == FuenteMonitoreada.FormaDeFallar.RESPUESTA_VACIA;
        if (ultima.ms() > LENTA_MS || fallasRecientes || vaciaSospechosa) {
            return EstadoFuente.AMARILLO;
        }
        return EstadoFuente.VERDE;
    }
}
