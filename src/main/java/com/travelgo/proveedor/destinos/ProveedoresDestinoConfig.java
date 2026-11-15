package com.travelgo.proveedor.destinos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Crea los adaptadores de las APIs de destinos. Aquí es el único lugar donde se decide qué API concreta se
 * usa: InfoDestinoService solo conoce las interfaces (inversión de dependencias).
 * <p>
 * La llave de OpenTripMap se lee de la variable de entorno {@code OPENTRIPMAP_API_KEY} (nunca va en el
 * repositorio). Sin llave, las atracciones salen de Wikipedia.
 */
@Configuration
public class ProveedoresDestinoConfig {

	/** Nombre del builder de RestClient que usan los proveedores. */
	public static final String HTTP_PROVEEDORES = "httpProveedores";

	private static final Logger LOG = LoggerFactory.getLogger(ProveedoresDestinoConfig.class);

	/** Wikimedia pide un User-Agent que diga qué aplicación hace las consultas. */
	private static final String USER_AGENT = "TravelGo/0.4 (proyecto academico UPC; Spring RestClient)";

	/**
	 * RestClient con tiempos de espera cortos: si una API está lenta, la página no se queda colgada.
	 */
	@Bean(HTTP_PROVEEDORES)
	public RestClient.Builder httpProveedores(
			@Value("${travelgo.proveedores.timeout-segundos:5}") int segundos) {
		HttpClient cliente = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(segundos))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
		fabrica.setReadTimeout(Duration.ofSeconds(segundos));
		return RestClient.builder()
			.requestFactory(fabrica)
			.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT);
	}

	@Bean
	public Geocodificador geocodificador(@Qualifier(HTTP_PROVEEDORES) RestClient.Builder http) {
		return new OpenMeteoGeocodificador(http);
	}

	@Bean
	public ProveedorClima proveedorClima(@Qualifier(HTTP_PROVEEDORES) RestClient.Builder http) {
		return new OpenMeteoClima(http);
	}

	@Bean
	public ProveedorFotos proveedorFotos(@Qualifier(HTTP_PROVEEDORES) RestClient.Builder http) {
		return new WikipediaFotos(http);
	}

	/**
	 * OpenTripMap si hay llave; si no, Wikipedia. Cambiar de API es cambiar este método, nada más.
	 */
	@Bean
	public ProveedorAtracciones proveedorAtracciones(@Qualifier(HTTP_PROVEEDORES) RestClient.Builder http,
			@Value("${travelgo.proveedores.opentripmap.api-key:}") String llaveOpenTripMap) {
		if (llaveOpenTripMap == null || llaveOpenTripMap.isBlank()) {
			LOG.info("Sin OPENTRIPMAP_API_KEY: las atracciones se consultan en Wikipedia.");
			return new WikipediaAtracciones(http);
		}
		LOG.info("Atracciones: OpenTripMap.");
		return new OpenTripMapAtracciones(http, llaveOpenTripMap);
	}
}
