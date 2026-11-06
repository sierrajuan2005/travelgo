package com.travelgo.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Ayudas para comparar textos que escriben personas o que vienen de APIs.
 */
public final class Textos {

	private Textos() {
	}

	/**
	 * Pasa a minúsculas y quita tildes y espacios de los extremos, para comparar "Perú" con "peru" o
	 * "Bogotá" con "BOGOTA". Un texto null da "".
	 */
	public static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		return Normalizer.normalize(texto, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT)
			.trim();
	}
}
