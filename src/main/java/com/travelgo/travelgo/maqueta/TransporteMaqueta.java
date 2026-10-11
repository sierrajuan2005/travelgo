package com.travelgo.travelgo.maqueta;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Transporte de ejemplo con los mismos atributos que Transporte del diagrama de clases.
 */
public class TransporteMaqueta extends ServicioReservableMaqueta {

	private final String tipo;
	private final String origen;
	private final String destino;
	private final LocalDateTime fecha;

	public TransporteMaqueta(String id, double precio, boolean disponible, String tipo, String origen,
			String destino, LocalDateTime fecha) {
		super(id, precio, disponible);
		this.tipo = tipo;
		this.origen = origen;
		this.destino = destino;
		this.fecha = fecha;
	}

	/**
	 * Transportes de ejemplo en la ciudad del paquete.
	 */
	public static List<TransporteMaqueta> deEjemplo(String ciudad) {
		return List.of(
			new TransporteMaqueta("t1", 45.00, true, "Traslado privado", "Aeropuerto de " + ciudad,
				"Hotel", LocalDateTime.of(2026, 11, 20, 12, 0)),
			new TransporteMaqueta("t2", 150.00, true, "Alquiler de auto", "Aeropuerto de " + ciudad,
				ciudad, LocalDateTime.of(2026, 11, 20, 12, 0)));
	}

	public String getTipo() {
		return tipo;
	}

	public String getOrigen() {
		return origen;
	}

	public String getDestino() {
		return destino;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

}
