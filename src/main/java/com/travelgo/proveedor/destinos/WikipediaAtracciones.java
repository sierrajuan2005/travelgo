package com.travelgo.proveedor.destinos;

import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador de respaldo para atracciones: busca artículos de Wikipedia en español cerca de un punto
 * (gratis y sin llave). Se usa cuando no hay llave de OpenTripMap.
 * <p>
 * Wikipedia también devuelve barrios, colegios o batallas, así que solo se dejan los artículos cuyo nombre o
 * descripción indica un lugar para visitar (museo, iglesia, plaza, fuerte...).
 */
public class WikipediaAtracciones implements ProveedorAtracciones {

	static final String URL_BASE = "https://es.wikipedia.org";

	/** Radio máximo que acepta la API de Wikipedia, en metros. */
	private static final int RADIO_METROS = 10_000;

	/** Palabras que indican que el artículo es un lugar turístico. */
	private static final List<String> PALABRAS_TURISTICAS = List.of(
		"museo", "iglesia", "catedral", "templo", "convento", "basilica", "capilla", "santuario",
		"plaza", "plazoleta", "parque", "jardin", "castillo", "fuerte", "fortaleza", "fortificacion", "baluarte",
		"muralla", "palacio", "teatro", "monumento", "mirador", "playa", "isla", "cerro", "volcan",
		"sitio arqueologico", "arqueolog", "patrimonio", "ruinas", "mercado", "escultura", "mural", "puente",
		"faro", "acuario", "zoologico", "casa colonial", "edificio colonial");

	/** Palabras que indican que no es un lugar para visitar, aunque tenga una de las anteriores. */
	private static final List<String> PALABRAS_EXCLUIDAS = List.of(
		"batalla", "asedio", "expedicion", "accidente", "distrito", "provincia", "municipio", "barrio",
		"universidad", "colegio", "institucion educativa", "hospital", "estacion");

	/** Comienzos de título de artículos sobre hechos históricos, no sobre lugares (por ejemplo "Sitio de Cartagena"). */
	private static final List<String> TITULOS_EXCLUIDOS = List.of("sitio de ", "toma de ", "ataque a ", "vuelo ");

	private final RestClient http;

	/**
	 * @param http builder de RestClient con los tiempos de espera y el User-Agent ya configurados
	 */
	public WikipediaAtracciones(RestClient.Builder http) {
		this.http = http.clone().baseUrl(URL_BASE).build();
	}

	@Override
	public List<Atraccion> buscarAtracciones(Coordenadas centro, int limite) {
		JsonNode respuesta = LlamadaExterna.ejecutar(nombreFuente(), () -> http.get()
			.uri(uri -> uri.path("/w/api.php")
				.queryParam("action", "query")
				.queryParam("generator", "geosearch")
				.queryParam("ggscoord", "{coordenadas}")
				.queryParam("ggsradius", RADIO_METROS)
				.queryParam("ggslimit", 50)
				.queryParam("prop", "{propiedades}")
				.queryParam("colimit", "max") // sin esto, solo trae coordenadas de 10 páginas
				.queryParam("format", "json")
				.queryParam("formatversion", 2)
				.build(centro.latitud() + "|" + centro.longitud(), "coordinates|description"))
			.retrieve()
			.body(JsonNode.class));

		JsonNode paginas = respuesta == null ? null : respuesta.path("query").path("pages");
		if (paginas == null || !paginas.isArray()) {
			return List.of();
		}
		List<Candidata> candidatas = new ArrayList<>();
		for (JsonNode pagina : paginas) {
			String titulo = pagina.path("title").asString("");
			String descripcion = LlamadaExterna.textoONulo(pagina.path("description").asString(null));
			JsonNode punto = pagina.path("coordinates").path(0);
			if (titulo.isBlank() || punto.isMissingNode() || !esTuristico(titulo, descripcion)) {
				continue;
			}
			Coordenadas ubicacion = new Coordenadas(punto.path("lat").asDouble(), punto.path("lon").asDouble());
			// "index" es el orden de cercanía que devuelve geosearch
			candidatas.add(new Candidata(pagina.path("index").asInt(Integer.MAX_VALUE), new Atraccion(
				titulo, descripcion, distanciaTexto(centro, ubicacion), ubicacion,
				URL_BASE + "/?curid=" + pagina.path("pageid").asLong())));
		}
		return candidatas.stream()
			.sorted(Comparator.comparingInt(Candidata::orden))
			.limit(Math.max(limite, 0))
			.map(Candidata::atraccion)
			.toList();
	}

	private record Candidata(int orden, Atraccion atraccion) {
	}

	static boolean esTuristico(String titulo, String descripcion) {
		String tituloNormalizado = LlamadaExterna.normalizar(titulo);
		String texto = tituloNormalizado + " " + LlamadaExterna.normalizar(descripcion);
		if (TITULOS_EXCLUIDOS.stream().anyMatch(tituloNormalizado::startsWith)
			|| PALABRAS_EXCLUIDAS.stream().anyMatch(texto::contains)) {
			return false;
		}
		return PALABRAS_TURISTICAS.stream().anyMatch(texto::contains);
	}

	/**
	 * Wikipedia no da la dirección; se muestra a qué distancia está del centro de la ciudad.
	 */
	static String distanciaTexto(Coordenadas centro, Coordenadas lugar) {
		double km = distanciaKm(centro, lugar);
		return km < 1
			? String.format(Locale.ROOT, "A %d m del centro", Math.round(km * 1000))
			: String.format(Locale.ROOT, "A %.1f km del centro", km).replace('.', ',');
	}

	/** Distancia en línea recta (fórmula de Haversine). */
	static double distanciaKm(Coordenadas a, Coordenadas b) {
		double radioTierraKm = 6371;
		double dLat = Math.toRadians(b.latitud() - a.latitud());
		double dLon = Math.toRadians(b.longitud() - a.longitud());
		double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(a.latitud())) * Math.cos(Math.toRadians(b.latitud()))
			* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * radioTierraKm * Math.asin(Math.sqrt(h));
	}

	@Override
	public String nombreFuente() {
		return "Wikipedia";
	}

	@Override
	public String atribucion() {
		return "Lugares de Wikipedia en español (CC BY-SA 4.0)";
	}

	@Override
	public String enlaceFuente() {
		return "https://es.wikipedia.org/";
	}
}
