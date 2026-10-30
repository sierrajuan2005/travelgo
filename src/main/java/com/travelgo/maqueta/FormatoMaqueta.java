package com.travelgo.maqueta;

import com.travelgo.model.Hotel;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;

/**
 * Textos que muestran la consola y la web para reservas, servicios y pagos. Están en un solo lugar para que las dos
 * interfaces digan lo mismo.
 */
public final class FormatoMaqueta {

	private FormatoMaqueta() {
	}

	/**
	 * Texto corto de un servicio para la factura. Cada tipo usa sus propios datos.
	 */
	public static String describir(ServicioReservable servicio) {
		return switch (servicio) {
			case Vuelo vuelo -> "Vuelo " + vuelo.getAerolinea() + ", " + vuelo.getOrigen() + " -> " + vuelo.getDestino();
			case Hotel hotel -> hotel.getNombre() + ", " + hotel.getNoches() + " noches";
			case Transporte transporte -> transporte.getTipo() + ", " + transporte.getOrigen() + " -> "
				+ transporte.getDestino();
			default -> "Servicio " + servicio.getId();
		};
	}

	public static String nombreMetodo(MetodoPago metodo) {
		return switch (metodo) {
			case TARJETA_CREDITO -> "Tarjeta de crédito";
			case TARJETA_DEBITO -> "Tarjeta de débito";
			case TRANSFERENCIA -> "Transferencia";
			case EFECTIVO -> "Efectivo";
		};
	}

	public static String nombrePaquete(Reserva reserva) {
		return reserva.getPaqueteTuristico() == null ? "Sin paquete" : reserva.getPaqueteTuristico().getNombre();
	}

	/**
	 * Primeros 8 caracteres del id de la reserva (es un UUID), para que se pueda leer en pantalla.
	 */
	public static String idCorto(String id) {
		return id.substring(0, 8);
	}

}
