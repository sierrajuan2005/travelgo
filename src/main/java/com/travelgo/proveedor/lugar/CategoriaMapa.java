package com.travelgo.proveedor.lugar;

/** Categorías del mapa. Los hoteles no salen de Geoapify sino de Xotelo (con precio de referencia). */
public enum CategoriaMapa {
    RESTAURANTE("catering.restaurant", "Restaurantes"),
    CAFE("catering.cafe", "Cafés"),
    COMIDA_RAPIDA("catering.fast_food", "Comida rápida"),
    BAR("catering.bar", "Bares"),
    PANADERIA("commercial.food_and_drink.bakery", "Panaderías"),
    HELADERIA("catering.ice_cream", "Heladerías"),
    HOTEL("accommodation.hotel", "Hoteles"),
    HOSTAL("accommodation.hostel", "Hostales"),
    CENTRO_COMERCIAL("commercial.shopping_mall", "Centros comerciales"),
    SUPERMERCADO("commercial.supermarket", "Supermercados"),
    MERCADO("commercial.marketplace", "Mercados locales"),
    SITIO_TURISTICO("tourism.sights", "Sitios turísticos"),
    MUSEO("entertainment.museum", "Museos"),
    MONUMENTO("tourism.sights.memorial", "Monumentos"),
    MIRADOR("tourism.attraction.viewpoint", "Miradores"),
    PARQUE("leisure.park", "Parques"),
    PLAYA("beach", "Playas"),
    IGLESIA("religion.place_of_worship.christianity", "Iglesias"),
    HISTORICO("tourism.sights.archaeological_site,tourism.sights.castle,tourism.sights.ruines", "Sitios históricos"),
    PARQUE_TEMATICO("entertainment.theme_park", "Parques temáticos"),
    TEATRO("entertainment.culture.theatre", "Teatros"),
    CINE("entertainment.cinema", "Cines"),
    AEROPUERTO("airport", "Aeropuertos"),
    TERMINAL("public_transport.bus", "Terminales"),
    TREN("public_transport.train", "Estaciones de tren"),
    HOSPITAL("healthcare.hospital", "Hospitales"),
    FARMACIA("healthcare.pharmacy", "Farmacias"),
    CAJERO("service.financial.atm", "Cajeros automáticos"),
    POLICIA("service.police", "Estaciones de policía");

    private final String categoriaGeoapify;
    private final String etiqueta;

    CategoriaMapa(String categoriaGeoapify, String etiqueta) {
        this.categoriaGeoapify = categoriaGeoapify;
        this.etiqueta = etiqueta;
    }

    /** Categoría que entiende Geoapify; null si la categoría no se consulta allí. */
    public String categoriaGeoapify() { return categoriaGeoapify; }

    /** Prefiere categorías específicas frente a la categoría turística general. */
    public static CategoriaMapa clasificar(CategoriaMapa solicitada, java.util.List<String> reales) {
        if (solicitada != SITIO_TURISTICO || reales == null) return solicitada;
        for (CategoriaMapa c : values()) {
            if (c == SITIO_TURISTICO || c == HOTEL || c.categoriaGeoapify == null) continue;
            for (String prefijo : c.categoriaGeoapify.split(","))
                if (reales.stream().anyMatch(v -> v.equals(prefijo) || v.startsWith(prefijo + "."))) return c;
        }
        return solicitada;
    }

    public String etiqueta() { return etiqueta; }
}