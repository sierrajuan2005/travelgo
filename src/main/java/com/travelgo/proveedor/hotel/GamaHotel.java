package com.travelgo.proveedor.hotel;

/** Gama de un hotel, calculada con su precio por noche (la fuente no informa las estrellas). */
public enum GamaHotel {
    ECONOMICO("Económico"), MEDIO("Medio"), LUJO("Lujo");

    private final String etiqueta;

    GamaHotel(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() { return etiqueta; }
}