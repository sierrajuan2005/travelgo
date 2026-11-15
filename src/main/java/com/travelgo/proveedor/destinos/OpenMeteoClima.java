package com.travelgo.proveedor.destinos;

import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador de la API de pronóstico de Open-Meteo (gratis y sin llave, uso no comercial).
 * Traduce la respuesta al modelo {@link Clima} de TravelGo (patrón Adapter).
 * Ejemplo: {@code https://api.open-meteo.com/v1/forecast?latitude=10.39&longitude=-75.48&current=temperature_2m,weather_code}
 */
public class OpenMeteoClima implements ProveedorClima {

	static final String URL_BASE = "https://api.open-meteo.com";

	/** Hoy y los 3 días siguientes. */
	private static final int DIAS_PRONOSTICO = 4;

	private final RestClient http;

	/**
	 * @param http builder de RestClient con los tiempos de espera ya configurados
	 */
	public OpenMeteoClima(RestClient.Builder http) {
		this.http = http.clone().baseUrl(URL_BASE).build();
	}

	@Override
	public Clima consultarClima(Coordenadas lugar) {
		JsonNode respuesta = LlamadaExterna.ejecutar(nombreFuente(), () -> http.get()
			.uri(uri -> uri.path("/v1/forecast")
				.queryParam("latitude", lugar.latitud())
				.queryParam("longitude", lugar.longitud())
				.queryParam("current", "temperature_2m,weather_code")
				.queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min")
				.queryParam("timezone", "auto")
				.queryParam("forecast_days", DIAS_PRONOSTICO)
				.build())
			.retrieve()
			.body(JsonNode.class));

		return LlamadaExterna.ejecutar(nombreFuente(), () -> leer(respuesta));
	}

	private Clima leer(JsonNode respuesta) {
		JsonNode actual = respuesta.path("current");
		if (actual.isMissingNode() || !actual.has("temperature_2m")) {
			throw new IllegalArgumentException("La respuesta no trae el clima actual.");
		}
		int codigoActual = actual.path("weather_code").asInt(-1);

		JsonNode diario = respuesta.path("daily");
		JsonNode fechas = diario.path("time");
		List<Clima.Dia> dias = new ArrayList<>();
		for (int i = 0; i < fechas.size(); i++) {
			int codigo = diario.path("weather_code").path(i).asInt(-1);
			dias.add(new Clima.Dia(LocalDate.parse(fechas.path(i).asString()),
				diario.path("temperature_2m_min").path(i).asDouble(),
				diario.path("temperature_2m_max").path(i).asDouble(),
				descripcion(codigo), icono(codigo)));
		}
		return new Clima(actual.path("temperature_2m").asDouble(), descripcion(codigoActual), icono(codigoActual), dias);
	}

	/**
	 * Traduce el código WMO del estado del cielo a español.
	 * Tabla de códigos: https://open-meteo.com/en/docs (sección "WMO Weather interpretation codes").
	 */
	static String descripcion(int codigo) {
		return switch (codigo) {
			case 0 -> "Despejado";
			case 1 -> "Mayormente despejado";
			case 2 -> "Parcialmente nublado";
			case 3 -> "Nublado";
			case 45, 48 -> "Niebla";
			case 51, 53, 55, 56, 57 -> "Llovizna";
			case 61, 63, 65, 66, 67 -> "Lluvia";
			case 71, 73, 75, 77 -> "Nieve";
			case 80, 81, 82 -> "Chubascos";
			case 85, 86 -> "Chubascos de nieve";
			case 95, 96, 99 -> "Tormenta";
			default -> "Sin dato";
		};
	}

	/**
	 * Ícono de Bootstrap Icons para cada código WMO.
	 */
	static String icono(int codigo) {
		return switch (codigo) {
			case 0, 1 -> "bi-sun";
			case 2 -> "bi-cloud-sun";
			case 3 -> "bi-clouds";
			case 45, 48 -> "bi-cloud-fog2";
			case 51, 53, 55, 56, 57 -> "bi-cloud-drizzle";
			case 61, 63, 65, 66, 67 -> "bi-cloud-rain";
			case 71, 73, 75, 77, 85, 86 -> "bi-cloud-snow";
			case 80, 81, 82 -> "bi-cloud-rain-heavy";
			case 95, 96, 99 -> "bi-cloud-lightning-rain";
			default -> "bi-question-circle";
		};
	}

	@Override
	public String nombreFuente() {
		return "Open-Meteo";
	}

	@Override
	public String atribucion() {
		return "Datos del clima: Open-Meteo.com (CC BY 4.0)";
	}

	@Override
	public String enlaceFuente() {
		return "https://open-meteo.com/";
	}
}
