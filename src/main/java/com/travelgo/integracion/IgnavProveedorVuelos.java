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

@Service
public class IgnavProveedorVuelos implements ProveedorVuelos {

    private final RestClient restClient;
    private final String apiKey;
    private final String market;

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

        IgnavSolicitud solicitud = new IgnavSolicitud(
                origen.trim().toUpperCase(),
                destino.trim().toUpperCase(),
                fechaSalida.toString(),
                market,
                codigoAerolinea == null || codigoAerolinea.isBlank()
                        ? null
                        : codigoAerolinea.trim().toUpperCase()
        );

        IgnavRespuesta respuesta = restClient.post()
                .uri("/fares/one-way")
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .body(IgnavRespuesta.class);

        if (respuesta == null || respuesta.itineraries() == null) {
            return List.of();
        }

        return respuesta.itineraries().stream()
                .map(itinerario -> convertirVuelo(itinerario, respuesta))
                .filter(vuelo -> vuelo != null)
                .toList();
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
                || segmento.arrival_time_local() == null) {
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

        if (respuesta == null || respuesta.booking_options() == null) {
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
        return null;
    }
}
}