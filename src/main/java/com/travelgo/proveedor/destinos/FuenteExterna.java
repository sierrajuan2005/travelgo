package com.travelgo.proveedor.destinos;

/**
 * Datos de atribución de una API externa. Los comparten todos los proveedores para que la web muestre
 * siempre de dónde salió cada dato (regla del Sprint 4: atribución obligatoria donde la fuente la exija).
 */
public interface FuenteExterna {

	/** Nombre corto de la fuente, por ejemplo "OpenTripMap". */
	String nombreFuente();

	/** Texto de atribución que pide la licencia de la fuente. */
	String atribucion();

	/** Sitio web de la fuente. */
	String enlaceFuente();
}
