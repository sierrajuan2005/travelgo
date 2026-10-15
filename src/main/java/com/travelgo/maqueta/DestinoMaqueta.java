package com.travelgo.maqueta;

/**
 * Destino de ejemplo con los mismos atributos que Destino del diagrama de clases.
 * Se elimina cuando exista la entidad Destino.
 */
public class DestinoMaqueta {

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
