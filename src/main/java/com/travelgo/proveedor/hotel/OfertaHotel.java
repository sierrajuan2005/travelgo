package com.travelgo.proveedor.hotel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Precio de referencia de un hotel para unas fechas y una ocupación concretas.
 * No es una reserva ni una oferta garantizada: el precio puede cambiar en la agencia.
 */
public record OfertaHotel(String hotelClave, TarifaAgencia mejorTarifa, List<TarifaAgencia> otrasTarifas,
                          String moneda, LocalDate entrada, LocalDate salida, int noches,
                          int habitaciones, int adultos, Instant fechaConsulta, String fuente) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Costo estimado de toda la estadía con la mejor tarifa (por habitación y por noche). */
    public double costoTotalEstimado() {
        return mejorTarifa.precioPorNoche() * noches * habitaciones;
    }

    /** Texto estándar para mostrar junto al precio. */
    public String descripcionReferencia() {
        return String.format(
                "Precio de referencia de %s para %s a %s (%d noche(s), %d habitación(es), %d adulto(s)), "
                        + "consultado el %s en %s. Impuestos no especificados.",
                mejorTarifa.agencia(), entrada.format(FORMATO), salida.format(FORMATO),
                noches, habitaciones, adultos,
                fechaConsulta.atZone(ZoneId.of("America/Bogota")).toLocalDate().format(FORMATO), fuente);
    }
}