package com.travelgo.estado;

import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.destinos.Clima;
import com.travelgo.proveedor.destinos.Coordenadas;
import com.travelgo.proveedor.destinos.ProveedorClima;
import com.travelgo.proveedor.destinos.ProveedorExternoException;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import com.travelgo.proveedor.moneda.TasaCambio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas del panel de estado con adaptadores falsos que se llaman como los reales (mismo nombre de clase),
 * así el monitor los reconoce. Nunca se llama a una API de verdad.
 */
class MonitorProveedoresTest {

    private RegistroLlamadas registro;
    private MonitorProveedores monitor;

    @BeforeEach
    void preparar() {
        registro = new RegistroLlamadas(Clock.fixed(Instant.parse("2026-12-01T15:00:00Z"), ZoneOffset.UTC));
        DefaultListableBeanFactory fabrica = new DefaultListableBeanFactory();
        fabrica.registerSingleton("registro", registro);
        ObjectProvider<RegistroLlamadas> proveedor = fabrica.getBeanProvider(RegistroLlamadas.class);
        monitor = new MonitorProveedores(proveedor);
    }

    @SuppressWarnings("unchecked")
    private <T> T envolver(Object adaptador) {
        return (T) monitor.postProcessAfterInitialization(adaptador, "adaptador");
    }

    @Test
    @DisplayName("Mide las consultas reales y no los datos de atribución")
    void mideLasConsultas() {
        ProveedorClima clima = envolver(new OpenMeteoClima(false));

        clima.consultarClima(new Coordenadas(10.46, -73.25));
        clima.nombreFuente();
        clima.atribucion();

        EstadoFuente estado = registro.estados(Set.of("open-meteo-clima")).get(0);
        assertThat(estado.llamadas()).isEqualTo(1);
        assertThat(estado.semaforo()).isEqualTo(EstadoFuente.VERDE);
        assertThat(monitor.activas()).containsExactly("open-meteo-clima");
    }

    @Test
    @DisplayName("Una excepción queda registrada como falla y sigue su camino")
    void registraLasFallas() {
        ProveedorClima clima = envolver(new OpenMeteoClima(true));

        assertThatThrownBy(() -> clima.consultarClima(new Coordenadas(10.46, -73.25)))
                .isInstanceOf(ProveedorExternoException.class);

        EstadoFuente estado = registro.estados(Set.of("open-meteo-clima")).get(0);
        assertThat(estado.semaforo()).isEqualTo(EstadoFuente.ROJO);
        assertThat(estado.ultimoError()).contains("sin conexión");
        assertThat(estado.errores()).isEqualTo(1);
    }

    @Test
    @DisplayName("La caída simulada falla como la API real: ProveedorExternoException en los destinos")
    void caidaSimuladaDestinos() {
        OpenMeteoClima real = new OpenMeteoClima(false);
        ProveedorClima clima = envolver(real);
        registro.simularCaida("open-meteo-clima", true);

        assertThatThrownBy(() -> clima.consultarClima(new Coordenadas(10.46, -73.25)))
                .isInstanceOf(ProveedorExternoException.class)
                .hasMessageContaining("simulado");
        assertThat(real.llamadas).isZero(); // nunca llegó a la API
        assertThat(registro.estados(Set.of("open-meteo-clima")).get(0).semaforo()).isEqualTo(EstadoFuente.SIMULADA);

        registro.simularCaida("open-meteo-clima", false);
        clima.consultarClima(new Coordenadas(10.46, -73.25));
        assertThat(real.llamadas).isEqualTo(1);
    }

    @Test
    @DisplayName("La caída simulada de la tasa de cambio lanza ProveedorNoDisponibleException")
    void caidaSimuladaProveedor() {
        ProveedorTasaCambio tasa = envolver(new OpenErApiProveedorTasaCambio());
        registro.simularCaida("tasa-cambio", true);

        assertThatThrownBy(tasa::obtenerUsdACop).isInstanceOf(ProveedorNoDisponibleException.class);
    }

    @Test
    @DisplayName("Ignav esconde sus errores: sin resultados queda en amarillo, y su caída devuelve vacío")
    void ignavRespuestaVacia() {
        ProveedorVuelos vuelos = envolver(new IgnavProveedorVuelos());

        assertThat(vuelos.buscarVuelos("VUP", "BOG", LocalDate.of(2026, 12, 20), null)).isEmpty();
        EstadoFuente estado = registro.estados(Set.of("ignav")).get(0);
        assertThat(estado.semaforo()).isEqualTo(EstadoFuente.AMARILLO);
        assertThat(estado.ultimoError()).contains("no informa sus errores");
        assertThat(estado.sePuedeProbar()).isFalse();

        registro.simularCaida("ignav", true);
        assertThat(vuelos.buscarVuelos("VUP", "BOG", LocalDate.of(2026, 12, 20), null)).isEmpty();
        assertThat(vuelos.obtenerEnlaceReserva("x")).isNull();
    }

    @Test
    @DisplayName("En un adaptador con caché propia, una respuesta casi instantánea cuenta como caché")
    void respuestaDeCache() {
        ProveedorVuelos vuelos = envolver(new IgnavDesdeSuCache());
        vuelos.obtenerEnlaceReserva("x");

        EstadoFuente estado = registro.estados(Set.of("ignav")).get(0);
        assertThat(estado.grafico()).extracting(EstadoFuente.Punto::resultado).containsExactly("CACHE");
        assertThat(estado.msUltima()).isNull();
        assertThat(estado.semaforo()).isEqualTo(EstadoFuente.VERDE);
    }

    @Test
    @DisplayName("Una clase que no es de una API externa no se envuelve")
    void otrasClasesNoSeEnvuelven() {
        Object cualquiera = new Object();
        assertThat(monitor.postProcessAfterInitialization(cualquiera, "x")).isSameAs(cualquiera);
    }

    @Test
    @DisplayName("Simular sin internet cae todas las APIs activas a la vez")
    void sinInternet() {
        Set<String> activas = Set.of("open-meteo-clima", "ignav");
        registro.simularSinInternet(activas, true);
        assertThat(registro.sinInternet(activas)).isTrue();

        registro.simularSinInternet(activas, false);
        assertThat(registro.sinInternet(activas)).isFalse();
    }

    /* ---------- adaptadores falsos con el mismo nombre de clase que los reales ---------- */

    static class OpenMeteoClima implements ProveedorClima {
        private final boolean falla;
        int llamadas;

        OpenMeteoClima(boolean falla) {
            this.falla = falla;
        }

        @Override
        public Clima consultarClima(Coordenadas lugar) {
            llamadas++;
            if (falla) {
                throw new ProveedorExternoException("Open-Meteo", "Open-Meteo no respondió: sin conexión.", null);
            }
            return new Clima(30, "Despejado", "bi-sun", List.of());
        }

        @Override
        public String nombreFuente() {
            return "Open-Meteo";
        }

        @Override
        public String atribucion() {
            return "Open-Meteo.com";
        }

        @Override
        public String enlaceFuente() {
            return "https://open-meteo.com";
        }
    }

    /** Mismo nombre que el adaptador de Ignav (caché propia), pero responde al instante: como su caché. */
    static class IgnavDesdeSuCache extends IgnavProveedorVuelos {
    }

    static class OpenErApiProveedorTasaCambio implements ProveedorTasaCambio {
        @Override
        public TasaCambio obtenerUsdACop() {
            throw new AssertionError("no debería llegar a la API");
        }
    }

    static class IgnavProveedorVuelos implements ProveedorVuelos {
        @Override
        public List<Vuelo> buscarVuelos(String origen, String destino, LocalDate fechaSalida, String codigoAerolinea) {
            esperar(MonitorProveedores.UMBRAL_CACHE_MS + 5); // tarda como una consulta real, no como su caché
            return List.of(); // así responde Ignav cuando falla
        }

        private static void esperar(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public String obtenerEnlaceReserva(String idExterno) {
            return "https://ignav.com/x";
        }
    }
}
