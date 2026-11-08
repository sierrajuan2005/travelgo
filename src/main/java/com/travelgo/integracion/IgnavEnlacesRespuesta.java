package com.travelgo.integracion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties (ignoreUnknown = true)

public record IgnavEnlacesRespuesta(
    List<OpcionReserva> booking_options
) {
    @JsonIgnoreProperties (ignoreUnknown = true)
    public record OpcionReserva(
        List<Enlace> links
    ){}

    @JsonIgnoreProperties (ignoreUnknown = true)
    public record Enlace(
        String provider_name,
        String provider_type,
        String specificity,
        String url
    ){}
}
