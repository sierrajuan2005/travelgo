package com.travelgo.maqueta;

import com.travelgo.model.PaqueteTuristico;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Paquete turístico de ejemplo con los mismos atributos que PaqueteTuristico del diagrama de clases.
 * Los paquetes se guardan en una lista en memoria para que el panel de administración los gestione.
 * Se elimina cuando existan las entidades Destino y PaqueteTuristico.
 */
public class PaqueteMaqueta {

	private static final List<PaqueteMaqueta> PAQUETES = new ArrayList<>(List.of(
		new PaqueteMaqueta("1", "Cartagena colonial",
			"Recorre la ciudad amurallada, disfruta de sus playas y de la gastronomía del Caribe.", 450.00, 4, true,
			DestinoMaqueta.buscar("d1")),
		new PaqueteMaqueta("2", "Machu Picchu mágico",
			"Visita la ciudadela inca, el Valle Sagrado y el centro histórico de Cusco.", 780.00, 5, true,
			DestinoMaqueta.buscar("d2")),
		new PaqueteMaqueta("3", "Cancún todo incluido",
			"Relájate en las playas del Caribe mexicano con todo incluido.", 1200.00, 7, true,
			DestinoMaqueta.buscar("d3")),
		new PaqueteMaqueta("4", "Madrid y Toledo",
			"Descubre los museos y barrios de Madrid con una excursión de un día a Toledo.", 1650.00, 8, true,
			DestinoMaqueta.buscar("d4")),
		new PaqueteMaqueta("5", "Buenos Aires de tango",
			"Vive el tango, la gastronomía porteña y los barrios de San Telmo y La Boca.", 890.00, 5, true,
			DestinoMaqueta.buscar("d5")),
		new PaqueteMaqueta("6", "Punta Cana relax",
			"Playas de arena blanca, resort frente al mar y actividades acuáticas.", 1100.00, 6, true,
			DestinoMaqueta.buscar("d6"))
	));

	/** Número del próximo id. No se reutilizan los ids de paquetes eliminados. */
	private static int siguienteId = PAQUETES.size() + 1;

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
	 * Filtra los paquetes activos por destino y rango de precios (Historia 3).
	 * Un filtro vacío o null no se aplica.
	 *
	 * @param destino   texto a buscar en la ciudad, el país o el nombre del destino
	 * @param precioMin precio mínimo
	 * @param precioMax precio máximo
	 */
	public static List<PaqueteMaqueta> filtrar(String destino, Double precioMin, Double precioMax) {
		String texto = destino == null ? "" : destino.trim().toLowerCase();
		return activos().stream()
			.filter(p -> texto.isEmpty()
				|| p.getDestino().getCiudad().toLowerCase().contains(texto)
				|| p.getDestino().getPais().toLowerCase().contains(texto)
				|| p.getDestino().getNombre().toLowerCase().contains(texto))
			.filter(p -> precioMin == null || p.getPrecio() >= precioMin)
			.filter(p -> precioMax == null || p.getPrecio() <= precioMax)
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

	/**
	 * Devuelve todos los paquetes, activos e inactivos. Lo usa el panel de administración.
	 */
	public static List<PaqueteMaqueta> todos() {
		return List.copyOf(PAQUETES);
	}

	/**
	 * Crea un paquete con el siguiente id (7, 8...) y lo guarda en la lista.
	 *
	 * @throws IllegalArgumentException si falta un dato, el precio o la duración no son mayores que cero,
	 *                                  o el destino es null
	 */
	public static PaqueteMaqueta agregar(String nombre, String descripcion, double precio, int duracionDias,
			boolean activo, DestinoMaqueta destino) {
		if (nombre == null || nombre.isBlank() || descripcion == null || descripcion.isBlank()) {
			throw new IllegalArgumentException("El nombre y la descripción son obligatorios.");
		}
		if (precio <= 0 || duracionDias <= 0) {
			throw new IllegalArgumentException("El precio y la duración deben ser mayores que cero.");
		}
		if (destino == null) {
			throw new IllegalArgumentException("El paquete necesita un destino.");
		}
		PaqueteMaqueta paquete = new PaqueteMaqueta(String.valueOf(siguienteId++), nombre.trim(), descripcion.trim(),
			precio, duracionDias, activo, destino);
		PAQUETES.add(paquete);
		return paquete;
	}

	/**
	 * Elimina un paquete que no tenga reservas.
	 *
	 * @throws IllegalArgumentException si no existe un paquete con ese id
	 * @throws IllegalStateException    si alguna reserva usa el paquete
	 */
	public static void eliminar(String id) {
		PaqueteMaqueta paquete = PAQUETES.stream()
			.filter(p -> p.getId().equals(id))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("No existe un paquete con id " + id + "."));
		boolean tieneReservas = BackendConsola.RESERVAS.listarTodas().stream()
			.anyMatch(r -> r.getPaqueteTuristico() != null
				&& r.getPaqueteTuristico().getId().toString().equals(paquete.getId()));
		if (tieneReservas) {
			throw new IllegalStateException("No se puede eliminar un paquete que tiene reservas.");
		}
		PAQUETES.remove(paquete);
	}

	/**
	 * Crea el PaqueteTuristico real con los mismos datos, para guardarlo en una Reserva real.
	 * Los ids de la maqueta son números ("1", "2"...), así que sirven como id del paquete real.
	 */
	public PaqueteTuristico aModelo() {
		PaqueteTuristico paquete = new PaqueteTuristico(nombre, descripcion, BigDecimal.valueOf(precio), duracionDias);
		paquete.setId(Long.valueOf(id));
		paquete.setActivo(activo);
		return paquete;
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
