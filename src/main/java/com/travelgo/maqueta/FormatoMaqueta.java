package com.travelgo.maqueta;

import com.travelgo.model.MetodoPago;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.web.FormatoWeb;

/**
 * Textos que muestra la consola para reservas, servicios y pagos. Los toma de {@link FormatoWeb} para que la
 * consola y la web digan lo mismo.
 */
public final class FormatoMaqueta {

	private FormatoMaqueta() {
	}

	/**
	 * Texto corto de un servicio para la factura. Cada tipo usa sus propios datos.
	 */
	public static String describir(ServicioReservable servicio) {
		return FormatoWeb.describir(servicio);
	}

	public static String nombreMetodo(MetodoPago metodo) {
		return FormatoWeb.nombreMetodo(metodo);
	}

	public static String nombrePaquete(Reserva reserva) {
		return FormatoWeb.nombrePaquete(reserva);
	}

	/**
	 * Primeros 8 caracteres del id de la reserva, para que se pueda leer en pantalla.
	 */
	public static String idCorto(String id) {
		return FormatoWeb.idCorto(id);
	}

}
