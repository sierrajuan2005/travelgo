package com.travelgo.maqueta;

import java.util.ArrayList;
import java.util.List;

/**
 * Destino de ejemplo con los mismos atributos que Destino del diagrama de clases.
 * Los destinos se guardan en una lista en memoria para que el panel de administración los gestione.
 * Se elimina cuando exista la entidad Destino.
 */
public class DestinoMaqueta {

	private static final List<DestinoMaqueta> DESTINOS = new ArrayList<>(List.of(
		new DestinoMaqueta("d1", "Cartagena de Indias", "Ciudad amurallada del Caribe colombiano.", "Colombia", "Cartagena"),
		new DestinoMaqueta("d2", "Cusco y Machu Picchu", "Antigua capital del imperio inca.", "Perú", "Cusco"),
		new DestinoMaqueta("d3", "Cancún", "Playas de arena blanca en el Caribe mexicano.", "México", "Cancún"),
		new DestinoMaqueta("d4", "Madrid", "Capital de España, con museos y barrios históricos.", "España", "Madrid"),
		new DestinoMaqueta("d5", "Buenos Aires", "Capital del tango.", "Argentina", "Buenos Aires"),
		new DestinoMaqueta("d6", "Punta Cana", "Resorts frente al mar Caribe.", "República Dominicana", "Punta Cana")
	));

	/** Número del próximo id. No se reutilizan los ids de destinos eliminados. */
	private static int siguienteId = DESTINOS.size() + 1;

	private final String id;
	private final String nombre;
	private final String descripcion;
	private final String pais;
	private final String ciudad;

	public DestinoMaqueta(String id, String nombre, String descripcion, String pais, String ciudad) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.pais = pais;
		this.ciudad = ciudad;
	}

	/**
	 * Devuelve todos los destinos, en el orden en que se crearon.
	 */
	public static List<DestinoMaqueta> todos() {
		return List.copyOf(DESTINOS);
	}

	/**
	 * Busca un destino por su id, sin distinguir mayúsculas. Devuelve null si no existe.
	 */
	public static DestinoMaqueta buscar(String id) {
		return DESTINOS.stream()
			.filter(d -> d.getId().equalsIgnoreCase(id))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Crea un destino con el siguiente id (d7, d8...) y lo guarda en la lista.
	 *
	 * @throws IllegalArgumentException si algún dato está vacío
	 */
	public static DestinoMaqueta agregar(String nombre, String descripcion, String pais, String ciudad) {
		validarTexto(nombre, "nombre");
		validarTexto(descripcion, "descripción");
		validarTexto(pais, "país");
		validarTexto(ciudad, "ciudad");
		DestinoMaqueta destino = new DestinoMaqueta("d" + siguienteId++, nombre.trim(), descripcion.trim(),
			pais.trim(), ciudad.trim());
		DESTINOS.add(destino);
		return destino;
	}

	/**
	 * Elimina un destino que no tenga paquetes.
	 *
	 * @throws IllegalArgumentException si no existe un destino con ese id
	 * @throws IllegalStateException    si algún paquete usa el destino
	 */
	public static void eliminar(String id) {
		DestinoMaqueta destino = buscar(id);
		if (destino == null) {
			throw new IllegalArgumentException("No existe un destino con id " + id + ".");
		}
		boolean tienePaquetes = PaqueteMaqueta.todos().stream()
			.anyMatch(p -> p.getDestino() == destino);
		if (tienePaquetes) {
			throw new IllegalStateException("No se puede eliminar un destino que tiene paquetes.");
		}
		DESTINOS.remove(destino);
	}

	private static void validarTexto(String valor, String campo) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException("El campo " + campo + " es obligatorio.");
		}
	}

	public String getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public String getPais() {
		return pais;
	}

	public String getCiudad() {
		return ciudad;
	}

}
