package com.travelgo.chatbot;

import java.time.Month;

/**
 * Lo que el motor entendió de un mensaje. Los datos que no aparecen quedan en null o false.
 *
 * @param texto     el mensaje sin tildes y en minúsculas, para comparar
 * @param destino   destino reconocido, tal como está guardado en la base de datos
 * @param barato    pidió algo barato o económico
 * @param gama      gama de hotel pedida (por ahora solo se reconoce lujo y media)
 * @param mes       mes que mencionó (por ejemplo, "en diciembre")
 * @param personas  número de personas que mencionó
 * @param categoria categoría de lugar que mencionó (restaurantes, cafés...)
 */
public record DatosMensaje(String texto, String destino, boolean barato, Gama gama, Month mes,
		Integer personas, String categoria) {

	/** Indica si el mensaje contiene alguna de las frases (ya sin tildes y en minúsculas). */
	public boolean contiene(String... frases) {
		for (String frase : frases) {
			if (texto.contains(frase)) {
				return true;
			}
		}
		return false;
	}
}
