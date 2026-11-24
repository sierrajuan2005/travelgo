package com.travelgo.proveedor.lugar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de Geoapify Places. La llave vive solo en el backend (nunca llega al navegador) y los
 * resultados se guardan 12 horas en memoria para cuidar la cuota gratuita.
 */
@Component
public class GeoapifyProveedorLugares implements ProveedorLugares {

    private static final String FUENTE = "Geoapify · OpenStreetMap";
    private static final Duration VIGENCIA = Duration.ofHours(12);

    private final RestClient cliente;
    private final String llave;
    private final Map<String, EntradaCache> cache = new ConcurrentHashMap<>();

    @Autowired
    public GeoapifyProveedorLugares(
            @Value("${travelgo.proveedores.geoapify.url:https://api.geoapify.com/v2}") String urlBase,
            @Value("${travelgo.proveedores.geoapify.api-key:}") String llave) {
        this(construirCliente(urlBase), llave);
    }

    /** Constructor para pruebas. */
    GeoapifyProveedorLugares(RestClient cliente, String llave) {
        this.cliente = cliente;
        this.llave = llave;
    }

    private static RestClient construirCliente(String urlBase) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public List<LugarMapa> buscar(CategoriaMapa categoria, double latitud, double longitud,
                                  int radioMetros, int limite) {
        if (categoria == null || categoria.categoriaGeoapify() == null) {
            throw new IllegalArgumentException("Esta categoría no se consulta en Geoapify.");
        }
        if (llave == null || llave.isBlank()) {
            throw new ProveedorNoDisponibleException(
                    "Falta la llave de Geoapify: define la variable de entorno GEOAPIFY_API_KEY.");
        }
        int radio = Math.max(100, Math.min(radioMetros, 20000));
        int tope = Math.max(1, Math.min(limite, 50));

        String clave = String.format(Locale.ROOT, "%s|%.4f|%.4f|%d|%d", categoria, latitud, longitud, radio, tope);
        EntradaCache guardada = cache.get(clave);
        if (guardada != null && guardada.vigente()) {
            return guardada.lugares();
        }
        List<LugarMapa> lugares = consultar(categoria, latitud, longitud, radio, tope);
        cache.put(clave, new EntradaCache(lugares, Instant.now()));
        return lugares;
    }

    private List<LugarMapa> consultar(CategoriaMapa categoria, double latitud, double longitud,
                                      int radio, int tope) {
        try {
            Respuesta respuesta = cliente.get().uri(uri -> uri.path("/places")
                    .queryParam("categories", categoria.categoriaGeoapify())
                    .queryParam("filter", String.format(Locale.ROOT, "circle:%.6f,%.6f,%d", longitud, latitud, radio))
                    .queryParam("bias", String.format(Locale.ROOT, "proximity:%.6f,%.6f", longitud, latitud))
                    .queryParam("limit", tope)
                    .queryParam("lang", "es")
                    .queryParam("apiKey", llave)
                    .build()).retrieve().body(Respuesta.class);

            if (respuesta == null || respuesta.features() == null) {
                return List.of();
            }
            return respuesta.features().stream()
                    .map(Feature::properties)
                    .filter(p -> p != null && p.name() != null && !p.name().isBlank()
                            && p.lat() != null && p.lon() != null)
                    .map(p -> new LugarMapa(
                            p.placeId() != null ? p.placeId() : categoria + "-" + p.lat() + "-" + p.lon(),
                            p.name(), CategoriaMapa.clasificar(categoria, p.categories()), p.lat(), p.lon(), p.formatted(), p.website(), null, FUENTE))
                    .toList();
        } catch (RestClientException e) {
            // Sin e.getMessage(): podría traer la URL con la llave y llegar al navegador.
            throw new ProveedorNoDisponibleException(
                    "No se pudo consultar Geoapify (revisa la llave y el límite diario).", e);
        }
    }

    private record EntradaCache(List<LugarMapa> lugares, Instant guardadoEn) {
        boolean vigente() {
            return Instant.now().isBefore(guardadoEn.plus(VIGENCIA));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(List<Feature> features) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Feature(Propiedades properties) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Propiedades(String name, String formatted, @JsonProperty("place_id") String placeId,
                       Double lat, Double lon, String website, List<String> categories) { }
}