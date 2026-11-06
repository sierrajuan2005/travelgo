package com.travelgo.proveedor.destinos;

import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Adaptador de la API de geocodificación de Open-Meteo (gratis y sin llave).
 * Ejemplo: {@code https://geocoding-api.open-meteo.com/v1/search?name=Cusco&count=10&language=es}
 */
public class OpenMeteoGeocodificador implements Geocodificador {

	static final String URL_BASE = "https://geocoding-api.open-meteo.com";

	private final RestClient http;

	/**
	 * @param http builder de RestClient con los tiempos de espera ya configurados
	 */
	public OpenMeteoGeocodificador(RestClient.Builder http) {
		this.http = http.clone().baseUrl(URL_BASE).build();
	}

	@Override
	public Optional<Coordenadas> ubicar(String ciudad, String pais) {
		JsonNode respuesta = LlamadaExterna.ejecutar(nombreFuente(), () -> http.get()
			.uri(uri -> uri.path("/v1/search")
				.queryParam("name", "{ciudad}")
				.queryParam("count", 10)
				.queryParam("language", "es")
				.queryParam("format", "json")
				.build(ciudad))
			.retrieve()
			.body(JsonNode.class));

		JsonNode resultados = respuesta == null ? null : respuesta.path("results");
		if (resultados == null || !resultados.isArray() || resultados.isEmpty()) {
			return Optional.empty();
		}
		// Se prefiere el resultado del mismo país; si ninguno coincide, el primero (el más poblado)
		JsonNode elegido = resultados.get(0);
		String paisBuscado = LlamadaExterna.normalizar(pais);
		for (JsonNode resultado : resultados) {
			if (!paisBuscado.isEmpty()
				&& LlamadaExterna.normalizar(resultado.path("country").asString("")).equals(paisBuscado)) {
				elegido = resultado;
				break;
			}
		}
		return Optional.of(new Coordenadas(elegido.path("latitude").asDouble(), elegido.path("longitude").asDouble()));
	}

	@Override
	public String nombreFuente() {
		return "Open-Meteo";
	}

	@Override
	public String atribucion() {
		return "Ubicación: Open-Meteo Geocoding API, datos de GeoNames (CC BY 4.0)";
	}

	@Override
	public String enlaceFuente() {
		return "https://open-meteo.com/";
	}
}
