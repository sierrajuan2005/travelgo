package com.travelgo.chatbot;

import com.travelgo.util.Textos;
import org.springframework.stereotype.Component;

import java.time.Month;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Saca del mensaje del usuario los datos que piden las estrategias: destino, si quiere algo barato,
 * gama, mes, número de personas y categoría. Trabaja por palabras clave, sin inteligencia artificial.
 */
@Component
public class ExtractorDatos {

	private static final Pattern PERSONAS = Pattern.compile("\\b(\\d{1,2})\\s*(personas?|pasajeros?|viajeros?|adultos?)\\b");

	private static final List<String> BARATO = List.of("barato", "baratos", "barata", "baratas", "economico",
		"economicos", "economica", "economicas", "bajo presupuesto", "poco presupuesto", "menor precio");
	private static final List<String> LUJO = List.of("lujo", "lujoso", "lujosos", "lujosa", "5 estrellas",
		"cinco estrellas", "premium");
	private static final List<String> MEDIA = List.of("gama media", "categoria media", "gama intermedia");

	private static final Map<String, Month> MESES = new LinkedHashMap<>();
	private static final Map<String, String> CATEGORIAS = new LinkedHashMap<>();

	static {
		String[] nombres = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
			"septiembre", "octubre", "noviembre", "diciembre"};
		for (int i = 0; i < nombres.length; i++) {
			MESES.put(nombres[i], Month.of(i + 1));
		}
		CATEGORIAS.put("restaurante", "restaurantes");
		CATEGORIAS.put("cafe", "cafes");
		CATEGORIAS.put("museo", "museos");
		CATEGORIAS.put("hotel", "hoteles");
		CATEGORIAS.put("playa", "playas");
		CATEGORIAS.put("sitio turistico", "sitios turisticos");
	}

	/**
	 * @param mensaje          lo que escribió el usuario
	 * @param destinosConocidos nombres de destinos, ciudades y países guardados en la base de datos
	 */
	public DatosMensaje extraer(String mensaje, Collection<String> destinosConocidos) {
		String texto = Textos.normalizar(mensaje);
		return new DatosMensaje(texto, buscarDestino(texto, destinosConocidos), esBarato(texto), buscarGama(texto),
			buscarMes(texto), buscarPersonas(texto), buscarCategoria(texto));
	}

	private String buscarDestino(String texto, Collection<String> destinos) {
		String mejor = null;
		for (String destino : destinos) {
			String normalizado = Textos.normalizar(destino);
			if (normalizado.isEmpty()) {
				continue;
			}
			boolean aparece = Pattern.compile("\\b" + Pattern.quote(normalizado) + "\\b").matcher(texto).find();
			if (aparece && (mejor == null || normalizado.length() > Textos.normalizar(mejor).length())) {
				mejor = destino;
			}
		}
		return mejor;
	}

	private boolean esBarato(String texto) {
		return BARATO.stream().anyMatch(texto::contains);
	}

	private Gama buscarGama(String texto) {
		if (LUJO.stream().anyMatch(texto::contains)) {
			return Gama.LUJO;
		}
		if (MEDIA.stream().anyMatch(texto::contains)) {
			return Gama.MEDIO;
		}
		return null;
	}

	private Month buscarMes(String texto) {
		for (Map.Entry<String, Month> mes : MESES.entrySet()) {
			if (Pattern.compile("\\b" + mes.getKey() + "\\b").matcher(texto).find()) {
				return mes.getValue();
			}
		}
		return null;
	}

	private Integer buscarPersonas(String texto) {
		Matcher m = PERSONAS.matcher(texto);
		if (m.find()) {
			int personas = Integer.parseInt(m.group(1));
			return personas > 0 ? personas : null;
		}
		return null;
	}

	private String buscarCategoria(String texto) {
		for (Map.Entry<String, String> categoria : CATEGORIAS.entrySet()) {
			if (texto.contains(categoria.getKey())) {
				return categoria.getValue();
			}
		}
		return null;
	}
}
