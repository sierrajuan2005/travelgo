package com.travelgo.proveedor.moneda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.travelgo.exception.ProveedorNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Adaptador de open.er-api.com (tasas de cambio gratuitas). Guarda la tasa 6 horas para no
 * llamar a la API en cada consulta. Verifica sus términos de uso: la versión abierta suele
 * pedir que se cite la fuente.
 */
@Component
public class OpenErApiProveedorTasaCambio implements ProveedorTasaCambio {

    private static final String FUENTE = "open.er-api.com";
    private static final Duration VIGENCIA = Duration.ofHours(6);

    private final RestClient cliente;
    private volatile TasaCambio guardada;

    @Autowired
    public OpenErApiProveedorTasaCambio(
            @Value("${travelgo.proveedores.tasa-cambio.url:https://open.er-api.com/v6}") String urlBase) {
        this(construirCliente(urlBase));
    }

    /** Constructor para pruebas: recibe un cliente ya armado. */
    OpenErApiProveedorTasaCambio(RestClient cliente) {
        this.cliente = cliente;
    }

    private static RestClient construirCliente(String urlBase) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public TasaCambio obtenerUsdACop() {
        TasaCambio actual = guardada;
        if (actual != null && Instant.now().isBefore(actual.fechaConsulta().plus(VIGENCIA))) {
            return actual;
        }
        try {
            Respuesta respuesta = cliente.get().uri("/latest/USD").retrieve().body(Respuesta.class);
            Double cop = respuesta == null || respuesta.rates() == null ? null : respuesta.rates().get("COP");
            if (cop == null || cop <= 0) {
                throw new ProveedorNoDisponibleException(
                        "La fuente de tasas de cambio no devolvió el valor del peso colombiano.");
            }
            guardada = new TasaCambio(cop, FUENTE, Instant.now());
            return guardada;
        } catch (RestClientException e) {
            throw new ProveedorNoDisponibleException(
                    "No se pudo consultar la tasa de cambio: " + e.getMessage(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(String result, Map<String, Double> rates) { }
}