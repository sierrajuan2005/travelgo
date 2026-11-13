package com.travelgo.integracion;

import com.travelgo.model.Vuelo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.web.client.HttpClientErrorException;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;

@Service
public class IgnavProveedorVuelos implements ProveedorVuelos {

    private static final Logger logger =
        LoggerFactory.getLogger(IgnavProveedorVuelos.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String market;
    
    private static final Duration TIEMPO_CACHE = Duration.ofMinutes(30);

    private final ConcurrentMap<String, EntradaCache> cacheVuelos =
        new ConcurrentHashMap<>();

    private record EntradaCache(
        List<Vuelo> vuelos,
        Instant expiracion
) {}

    public IgnavProveedorVuelos(
            RestClient ignavRestClient,
            @Value("${travelgo.ignav.api-key:}") String apiKey,
            @Value("${travelgo.ignav.market:CO}") String market) {

        this.restClient = ignavRestClient;
        this.apiKey = apiKey;
        this.market = market;
    }

    @Override
    public List<Vuelo> buscarVuelos(
        String origen,
        String destino,
        LocalDate fechaSalida,
        String codigoAerolinea) {

        if (origen == null || origen.isBlank()
                || destino == null || destino.isBlank()
                || fechaSalida == null) {
            throw new IllegalArgumentException(
                    "Origen, destino y fecha de salida son obligatorios."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "La API Key de Ignav no está configurada."
            );
        }

        String origenNormalizado = origen.trim().toUpperCase();
        String destinoNormalizado = destino.trim().toUpperCase();

        String aerolineaNormalizada =
                codigoAerolinea == null || codigoAerolinea.isBlank()
                        ? ""
                        : codigoAerolinea.trim().toUpperCase();

        String clave = String.join("|",
                origenNormalizado,
                destinoNormalizado,
                fechaSalida.toString(),
                aerolineaNormalizada,
                market
        );

        EntradaCache entrada = cacheVuelos.get(clave);

        if (entrada != null) {
            if (Instant.now().isBefore(entrada.expiracion())) {
                return entrada.vuelos();
            }

            cacheVuelos.remove(clave, entrada);
        }

        IgnavSolicitud solicitud = new IgnavSolicitud(
                origenNormalizado,
                destinoNormalizado,
                fechaSalida.toString(),
                market,
                aerolineaNormalizada.isEmpty() ? null : aerolineaNormalizada
        );

                IgnavRespuesta respuesta;

        try {
            respuesta = restClient.post()
                    .uri("/fares/one-way")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .body(IgnavRespuesta.class);

        } catch (RestClientException e) {

            logger.error(
                    "Error al consultar vuelos en Ignav: {}",
                    e.getMessage()
            );

            return List.of();
        }

        if (respuesta == null || respuesta.itineraries() == null) {
            return List.of();
        }

        List<Vuelo> vuelos = respuesta.itineraries().stream()
                .map(itinerario -> convertirVuelo(itinerario, respuesta))
                .filter(vuelo -> vuelo != null)
                .toList();

        cacheVuelos.put(
            clave,
            new EntradaCache(
                    vuelos,
                    Instant.now().plus(TIEMPO_CACHE)
            )
        );

        return vuelos;
    }

    private Vuelo convertirVuelo(
            IgnavRespuesta.Itinerario itinerario,
            IgnavRespuesta respuesta) {

        if (itinerario == null
                || itinerario.price() == null
                || itinerario.outbound() == null
                || itinerario.outbound().segments() == null
                || itinerario.outbound().segments().size() != 1
                || itinerario.ignav_id() == null
                || itinerario.ignav_id().isBlank()) {
            return null;
        }

        IgnavRespuesta.Precio precio = itinerario.price();
        IgnavRespuesta.Trayecto trayecto = itinerario.outbound();

        // TravelGo todavía maneja precios únicamente en COP.
        if (!"COP".equalsIgnoreCase(precio.currency())) {
            return null;
        }

        if (!Double.isFinite(precio.amount()) || precio.amount() < 0) {
            return null;
        }

        IgnavRespuesta.Segmento segmento = trayecto.segments().get(0);

        if (segmento == null
                || segmento.departure_time_local() == null
                || segmento.arrival_time_local() == null
                || respuesta.observed_at() == null
                || trayecto.carrier() == null
                || segmento.departure_airport() == null
                || segmento.arrival_airport() == null) {

            return null;
        }

        LocalDateTime fechaSalida =
                LocalDateTime.parse(segmento.departure_time_local());

        LocalDateTime fechaLlegada =
                LocalDateTime.parse(segmento.arrival_time_local());

        LocalDateTime fechaConsulta =
                OffsetDateTime.parse(respuesta.observed_at())
                        .atZoneSameInstant(ZoneId.of("America/Bogota"))
                        .toLocalDateTime();

        boolean disponible =
                "verified".equalsIgnoreCase(precio.status());

        return new Vuelo(
                UUID.randomUUID().toString(),
                precio.amount(),
                disponible,
                trayecto.carrier(),
                segmento.departure_airport(),
                segmento.arrival_airport(),
                fechaSalida,
                fechaLlegada,
                "IGNAV",
                itinerario.ignav_id(),
                fechaConsulta,
                null
        );
        
    }
    @Override
public String obtenerEnlaceReserva(String idExterno) {

    if (idExterno == null || idExterno.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador externo del vuelo es obligatorio."
        );
    }

    if (apiKey == null || apiKey.isBlank()) {
        throw new IllegalStateException(
                "La API Key de Ignav no está configurada."
        );
    }

    
    try {
            IgnavEnlacesRespuesta respuesta = restClient.post()
                    .uri("/fares/booking-links")
                    .header("X-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("ignav_id", idExterno.trim()))
                    .retrieve()
                    .body(IgnavEnlacesRespuesta.class);

            if (respuesta == null
                    || respuesta.booking_options() == null) {
                return null;
            }

            List<IgnavEnlacesRespuesta.Enlace> enlaces =
                    respuesta.booking_options().stream()
                            .filter(opcion -> opcion.links() != null)
                            .flatMap(opcion -> opcion.links().stream())
                            .filter(enlace -> enlace.url() != null
                                    && !enlace.url().isBlank())
                            .toList();

            // Priorizar el enlace de la aerolínea.
            return enlaces.stream()
                    .filter(enlace -> "airline".equalsIgnoreCase(
                            enlace.provider_type()))
                    .map(IgnavEnlacesRespuesta.Enlace::url)
                    .findFirst()
                    .orElseGet(() -> enlaces.stream()
                            .map(IgnavEnlacesRespuesta.Enlace::url)
                            .findFirst()
                            .orElse(null));

        } catch (HttpClientErrorException.NotFound e) {

            logger.warn(
                    "Ignav no encontró enlaces para el vuelo: {}",
                    idExterno
            );

            return null;

        } catch (RestClientException e) {

            logger.error(
                    "Error al consultar enlaces de reserva en Ignav: {}",
                    e.getMessage()
            );

            return null;
        }
}
}