package com.travelgo.proveedor.hotel;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Contrato de cualquier fuente de hoteles reales. Cambiar de API es crear otro adaptador. */
public interface ProveedorHoteles {

    /** Ocupación fija con la que se consultan los precios (se guarda junto a cada oferta). */
    int HABITACIONES_POR_DEFECTO = 1;
    int ADULTOS_POR_DEFECTO = 2;

    /** Hoteles reales de una ciudad (por la clave de ciudad que usa la fuente). */
    List<HotelResumen> buscarHoteles(String ciudadClave, int limite);

    /** Precio de referencia de un hotel para unas fechas; vacío si la fuente no tiene tarifas. */
    Optional<OfertaHotel> consultarTarifa(String hotelClave, LocalDate entrada, LocalDate salida,
                                          int habitaciones, int adultos);
}