package com.travelgo.maqueta;

import com.travelgo.model.Hotel;

import java.util.List;

/**
 * Hotel de ejemplo con los mismos atributos que Hotel del diagrama de clases.
 * El precio es por noche.
 */
public class HotelMaqueta extends ServicioReservableMaqueta {

	private final String nombre;
	private final String direccion;
	private final String tipoHabitacion;
	private final int noches;

	public HotelMaqueta(String id, double precio, boolean disponible, String nombre, String direccion,
			String tipoHabitacion, int noches) {
		super(id, precio, disponible);
		this.nombre = nombre;
		this.direccion = direccion;
		this.tipoHabitacion = tipoHabitacion;
		this.noches = noches;
	}

	/**
	 * Hoteles de ejemplo en la ciudad del paquete.
	 */
	public static List<HotelMaqueta> deEjemplo(String ciudad, int noches) {
		return List.of(
			new HotelMaqueta("h1", 90.00, true, "Hotel Centro " + ciudad, "Calle principal 10, " + ciudad,
				"Habitación doble", noches),
			new HotelMaqueta("h2", 180.00, true, "Gran Hotel " + ciudad, "Avenida del mar 200, " + ciudad,
				"Suite", noches));
	}

	@Override
	public String describir() {
		return nombre + ", " + noches + " noches";
	}

	@Override
	public Hotel aModelo() {
		return new Hotel(getId(), getPrecio(), isDisponible(), nombre, direccion, tipoHabitacion, noches);
	}

	/**
	 * Precio por noche multiplicado por el número de noches.
	 */
	@Override
	public double calcularPrecio() {
		return getPrecio() * noches;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDireccion() {
		return direccion;
	}

	public String getTipoHabitacion() {
		return tipoHabitacion;
	}

	public int getNoches() {
		return noches;
	}

}
