package com.travelgo.proveedor.destinos;

/**
 * Foto real de un destino con los datos de atribución que exige su licencia.
 * Siempre que se muestra la foto hay que mostrar el autor, la licencia y el enlace a la fuente.
 *
 * @param url            dirección de la imagen (tamaño mediano, apta para la web)
 * @param titulo         nombre del archivo o de la página de la foto
 * @param autor          autor de la foto, sin etiquetas HTML; "Autor desconocido" si la fuente no lo dice
 * @param licencia       nombre corto de la licencia (por ejemplo "CC BY-SA 4.0"); puede ser null
 * @param enlaceLicencia texto legal de la licencia; puede ser null
 * @param enlaceFuente   página de la foto en la fuente (Wikimedia Commons o Wikipedia)
 */
public record FotoDestino(String url, String titulo, String autor, String licencia, String enlaceLicencia,
		String enlaceFuente) {
}
