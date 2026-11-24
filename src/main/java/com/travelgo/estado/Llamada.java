package com.travelgo.estado;

import java.time.Instant;

/**
 * Una llamada real a una API externa, medida por {@link MonitorProveedores}.
 *
 * @param fin       cuándo terminó
 * @param ms        cuánto tardó
 * @param resultado cómo terminó
 * @param detalle   mensaje del error (o del motivo de la respuesta vacía); null si salió bien
 */
public record Llamada(Instant fin, long ms, Resultado resultado, String detalle) {

    public enum Resultado {
        /** Respondió con datos. */
        OK,
        /** Respondió sin datos (lista vacía, nada encontrado). En Ignav es la única señal de que falló. */
        VACIA,
        /** Lanzó una excepción (sin conexión, cuota agotada, llave inválida, respuesta rara...). */
        ERROR,
        /** Falló porque el administrador simuló la caída desde el panel. */
        SIMULADA,
        /**
         * Respondió en menos de {@value MonitorProveedores#UMBRAL_CACHE_MS} ms un adaptador con caché propia
         * (Xotelo, Geoapify, Ignav): salió de su caché, no de internet. No cuenta para la latencia ni el semáforo.
         */
        CACHE
    }
}
