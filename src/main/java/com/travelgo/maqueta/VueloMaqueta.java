package com.travelgo.maqueta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Vuelo de ejemplo con los mismos atributos que Vuelo del diagrama de clases.
 */
public class VueloMaqueta extends ServicioReservableMaqueta {

	private final String aerolinea;
	private final String origen;
	private final String destino;
	private final LocalDateTime fechaSalida;
	private final LocalDateTime fechaLlegada;

	public VueloMaqueta(String id, double precio, boolean disponible, String aerolinea, String origen,
			String destino, LocalDateTime fechaSalida, LocalDateTime fechaLlegada) {
		super(id, precio, disponible);
		this.aerolinea = aerolinea;
		this.origen = origen;
		this.destino = destino;
		this.fechaSalida = fechaSalida;
		this.fechaLlegada = fechaLlegada;
	}

	/**
	 * Vuelos de ejemplo hacia la ciudad del paquete.
	 */
	public static List<VueloMaqueta> deEjemplo(String ciudad) {
		return List.of(
			new VueloMaqueta("v1", 320.00, true, "Avianca", "Bogotá", ciudad,
				LocalDateTime.of(2026, 11, 20, 7, 30), LocalDateTime.of(2026, 11, 20, 10, 45)),
			new VueloMaqueta("v2", 260.00, true, "LATAM", "Bogotá", ciudad,
				LocalDateTime.of(2026, 11, 20, 13, 15), LocalDateTime.of(2026, 11, 20, 19, 40)));
	}

	public String getAerolinea() {
		return aerolinea;
	}

	public String getOrigen() {
		return origen;
	}

	public String getDestino() {
		return destino;
	}

	public LocalDateTime getFechaSalida() {
		return fechaSalida;
	}

	public LocalDateTime getFechaLlegada() {
		return fechaLlegada;
	}

}
