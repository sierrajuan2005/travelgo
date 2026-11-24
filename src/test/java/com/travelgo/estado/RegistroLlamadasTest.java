package com.travelgo.estado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Pruebas de las reglas del semáforo del panel de estado. */
class RegistroLlamadasTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T15:00:00Z");
    private static final Set<String> CLIMA = Set.of("open-meteo-clima");

    private final RegistroLlamadas registro = new RegistroLlamadas(Clock.fixed(AHORA, ZoneOffset.UTC));

    private void llamar(long ms, Llamada.Resultado resultado, int haceSegundos) {
        registro.registrar("open-meteo-clima",
                new Llamada(AHORA.minusSeconds(haceSegundos), ms, resultado,
                        resultado == Llamada.Resultado.OK || resultado == Llamada.Resultado.CACHE ? null : "problema"));
    }

    private EstadoFuente estado() {
        return registro.estados(CLIMA).get(0);
    }

    @Test
    @DisplayName("Sin llamadas: SIN_DATOS")
    void sinDatos() {
        assertThat(estado().semaforo()).isEqualTo(EstadoFuente.SIN_DATOS);
        assertThat(estado().msUltima()).isNull();
    }

    @Test
    @DisplayName("Rápida y sin fallas: VERDE, con promedio, éxito y hace cuánto")
    void verde() {
        llamar(200, Llamada.Resultado.OK, 30);
        llamar(400, Llamada.Resultado.OK, 10);

        EstadoFuente e = estado();
        assertThat(e.semaforo()).isEqualTo(EstadoFuente.VERDE);
        assertThat(e.msPromedio()).isEqualTo(300);
        assertThat(e.msUltima()).isEqualTo(400);
        assertThat(e.haceSegundos()).isEqualTo(10);
        assertThat(e.exito()).isEqualTo(100);
        assertThat(e.grafico()).extracting(EstadoFuente.Punto::ms).containsExactly(200L, 400L);
    }

    @Test
    @DisplayName("Lenta o con una falla reciente: AMARILLO")
    void amarillo() {
        llamar(RegistroLlamadas.LENTA_MS + 1, Llamada.Resultado.OK, 5);
        assertThat(estado().semaforo()).isEqualTo(EstadoFuente.AMARILLO);

        llamar(100, Llamada.Resultado.ERROR, 4);
        llamar(100, Llamada.Resultado.OK, 3);
        assertThat(estado().semaforo()).isEqualTo(EstadoFuente.AMARILLO);
        assertThat(estado().ultimoError()).isEqualTo("problema");
    }

    @Test
    @DisplayName("Si la última falló: ROJO")
    void rojo() {
        llamar(100, Llamada.Resultado.OK, 5);
        llamar(5000, Llamada.Resultado.ERROR, 1);
        assertThat(estado().semaforo()).isEqualTo(EstadoFuente.ROJO);
        assertThat(estado().exito()).isEqualTo(50);
    }

    @Test
    @DisplayName("Vuelve a VERDE cuando las fallas salen de la ventana de las últimas 10")
    void seRecupera() {
        llamar(100, Llamada.Resultado.ERROR, 60);
        for (int i = 0; i < RegistroLlamadas.VENTANA_SEMAFORO; i++) {
            llamar(100, Llamada.Resultado.OK, 50 - i);
        }
        assertThat(estado().semaforo()).isEqualTo(EstadoFuente.VERDE);
    }

    @Test
    @DisplayName("Solo guarda las últimas 50 llamadas, pero cuenta todas")
    void guardaLasUltimas() {
        for (int i = 0; i < RegistroLlamadas.GUARDADAS + 5; i++) {
            llamar(100, Llamada.Resultado.OK, 1);
        }
        assertThat(estado().llamadas()).isEqualTo(RegistroLlamadas.GUARDADAS + 5);
        assertThat(estado().grafico()).hasSize(RegistroLlamadas.EN_GRAFICO);
    }

    @Test
    @DisplayName("Las respuestas de caché no cuentan para la latencia ni para el semáforo")
    void cacheNoCuenta() {
        llamar(800, Llamada.Resultado.OK, 20);
        llamar(1, Llamada.Resultado.CACHE, 10);
        llamar(2, Llamada.Resultado.CACHE, 5);

        EstadoFuente e = estado();
        assertThat(e.semaforo()).isEqualTo(EstadoFuente.VERDE);
        assertThat(e.msUltima()).isEqualTo(800);
        assertThat(e.msPromedio()).isEqualTo(800);
        assertThat(e.haceSegundos()).isEqualTo(5);
        assertThat(e.llamadas()).isEqualTo(3);
    }

    @Test
    @DisplayName("El panel muestra solo las APIs que la app usa")
    void soloActivas() {
        assertThat(registro.estados(Set.of("open-meteo-clima", "ignav"))).extracting(EstadoFuente::id)
                .containsExactly("open-meteo-clima", "ignav");
        assertThat(registro.estados(Set.of())).isEmpty();
    }
}
