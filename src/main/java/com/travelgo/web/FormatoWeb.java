package com.travelgo.web;

import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Textos que se muestran en las páginas web. Las plantillas lo usan como {@code ${@formato.cop(valor)}}.
 * La consola usa los mismos textos (FormatoMaqueta los toma de aquí).
 */
@Component("formato")
public class FormatoWeb {

	private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
	private static final DateTimeFormatter DIA_CORTO = DateTimeFormatter.ofPattern("EEE dd", Locale.of("es", "CO"));

	/**
	 * Precio en pesos colombianos, sin decimales: {@code 1200000 -> "$ 1.200.000"}.
	 * Los precios de la base de datos (data.sql) están en COP.
	 */
	public String cop(Number valor) {
		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setGroupingSeparator('.');
		simbolos.setDecimalSeparator(',');
		return "$ " + new DecimalFormat("#,##0", simbolos).format(valor == null ? 0 : valor.doubleValue());
	}

	/** Fecha y hora de una consulta a una API: "06/11/2026 14:30". */
	public String fechaHora(LocalDateTime fecha) {
		return fecha == null ? "" : fecha.format(FECHA_HORA);
	}

	/** Día del pronóstico del clima: "sáb 07". */
	public String diaCorto(LocalDate fecha) {
		return fecha == null ? "" : fecha.format(DIA_CORTO);
	}

	/** Ciudades de los destinos del paquete: "Cartagena, Colombia". */
	public String destinos(PaqueteTuristico paquete) {
		if (paquete == null || paquete.getDestinos() == null || paquete.getDestinos().isEmpty()) {
			return "Sin destino";
		}
		return paquete.getDestinos().stream()
			.map(d -> d.getCiudad() + ", " + d.getPais())
			.collect(Collectors.joining(" · "));
	}

	/** Primer destino del paquete (el que se usa para la foto, el clima y las atracciones), o null. */
	public Destino destinoPrincipal(PaqueteTuristico paquete) {
		return paquete == null || paquete.getDestinos() == null || paquete.getDestinos().isEmpty()
			? null : paquete.getDestinos().get(0);
	}

	/** Descripción corta de un servicio: "Vuelo Avianca, Bogotá -> Cartagena". */
	public static String describir(ServicioReservable servicio) {
		return switch (servicio) {
			case Vuelo vuelo -> "Vuelo " + vuelo.getAerolinea() + ", " + vuelo.getOrigen() + " -> " + vuelo.getDestino();
			case Hotel hotel -> hotel.getNombre() + ", " + hotel.getNoches() + " noches";
			case Transporte transporte -> transporte.getTipo() + ", " + transporte.getOrigen() + " -> "
				+ transporte.getDestino();
			default -> "Servicio " + servicio.getId();
		};
	}

	/** Tipo del servicio para mostrar: Vuelo, Hotel o Transporte. */
	public static String tipo(ServicioReservable servicio) {
		return switch (servicio) {
			case Vuelo v -> "Vuelo";
			case Hotel h -> "Hotel";
			case Transporte t -> "Transporte";
			default -> "Servicio";
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
	 * Los primeros 8 caracteres del id. Los ids de data.sql son cortos ("r-vacia"), así que no se corta
	 * si el id tiene 8 o menos.
	 */
	public static String idCorto(String id) {
		return id == null || id.length() <= 8 ? id : id.substring(0, 8);
	}
}
