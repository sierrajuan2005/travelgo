package com.travelgo.proveedor.hotel;

import com.travelgo.model.Hotel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Clasifica un hotel en económico, medio o lujo según su precio por noche en pesos.
 * Hasta el corte económico es económico; desde el corte de lujo es lujo; entre los dos, medio.
 */
@Component
public class ClasificadorGama {

    private final double economicoMaxCop;
    private final double lujoMinCop;

    public ClasificadorGama(@Value("${travelgo.gama.economico-max-cop:400000}") double economicoMaxCop,
                            @Value("${travelgo.gama.lujo-min-cop:1000000}") double lujoMinCop) {
        if (economicoMaxCop <= 0 || lujoMinCop <= economicoMaxCop) {
            throw new IllegalArgumentException("Los cortes de gama deben cumplir: 0 < económico < lujo.");
        }
        this.economicoMaxCop = economicoMaxCop;
        this.lujoMinCop = lujoMinCop;
    }

    public GamaHotel clasificar(double precioPorNocheCop) {
        if (!Double.isFinite(precioPorNocheCop) || precioPorNocheCop <= 0) {
            throw new IllegalArgumentException("El precio por noche debe ser mayor a cero.");
        }
        if (precioPorNocheCop <= economicoMaxCop) {
            return GamaHotel.ECONOMICO;
        }
        if (precioPorNocheCop >= lujoMinCop) {
            return GamaHotel.LUJO;
        }
        return GamaHotel.MEDIO;
    }

    /** Gama de un hotel guardado; null si no tiene un precio con el que clasificarlo. */
    public GamaHotel gamaDe(Hotel hotel) {
        return hotel == null || hotel.getPrecio() <= 0 ? null : clasificar(hotel.getPrecio());
    }
}