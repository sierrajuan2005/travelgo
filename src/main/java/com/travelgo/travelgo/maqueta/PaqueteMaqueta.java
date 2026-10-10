package com.travelgo.travelgo.maqueta;

import java.util.List;

/**
 * Paquete turístico de ejemplo para las maquetas del frontend.
 * Se elimina en el Sprint 2, cuando existan las entidades Destino y PaqueteTuristico.
 */
public class PaqueteMaqueta {

	private static final List<PaqueteMaqueta> PAQUETES = List.of(
		new PaqueteMaqueta(1L, "Cartagena colonial", "Cartagena", "Colombia",
			"Recorre la ciudad amurallada, disfruta de sus playas y de la gastronomía del Caribe.",
			450.00, 4, "bi-sun", List.of("Hotel 4 estrellas", "Desayuno incluido", "Tour por la ciudad amurallada")),
		new PaqueteMaqueta(2L, "Machu Picchu mágico", "Cusco", "Perú",
			"Visita la ciudadela inca, el Valle Sagrado y el centro histórico de Cusco.",
			780.00, 5, "bi-triangle", List.of("Hotel 3 estrellas", "Tren a Aguas Calientes", "Entrada a Machu Picchu")),
		new PaqueteMaqueta(3L, "Cancún todo incluido", "Cancún", "México",
			"Relájate en las playas del Caribe mexicano con todo incluido.",
			1200.00, 7, "bi-water", List.of("Resort todo incluido", "Traslados aeropuerto-hotel", "Excursión a Isla Mujeres")),
		new PaqueteMaqueta(4L, "Madrid y Toledo", "Madrid", "España",
			"Descubre los museos y barrios de Madrid con una excursión de un día a Toledo.",
			1650.00, 8, "bi-building", List.of("Hotel céntrico", "Desayuno incluido", "Excursión a Toledo")),
		new PaqueteMaqueta(5L, "Buenos Aires de tango", "Buenos Aires", "Argentina",
			"Vive el tango, la gastronomía porteña y los barrios de San Telmo y La Boca.",
			890.00, 5, "bi-music-note-beamed", List.of("Hotel 4 estrellas", "Show de tango con cena", "City tour")),
		new PaqueteMaqueta(6L, "Punta Cana relax", "Punta Cana", "República Dominicana",
			"Playas de arena blanca, resort frente al mar y actividades acuáticas.",
			1100.00, 6, "bi-umbrella", List.of("Resort todo incluido", "Traslados", "Snorkel en arrecife"))
	);

	private final Long id;
	private final String nombre;
	private final String destino;
	private final String pais;
	private final String descripcion;
	private final double precio;
	private final int duracionDias;
	private final String icono;
	private final List<String> incluye;

	public PaqueteMaqueta(Long id, String nombre, String destino, String pais, String descripcion,
			double precio, int duracionDias, String icono, List<String> incluye) {
		this.id = id;
		this.nombre = nombre;
		this.destino = destino;
		this.pais = pais;
		this.descripcion = descripcion;
		this.precio = precio;
		this.duracionDias = duracionDias;
		this.icono = icono;
		this.incluye = incluye;
	}

	public static List<PaqueteMaqueta> todos() {
		return PAQUETES;
	}

	public static PaqueteMaqueta buscar(Long id) {
		return PAQUETES.stream()
			.filter(p -> p.getId().equals(id))
			.findFirst()
			.orElse(null);
	}

	public Long getId() {
		return id;
	}

	public String getNombre() {
		return nombre;
	}

	public String getDestino() {
		return destino;
	}

	public String getPais() {
		return pais;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public double getPrecio() {
		return precio;
	}

	public int getDuracionDias() {
		return duracionDias;
	}

	public String getIcono() {
		return icono;
	}

	public List<String> getIncluye() {
		return incluye;
	}

}
