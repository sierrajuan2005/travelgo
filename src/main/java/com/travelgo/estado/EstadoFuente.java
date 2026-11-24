package com.travelgo.estado;

import java.util.List;

/**
 * Lo que muestra el panel de una API (también es el JSON que el panel pide cada pocos segundos).
 *
 * @param semaforo        VERDE, AMARILLO, ROJO, SIN_DATOS o SIMULADA
 * @param msPromedio      promedio de todas las llamadas desde que arrancó la app; null si no hay
 * @param msUltima        lo que tardó la última llamada; null si no hay
 * @param haceSegundos    hace cuánto fue la última llamada; null si no hay
 * @param exito           % de llamadas que respondieron (con o sin datos) entre las últimas guardadas
 * @param grafico         últimas llamadas, de la más vieja a la más nueva, para el gráfico de barras
 */
public record EstadoFuente(String id, String nombre, String paraQueSirve, String siFalla, String cuota,
                           boolean sePuedeProbar, String semaforo, boolean caidaSimulada, long llamadas,
                           long errores, Long msPromedio, Long msUltima, Long haceSegundos, int exito,
                           String ultimoError, List<Punto> grafico) {

    public static final String VERDE = "VERDE";
    public static final String AMARILLO = "AMARILLO";
    public static final String ROJO = "ROJO";
    public static final String SIN_DATOS = "SIN_DATOS";
    public static final String SIMULADA = "SIMULADA";

    /** Una barra del gráfico: cuánto tardó y cómo terminó (OK, VACIA, ERROR, SIMULADA). */
    public record Punto(long ms, String resultado) {
    }
}
