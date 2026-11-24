package com.travelgo.expo;

/**
 * Un destino que el visitante de la ExpoSoftware puede elegir en "Lanza tu viaje".
 *
 * @param id             clave corta que viaja en la URL y se guarda en la base de datos (por ejemplo "paris")
 * @param ciudad         nombre para mostrar
 * @param pais           país, para mostrar debajo de la ciudad
 * @param region         grupo en el selector del celular (Colombia, América, Europa...)
 * @param latitud        para dibujar la ruta en el globo
 * @param longitud       para dibujar la ruta en el globo
 * @param filtroCatalogo texto del filtro {@code /catalogo?destino=...} si TravelGo vende paquetes allí; si no, null
 * @param aeropuerto     código IATA del aeropuerto principal (CDG, CTG...), para el pase de abordar
 */
public record DestinoSonado(String id, String ciudad, String pais, String region,
                            double latitud, double longitud, String filtroCatalogo, String aeropuerto) {

    /** Todos los aviones salen de Valledupar (aeropuerto Alfonso López Pumarejo, VUP). */
    public static final double VALLEDUPAR_LATITUD = 10.4631;
    public static final double VALLEDUPAR_LONGITUD = -73.2532;
    private static final double RADIO_TIERRA_KM = 6371;

    /** true si TravelGo tiene paquetes a este destino (el celular muestra el botón "Ver paquetes"). */
    public boolean tienePaquetes() {
        return filtroCatalogo != null;
    }

    /** Distancia en línea recta (círculo máximo, fórmula del haversine) desde Valledupar, en km. */
    public long kmDesdeValledupar() {
        double f1 = Math.toRadians(VALLEDUPAR_LATITUD);
        double f2 = Math.toRadians(latitud);
        double dF = f2 - f1;
        double dL = Math.toRadians(longitud - VALLEDUPAR_LONGITUD);
        double a = Math.pow(Math.sin(dF / 2), 2) + Math.cos(f1) * Math.cos(f2) * Math.pow(Math.sin(dL / 2), 2);
        return Math.round(RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
}
