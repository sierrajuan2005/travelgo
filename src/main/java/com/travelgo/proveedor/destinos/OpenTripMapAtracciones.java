package com.travelgo.proveedor.destinos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Adaptador de OpenTripMap: atracciones reales con nombre, dirección y coordenadas (datos de OpenStreetMap
 * y Wikidata). Necesita una llave gratuita en la variable de entorno {@code OPENTRIPMAP_API_KEY}.
 * <ol>
 *   <li>{@code /0.1/es/places/radius}: lugares cerca del centro, ordenados por popularidad.</li>
 *   <li>{@code /0.1/es/places/xid/{xid}}: dirección de cada lugar (una llamada por lugar).</li>
 * </ol>
 */
public class OpenTripMapAtracciones implements ProveedorAtracciones {

	static final String URL_BASE = "https://api.opentripmap.com";

	private static final Logger LOG = LoggerFactory.getLogger(OpenTripMapAtracciones.class);

	private static final int RADIO_METROS = 10_000;

	/** Nombres en español de las categorías ("kinds") más comunes de OpenTripMap. */
	private static final Map<String, String> CATEGORIAS = new LinkedHashMap<>();

	static {
		CATEGORIAS.put("museums", "Museo");
		CATEGORIAS.put("fortifications", "Fortificación");
		CATEGORIAS.put("churches", "Iglesia");
		CATEGORIAS.put("cathedrals", "Catedral");
		CATEGORIAS.put("religion", "Lugar religioso");
		CATEGORIAS.put("archaeology", "Sitio arqueológico");
		CATEGORIAS.put("monuments_and_memorials", "Monumento");
		CATEGORIAS.put("beaches", "Playa");
		CATEGORIAS.put("view_points", "Mirador");
		CATEGORIAS.put("natural", "Naturaleza");
		CATEGORIAS.put("theatres_and_entertainments", "Teatro");
		CATEGORIAS.put("historic_architecture", "Arquitectura histórica");
		CATEGORIAS.put("architecture", "Arquitectura");
		CATEGORIAS.put("historic", "Sitio histórico");
		CATEGORIAS.put("cultural", "Cultura");
	}

	private final RestClient http;
	private final String llave;

	/**
	 * @param http  builder de RestClient con los tiempos de espera ya configurados
	 * @param llave llave de la API (viene de una variable de entorno, nunca del repositorio)
	 * @throws IllegalArgumentException si la llave está vacía
	 */
	public OpenTripMapAtracciones(RestClient.Builder http, String llave) {
		if (llave == null || llave.isBlank()) {
			throw new IllegalArgumentException("OpenTripMap necesita una llave (OPENTRIPMAP_API_KEY).");
		}
		this.http = http.clone().baseUrl(URL_BASE).build();
		this.llave = llave.trim();
	}

	@Override
	public List<Atraccion> buscarAtracciones(Coordenadas centro, int limite) {
		JsonNode lugares = LlamadaExterna.ejecutar(nombreFuente(), () -> http.get()
			.uri(uri -> uri.path("/0.1/es/places/radius")
				.queryParam("radius", RADIO_METROS)
				.queryParam("lat", centro.latitud())
				.queryParam("lon", centro.longitud())
				.queryParam("kinds", "interesting_places")
				.queryParam("rate", 2)
				.queryParam("format", "json")
				.queryParam("limit", Math.max(limite * 2, 10)) // algunos vienen sin nombre y se descartan
				.queryParam("apikey", "{llave}")
				.build(llave))
			.retrieve()
			.body(JsonNode.class));

		List<Atraccion> atracciones = new ArrayList<>();
		if (lugares == null || !lugares.isArray()) {
			return atracciones;
		}
		for (JsonNode lugar : lugares) {
			if (atracciones.size() >= limite) {
				break;
			}
			String nombre = lugar.path("name").asString("");
			if (nombre.isBlank()) {
				continue;
			}
			String xid = lugar.path("xid").asString("");
			JsonNode punto = lugar.path("point");
			atracciones.add(new Atraccion(nombre.trim(), categoria(lugar.path("kinds").asString("")),
				direccion(xid), new Coordenadas(punto.path("lat").asDouble(), punto.path("lon").asDouble()),
				"https://opentripmap.com/es/card/" + xid));
		}
		return atracciones;
	}

	/**
	 * Pide el detalle del lugar para sacar la dirección. Si esta llamada falla, la atracción se muestra
	 * igual, sin dirección: no vale la pena perder toda la lista por un dato.
	 */
	private String direccion(String xid) {
		if (xid.isBlank()) {
			return null;
		}
		try {
			JsonNode detalle = LlamadaExterna.ejecutar(nombreFuente(), () -> http.get()
				.uri(uri -> uri.path("/0.1/es/places/xid/{xid}")
					.queryParam("apikey", "{llave}")
					.build(xid, llave))
				.retrieve()
				.body(JsonNode.class));
			return detalle == null ? null : armarDireccion(detalle.path("address"));
		} catch (ProveedorExternoException e) {
			LOG.warn("No se pudo leer la dirección del lugar {} en OpenTripMap: {}", xid, e.getMessage());
			return null;
		}
	}

	/** Une calle, número, barrio y ciudad: "Calle 30 #17-10, Getsemaní, Cartagena". */
	static String armarDireccion(JsonNode direccion) {
		StringJoiner partes = new StringJoiner(", ");
		String calle = (direccion.path("road").asString("") + " " + direccion.path("house_number").asString("")).trim();
		for (String parte : new String[] {calle, direccion.path("suburb").asString(""),
			direccion.path("city").asString(direccion.path("town").asString(""))}) {
			if (!parte.isBlank()) {
				partes.add(parte.trim());
			}
		}
		return partes.length() == 0 ? null : partes.toString();
	}

	/** Toma la primera categoría conocida de la lista "kinds" (separada por comas). */
	static String categoria(String kinds) {
		List<String> lista = List.of(kinds.split(","));
		return CATEGORIAS.entrySet().stream()
			.filter(c -> lista.contains(c.getKey()))
			.map(Map.Entry::getValue)
			.findFirst()
			.orElse(null);
	}

	@Override
	public String nombreFuente() {
		return "OpenTripMap";
	}

	@Override
	public String atribucion() {
		return "Lugares de OpenTripMap · datos © colaboradores de OpenStreetMap (ODbL) y Wikidata";
	}

	@Override
	public String enlaceFuente() {
		return "https://opentripmap.com/";
	}
}
