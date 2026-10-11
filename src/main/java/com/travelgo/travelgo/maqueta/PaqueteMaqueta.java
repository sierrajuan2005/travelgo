package com.travelgo.travelgo.maqueta;

import java.util.List;

/**
 * Paquete turístico de ejemplo con los mismos atributos que PaqueteTuristico del diagrama de clases.
 * Se elimina cuando existan las entidades Destino y PaqueteTuristico.
 */
public class PaqueteMaqueta {

	private static final List<PaqueteMaqueta> PAQUETES = List.of(
		new PaqueteMaqueta("1", "Cartagena colonial",
			"Recorre la ciudad amurallada, disfruta de sus playas y de la gastronomía del Caribe.", 450.00, 4, true,
			new DestinoMaqueta("d1", "Cartagena de Indias", "Ciudad amurallada del Caribe colombiano.", "Colombia", "Cartagena")),
		new PaqueteMaqueta("2", "Machu Picchu mágico",
			"Visita la ciudadela inca, el Valle Sagrado y el centro histórico de Cusco.", 780.00, 5, true,
			new DestinoMaqueta("d2", "Cusco y Machu Picchu", "Antigua capital del imperio inca.", "Perú", "Cusco")),
		new PaqueteMaqueta("3", "Cancún todo incluido",
			"Relájate en las playas del Caribe mexicano con todo incluido.", 1200.00, 7, true,
			new DestinoMaqueta("d3", "Cancún", "Playas de arena blanca en el Caribe mexicano.", "México", "Cancún")),
		new PaqueteMaqueta("4", "Madrid y Toledo",
			"Descubre los museos y barrios de Madrid con una excursión de un día a Toledo.", 1650.00, 8, true,
			new DestinoMaqueta("d4", "Madrid", "Capital de España, con museos y barrios históricos.", "España", "Madrid")),
		new PaqueteMaqueta("5", "Buenos Aires de tango",
			"Vive el tango, la gastronomía porteña y los barrios de San Telmo y La Boca.", 890.00, 5, true,
			new DestinoMaqueta("d5", "Buenos Aires", "Capital del tango.", "Argentina", "Buenos Aires")),
		new PaqueteMaqueta("6", "Punta Cana relax",
			"Playas de arena blanca, resort frente al mar y actividades acuáticas.", 1100.00, 6, true,
			new DestinoMaqueta("d6", "Punta Cana", "Resorts frente al mar Caribe.", "República Dominicana", "Punta Cana"))
	);

	private final String id;
	private final String nombre;
	private final String descripcion;
	private final double precio;
	private final int duracionDias;
	private final boolean activo;
	private final DestinoMaqueta destino;

	public PaqueteMaqueta(String id, String nombre, String descripcion, double precio, int duracionDias,
			boolean activo, DestinoMaqueta destino) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
		this.duracionDias = duracionDias;
		this.activo = activo;
		this.destino = destino;
	}

	/**
	 * Devuelve solo los paquetes activos (Historia 3: solo se listan paquetes activos).
	 */
	public static List<PaqueteMaqueta> activos() {
		return PAQUETES.stream()
			.filter(PaqueteMaqueta::isActivo)
			.toList();
	}

	/**
	 * Busca un paquete activo por su id. Devuelve null si no existe.
	 */
	public static PaqueteMaqueta buscar(String id) {
		return activos().stream()
			.filter(p -> p.getId().equals(id))
			.findFirst()
			.orElse(null);
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

	public double getPrecio() {
		return precio;
	}

	public int getDuracionDias() {
		return duracionDias;
	}

	public boolean isActivo() {
		return activo;
	}

	public DestinoMaqueta getDestino() {
		return destino;
	}

}
