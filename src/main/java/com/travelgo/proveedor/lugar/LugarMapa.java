package com.travelgo.proveedor.lugar;

/** Un lugar para dibujar en el mapa, venga de la fuente que venga. */
public record LugarMapa(String id, String nombre, CategoriaMapa categoria, double latitud, double longitud,
                        String direccion, String enlace, Double calificacion, String fuente) {
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2),2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2),2);
        return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1,a)));
    }
}