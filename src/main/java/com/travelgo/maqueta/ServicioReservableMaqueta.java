package com.travelgo.maqueta;

import com.travelgo.model.ServicioReservable;

/**
 * Servicio reservable de ejemplo con los mismos atributos que ServicioReservable del diagrama de clases.
 * Se elimina cuando la consola use ServicioReservableService.
 */
public abstract class ServicioReservableMaqueta {

	private final String id;
	private final double precio;
	private boolean disponible;

	protected ServicioReservableMaqueta(String id, double precio, boolean disponible) {
		this.id = id;
		this.precio = precio;
		this.disponible = disponible;
	}

	/**
	 * Precio final del servicio. Las subclases lo sobrescriben si el cálculo cambia (por ejemplo, Hotel por noches).
	 */
	public double calcularPrecio() {
		return precio;
	}

	/**
	 * Texto corto del servicio para la factura. Cada subclase lo arma con sus propios datos.
	 */
	public abstract String describir();

	/**
	 * Crea el servicio real del dominio (Vuelo, Hotel o Transporte) con los mismos datos, para agregarlo a una
	 * Reserva real.
	 */
	public abstract ServicioReservable aModelo();

	/**
	 * Pasa el servicio de disponible a no disponible, o al revés. Lo usa el panel de administración.
	 */
	public void cambiarDisponibilidad() {
		disponible = !disponible;
	}

	public String getId() {
		return id;
	}

	public double getPrecio() {
		return precio;
	}

	public boolean isDisponible() {
		return disponible;
	}

}
