package com.travelgo.integracion;


import java.util.List;


public record IgnavRespuesta(
    String origin,
    String destination,
    String departure_date,
    String observed_at,
    List<Itinerario> itineraries
) {
     public record Itinerario(
            Precio price,
            Trayecto outbound,
            String ignav_id
    ) {}

    public record Precio(
            double amount,
            String currency,
            String status
    ) {}

    public record Trayecto(
            String carrier,
            int duration_minutes,
            List<Segmento> segments
    ) {}

    public record Segmento(
            String marketing_carrier_code,
            String flight_number,
            String operating_carrier_name,
            String departure_airport,
            String departure_time_local,
            String arrival_airport,
            String arrival_time_local,
            int duration_minutes
    ) {}
}
