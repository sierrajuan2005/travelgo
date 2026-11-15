package com.travelgo.proveedor.hotel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelgo.exception.ProveedorNoDisponibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Adaptador de Xotelo: hoteles y tarifas por agencia (datos de TripAdvisor).
 * Traduce la respuesta de la API a las clases de TravelGo y guarda en caché los resultados
 * para cuidar el límite de consultas.
 */
@Component
public class XoteloProveedorHoteles implements ProveedorHoteles {

    private static final String FUENTE = "Xotelo (precios de TripAdvisor)";
    private static final String MONEDA = "USD";
    private static final Duration VIGENCIA_CACHE = Duration.ofHours(6);

    private final RestClient cliente;
    private final Map<String, EntradaCache> cache = new ConcurrentHashMap<>();

    @Autowired
    public XoteloProveedorHoteles(
            @Value("${travelgo.proveedores.xotelo.url:https://data.xotelo.com/api}") String urlBase) {
        this(construirCliente(urlBase));
    }

    /** Constructor para pruebas: recibe un cliente ya armado. */
    XoteloProveedorHoteles(RestClient cliente) {
        this.cliente = cliente;
    }

    private static RestClient construirCliente(String urlBase) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().baseUrl(urlBase).requestFactory(fabrica).build();
    }

    @Override
    public List<HotelResumen> buscarHoteles(String ciudadClave, int limite) {
        if (ciudadClave == null || ciudadClave.isBlank()) {
            throw new IllegalArgumentException("La clave de la ciudad es obligatoria.");
        }
        int tope = Math.max(1, Math.min(limite, 100));

        return cacheado("lista|" + ciudadClave + "|" + tope, () -> {
            RespuestaLista respuesta = pedir(uri -> uri.path("/list")
                    .queryParam("location_key", ciudadClave)
                    .queryParam("limit", tope)
                    .queryParam("offset", 0)
                    .build(), RespuestaLista.class);

            if (respuesta.result() == null || respuesta.result().list() == null) {
                return List.<HotelResumen>of();
            }
            return respuesta.result().list().stream().map(this::aResumen).toList();
        });
    }

    @Override
    public Optional<OfertaHotel> consultarTarifa(String hotelClave, LocalDate entrada, LocalDate salida,
                                                 int habitaciones, int adultos) {
        if (hotelClave == null || hotelClave.isBlank()) {
            throw new IllegalArgumentException("La clave del hotel es obligatoria.");
        }
        if (entrada == null || salida == null || !salida.isAfter(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la de entrada.");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede estar en el pasado.");
        }
        if (habitaciones < 1 || adultos < 1) {
            throw new IllegalArgumentException("Debe haber al menos una habitación y un adulto.");
        }

        String clave = "tarifa|" + hotelClave + "|" + entrada + "|" + salida + "|" + habitaciones + "|" + adultos;
        return cacheado(clave, () -> {
            RespuestaTarifas respuesta = pedir(uri -> uri.path("/rates")
                    .queryParam("hotel_key", hotelClave)
                    .queryParam("chk_in", entrada)
                    .queryParam("chk_out", salida)
                    .queryParam("currency", MONEDA)
                    .queryParam("rooms", habitaciones)
                    .queryParam("adults", adultos)
                    .build(), RespuestaTarifas.class);

            if (respuesta.result() == null || respuesta.result().rates() == null) {
                return Optional.<OfertaHotel>empty();
            }

            List<TarifaAgencia> tarifas = respuesta.result().rates().stream()
                    .filter(t -> t.rate() != null && t.rate() > 0 && t.name() != null)
                    .map(t -> new TarifaAgencia(t.name(), t.rate()))
                    .sorted(Comparator.comparingDouble(TarifaAgencia::precioPorNoche))
                    .toList();
            if (tarifas.isEmpty()) {
                return Optional.<OfertaHotel>empty();
            }

            String moneda = respuesta.result().currency() == null ? MONEDA : respuesta.result().currency();
            int noches = (int) ChronoUnit.DAYS.between(entrada, salida);
            return Optional.of(new OfertaHotel(hotelClave, tarifas.get(0),
                    tarifas.subList(1, tarifas.size()), moneda, entrada, salida, noches,
                    habitaciones, adultos, Instant.now(), FUENTE));
        });
    }

    private HotelResumen aResumen(HotelJson h) {
        return new HotelResumen(h.key(), h.name(), h.accommodationType(), h.image(), h.url(),
                h.geo() == null ? null : h.geo().latitude(),
                h.geo() == null ? null : h.geo().longitude(),
                h.reviewSummary() == null ? null : h.reviewSummary().rating());
    }

    /** Hace la llamada y convierte cualquier fallo de red o de la API en un error propio. */
    private <T> T pedir(Function<UriBuilder, URI> uri, Class<T> tipo) {
        try {
            T respuesta = cliente.get().uri(uri).retrieve().body(tipo);
            if (respuesta == null) {
                throw new ProveedorNoDisponibleException("Xotelo devolvió una respuesta vacía.");
            }
            return respuesta;
        } catch (RestClientException e) {
            throw new ProveedorNoDisponibleException("No se pudo consultar Xotelo: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cacheado(String clave, Supplier<T> consulta) {
        EntradaCache guardada = cache.get(clave);
        if (guardada != null && guardada.vigente()) {
            return (T) guardada.valor();
        }
        T nuevo = consulta.get();
        cache.put(clave, new EntradaCache(nuevo, Instant.now()));
        return nuevo;
    }

    private record EntradaCache(Object valor, Instant guardadoEn) {
        boolean vigente() {
            return Instant.now().isBefore(guardadoEn.plus(VIGENCIA_CACHE));
        }
    }

    // ---- Forma de las respuestas de Xotelo (solo los campos que usamos) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaTarifas(ResultadoTarifas result) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultadoTarifas(String currency, List<TarifaJson> rates) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TarifaJson(String code, String name, Double rate, Double tax) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespuestaLista(ResultadoLista result) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultadoLista(List<HotelJson> list) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelJson(String name, String key,
                     @JsonProperty("accommodation_type") String accommodationType,
                     String url, String image, GeoJson geo,
                     @JsonProperty("review_summary") ResenasJson reviewSummary) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoJson(Double latitude, Double longitude) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResenasJson(Double rating) { }
}