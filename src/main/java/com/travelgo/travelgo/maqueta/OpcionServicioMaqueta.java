package com.travelgo.travelgo.maqueta;

import java.util.List;

/**
 * Opción de vuelo, hotel o transporte de ejemplo para la maqueta de reserva.
 * Se elimina en el Sprint 2, cuando existan Vuelo, Hotel y Transporte.
 */
public class OpcionServicioMaqueta {

	private final Long id;
	private final String titulo;
	private final String detalle;
	private final double precio;

	public OpcionServicioMaqueta(Long id, String titulo, String detalle, double precio) {
		this.id = id;
		this.titulo = titulo;
		this.detalle = detalle;
		this.precio = precio;
	}

	public static List<OpcionServicioMaqueta> vuelos(String destino) {
		return List.of(
			new OpcionServicioMaqueta(1L, "Avianca · vuelo directo a " + destino, "Salida 07:30 · equipaje de mano", 320.00),
			new OpcionServicioMaqueta(2L, "LATAM · vuelo con escala a " + destino, "Salida 13:15 · maleta de 23 kg", 260.00));
	}

	public static List<OpcionServicioMaqueta> hoteles(String destino) {
		return List.of(
			new OpcionServicioMaqueta(1L, "Hotel Centro " + destino, "Habitación doble · desayuno", 90.00),
			new OpcionServicioMaqueta(2L, "Gran Hotel " + destino, "Suite · desayuno y cena", 180.00));
	}

	public static List<OpcionServicioMaqueta> transportes(String destino) {
		return List.of(
			new OpcionServicioMaqueta(1L, "Traslado privado", "Aeropuerto ↔ hotel en " + destino, 45.00),
			new OpcionServicioMaqueta(2L, "Alquiler de auto", "Auto compacto por todo el viaje", 150.00));
	}

	public Long getId() {
		return id;
	}

	public String getTitulo() {
		return titulo;
	}

	public String getDetalle() {
		return detalle;
	}

	public double getPrecio() {
		return precio;
	}

}
