package com.travelgo.proveedor.destinos;

/**
 * Una API externa falló: no respondió, devolvió un error, se agotó la cuota o la respuesta no se entiende.
 * El sistema no se cae: la web muestra un aviso y el resto de la información sigue funcionando.
 */
public class ProveedorExternoException extends RuntimeException {

	private final String fuente;

	/**
	 * @param fuente  nombre de la API que falló, por ejemplo "Open-Meteo"
	 * @param mensaje explicación clara para el usuario
	 * @param causa   error original (puede ser null)
	 */
	public ProveedorExternoException(String fuente, String mensaje, Throwable causa) {
		super(mensaje, causa);
		this.fuente = fuente;
	}

	/** Nombre de la API que falló. */
	public String getFuente() {
		return fuente;
	}
}
