package com.travelgo.integracion;

import com.travelgo.model.Vuelo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.List;

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

        // La conversión de itinerarios a objetos Vuelo
        // se implementará en el siguiente paso.
        return List.of();
    }

    @Override
    public String obtenerEnlaceReserva(String idExterno) {
        return null;
    }
}