package com.travelgo.proveedor.lugar;

/** Categorías del mapa. Los hoteles no salen de Geoapify sino de Xotelo (con precio de referencia). */
public enum CategoriaMapa {
    RESTAURANTE("catering.restaurant", "Restaurantes"),
    CAFE("catering.cafe", "Cafés"),
    HOTEL(null, "Hoteles"),
    SITIO_TURISTICO("tourism.sights", "Sitios turísticos"),
    MUSEO("entertainment.museum", "Museos");

    private final String categoriaGeoapify;
    private final String etiqueta;

    CategoriaMapa(String categoriaGeoapify, String etiqueta) {
        this.categoriaGeoapify = categoriaGeoapify;
        this.etiqueta = etiqueta;
    }

    /** Categoría que entiende Geoapify; null si la categoría no se consulta allí. */
    public String categoriaGeoapify() { return categoriaGeoapify; }

    public String etiqueta() { return etiqueta; }
}