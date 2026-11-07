package com.travelgo.proveedor.destinos;

/**
 * Lugar real que se puede visitar en un destino (museo, iglesia, plaza, fuerte...).
 * Lo devuelve un {@link ProveedorAtracciones}; TravelGo no lo guarda en la base de datos.
 *
 * @param nombre    nombre del lugar
 * @param tipo      descripción corta del lugar (por ejemplo "templo inca y sitio arqueológico"); puede ser null
 * @param direccion dirección o ubicación aproximada; puede ser null si la fuente no la tiene
 * @param ubicacion coordenadas del lugar
 * @param enlace    página con más información del lugar en la fuente
 */
public record Atraccion(String nombre, String tipo, String direccion, Coordenadas ubicacion, String enlace) {
}
