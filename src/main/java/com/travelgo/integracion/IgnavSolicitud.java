package com.travelgo.integracion;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record IgnavSolicitud(
        String origin,
        String destination,
        String departure_date,
        String market,
        String airlines_include
) {
}