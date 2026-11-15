package com.travelgo.proveedor.destinos;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Adaptador que busca la foto principal del artículo de Wikipedia en español de un lugar y pide a
 * Wikimedia Commons su autor y su licencia (gratis y sin llave).
 * <ol>
 *   <li>{@code es.wikipedia.org/api/rest_v1/page/summary/{lugar}}: foto principal del artículo.</li>
 *   <li>{@code commons.wikimedia.org/w/api.php?action=query&prop=imageinfo}: autor, licencia y tamaño mediano.</li>
 * </ol>
 */
public class WikipediaFotos implements ProveedorFotos {

	static final String URL_WIKIPEDIA = "https://es.wikipedia.org";
	static final String URL_COMMONS = "https://commons.wikimedia.org";

	/** Ancho en píxeles de la foto que se pide a Commons (la original puede pesar varios MB). */
	private static final int ANCHO_FOTO = 1024;

	private final RestClient wikipedia;
	private final RestClient commons;

	/**
	 * @param http builder de RestClient con los tiempos de espera y el User-Agent ya configurados
	 */
	public WikipediaFotos(RestClient.Builder http) {
		this.wikipedia = http.clone().baseUrl(URL_WIKIPEDIA).build();
		this.commons = http.clone().baseUrl(URL_COMMONS).build();
	}

	@Override
	public Optional<FotoDestino> buscarFoto(String lugar) {
		if (lugar == null || lugar.isBlank()) {
			return Optional.empty();
		}
		Optional<JsonNode> resumen = LlamadaExterna.ejecutar(nombreFuente(), () -> resumen(lugar.trim()));
		if (resumen.isEmpty() || "disambiguation".equals(resumen.get().path("type").asString(""))) {
			return Optional.empty();
		}
		String original = resumen.get().path("originalimage").path("source").asString("");
		if (original.isBlank()) {
			return Optional.empty();
		}
		String archivo = nombreArchivo(original);
		String paginaArticulo = resumen.get().path("content_urls").path("desktop").path("page")
			.asString(URL_WIKIPEDIA + "/wiki/" + lugar.trim().replace(' ', '_'));

		JsonNode info = LlamadaExterna.ejecutar(nombreFuente(), () -> datosDeCommons(archivo));
		JsonNode imagen = info == null ? null : info.path("query").path("pages").path(0).path("imageinfo").path(0);
		if (imagen == null || imagen.isMissingNode()) {
			// La foto no está en Commons (caso raro): se usa la de Wikipedia y se enlaza el artículo
			String miniatura = resumen.get().path("thumbnail").path("source").asString(original);
			return Optional.of(new FotoDestino(miniatura, archivo, "Ver autor en Wikipedia", null, null, paginaArticulo));
		}
		JsonNode metadatos = imagen.path("extmetadata");
		String autor = LlamadaExterna.sinHtml(metadatos.path("Artist").path("value").asString(""));
		return Optional.of(new FotoDestino(
			imagen.path("thumburl").asString(imagen.path("url").asString(original)),
			archivo,
			autor.isBlank() ? "Autor desconocido" : autor,
			LlamadaExterna.textoONulo(metadatos.path("LicenseShortName").path("value").asString(null)),
			LlamadaExterna.textoONulo(metadatos.path("LicenseUrl").path("value").asString(null)),
			imagen.path("descriptionurl").asString(paginaArticulo)));
	}

	/**
	 * Resumen del artículo. Vacío si no existe un artículo con ese nombre (404).
	 */
	private Optional<JsonNode> resumen(String lugar) {
		try {
			return Optional.ofNullable(wikipedia.get()
				.uri("/api/rest_v1/page/summary/{titulo}", lugar.replace(' ', '_'))
				.retrieve()
				.body(JsonNode.class));
		} catch (HttpClientErrorException.NotFound e) {
			return Optional.empty();
		}
	}

	private JsonNode datosDeCommons(String archivo) {
		return commons.get()
			.uri(uri -> uri.path("/w/api.php")
				.queryParam("action", "query")
				.queryParam("titles", "{titulo}")
				.queryParam("prop", "imageinfo")
				.queryParam("iiprop", "{iiprop}")
				.queryParam("iiurlwidth", ANCHO_FOTO)
				.queryParam("iiextmetadatafilter", "{filtro}")
				.queryParam("format", "json")
				.queryParam("formatversion", 2)
				.build("File:" + archivo, "url|extmetadata", "Artist|LicenseShortName|LicenseUrl"))
			.retrieve()
			.body(JsonNode.class);
	}

	/**
	 * Saca el nombre del archivo de la URL de la foto.
	 * Ejemplo: {@code .../commons/c/cf/Cartagena_skyline%2C_Colombia.jpg?utm_source=...} da
	 * {@code Cartagena_skyline,_Colombia.jpg}.
	 * Si la URL es de una miniatura ({@code .../thumb/d/d8/Archivo.jpg/3840px-Archivo.jpg}), el nombre es el
	 * penúltimo tramo, no el último.
	 */
	static String nombreArchivo(String url) {
		String sinParametros = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
		String[] tramos = sinParametros.split("/");
		String archivo = tramos[tramos.length - 1];
		if (sinParametros.contains("/thumb/") && archivo.matches("^\\d+px-.*") && tramos.length > 1) {
			archivo = tramos[tramos.length - 2];
		}
		return URLDecoder.decode(archivo, StandardCharsets.UTF_8);
	}

	@Override
	public String nombreFuente() {
		return "Wikimedia Commons";
	}

	@Override
	public String atribucion() {
		return "Foto de Wikimedia Commons; el autor y la licencia se indican en cada foto";
	}

	@Override
	public String enlaceFuente() {
		return URL_COMMONS;
	}
}
