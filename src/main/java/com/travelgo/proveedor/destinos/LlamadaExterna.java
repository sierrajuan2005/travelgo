package com.travelgo.proveedor.destinos;

import com.travelgo.util.Textos;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

/**
 * Ayudas que comparten los adaptadores: traducir los errores HTTP a {@link ProveedorExternoException}
 * con un mensaje claro, y limpiar textos.
 */
final class LlamadaExterna {

	private LlamadaExterna() {
	}

	/**
	 * Ejecuta una llamada a una API. Si falla, lanza {@link ProveedorExternoException} con un mensaje que
	 * explica qué pasó (cuota agotada, llave inválida, sin respuesta, etc.).
	 *
	 * @param fuente  nombre de la API, para el mensaje
	 * @param llamada la llamada con RestClient
	 */
	static <T> T ejecutar(String fuente, Supplier<T> llamada) {
		try {
			return llamada.get();
		} catch (HttpClientErrorException.TooManyRequests e) {
			throw new ProveedorExternoException(fuente,
				fuente + " no respondió porque se agotó la cuota gratuita. Intenta más tarde.", e);
		} catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
			throw new ProveedorExternoException(fuente,
				fuente + " rechazó la llave de la API. Revisa la variable de entorno.", e);
		} catch (HttpStatusCodeException e) {
			throw new ProveedorExternoException(fuente,
				fuente + " respondió con un error (" + e.getStatusCode().value() + ").", e);
		} catch (ResourceAccessException e) {
			throw new ProveedorExternoException(fuente,
				fuente + " no respondió a tiempo o no hay conexión a internet.", e);
		} catch (RestClientException | IllegalArgumentException | NullPointerException e) {
			// IllegalArgument / NullPointer: la respuesta llegó con un formato que no se esperaba
			throw new ProveedorExternoException(fuente,
				"No se pudo leer la respuesta de " + fuente + ".", e);
		}
	}

	/**
	 * Pasa a minúsculas y quita tildes, para comparar "Perú" con "peru" o "Bogotá" con "BOGOTA".
	 */
	static String normalizar(String texto) {
		return Textos.normalizar(texto);
	}

	/**
	 * Quita las etiquetas HTML y los espacios repetidos. Wikimedia devuelve el autor como HTML
	 * (por ejemplo un enlace a su perfil).
	 */
	static String sinHtml(String html) {
		if (html == null) {
			return "";
		}
		return html.replaceAll("<[^>]*>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
			.replaceAll("\\s+", " ").trim();
	}

	/**
	 * Devuelve el texto, o null si está vacío. Las APIs a veces mandan "" en vez de no mandar el campo.
	 */
	static String textoONulo(String texto) {
		return texto == null || texto.isBlank() ? null : texto.trim();
	}
}
