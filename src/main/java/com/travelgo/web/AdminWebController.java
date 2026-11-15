package com.travelgo.web;

import com.travelgo.dto.HotelRequest;
import com.travelgo.dto.TransporteRequest;
import com.travelgo.dto.VueloRequest;
import com.travelgo.exception.AccesoDatosException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import com.travelgo.service.DestinoService;
import com.travelgo.service.PaqueteService;
import com.travelgo.service.ServicioReservableService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Panel de administración web (solo Administrador: SecurityConfig protege /admin/**).
 * Todo se guarda en PostgreSQL con DestinoService, PaqueteService y ServicioReservableService.
 */
@Controller
@RequestMapping("/admin")
public class AdminWebController {

	private final DestinoService destinoService;
	private final PaqueteService paqueteService;
	private final ServicioReservableService servicioService;

	public AdminWebController(DestinoService destinoService, PaqueteService paqueteService,
			ServicioReservableService servicioService) {
		this.destinoService = destinoService;
		this.paqueteService = paqueteService;
		this.servicioService = servicioService;
	}

	/** Una fila de la tabla de servicios. */
	public record FilaServicio(String id, String tipo, String descripcion, double costo, boolean disponible) {
	}

	@GetMapping
	public String inicio() {
		return "redirect:/admin/destinos";
	}

	// ---------- Destinos ----------

	@GetMapping("/destinos")
	public String destinos(Model model) {
		model.addAttribute("destinos", destinoService.listar().stream()
			.sorted(Comparator.comparing(Destino::getId)).toList());
		return "admin/destinos";
	}

	@PostMapping("/destinos")
	public String agregarDestino(@RequestParam String nombre, @RequestParam(required = false) String descripcion,
			@RequestParam String pais, @RequestParam String ciudad, RedirectAttributes redirect) {
		if (vacio(nombre) || vacio(pais) || vacio(ciudad)) {
			redirect.addFlashAttribute("error", "El nombre, el país y la ciudad son obligatorios.");
			return "redirect:/admin/destinos";
		}
		try {
			Destino destino = destinoService.crear(new Destino(nombre.trim(),
				vacio(descripcion) ? null : descripcion.trim(), pais.trim(), ciudad.trim()));
			redirect.addFlashAttribute("mensaje", "Destino agregado con id " + destino.getId() + ".");
		} catch (AccesoDatosException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/destinos";
	}

	/**
	 * No se elimina un destino que usa algún paquete (la base de datos tampoco lo permite).
	 */
	@PostMapping("/destinos/{id}/eliminar")
	public String eliminarDestino(@PathVariable Long id, RedirectAttributes redirect) {
		boolean enUso = paqueteService.listar(false).stream()
			.anyMatch(p -> p.getDestinos().stream().anyMatch(d -> id.equals(d.getId())));
		if (enUso) {
			redirect.addFlashAttribute("error", "No se puede eliminar el destino " + id
				+ " porque lo usa un paquete. Primero cambia o desactiva ese paquete.");
			return "redirect:/admin/destinos";
		}
		try {
			destinoService.eliminar(id);
			redirect.addFlashAttribute("mensaje", "Destino " + id + " eliminado.");
		} catch (RecursoNoEncontradoException | AccesoDatosException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/destinos";
	}

	// ---------- Paquetes ----------

	@GetMapping("/paquetes")
	public String paquetes(Model model) {
		model.addAttribute("paquetes", paqueteService.listar(false).stream()
			.sorted(Comparator.comparing(PaqueteTuristico::getId)).toList());
		model.addAttribute("destinos", destinoService.listar());
		return "admin/paquetes";
	}

	@PostMapping("/paquetes")
	public String agregarPaquete(@RequestParam String nombre, @RequestParam(required = false) String descripcion,
			@RequestParam(required = false) BigDecimal precio, @RequestParam(required = false) Integer duracionDias,
			@RequestParam(required = false) Long destinoId, @RequestParam(defaultValue = "false") boolean activo,
			RedirectAttributes redirect) {
		if (vacio(nombre) || precio == null || precio.signum() <= 0 || duracionDias == null || duracionDias < 1
			|| destinoId == null) {
			redirect.addFlashAttribute("error",
				"El nombre, el destino, un precio mayor que cero y una duración de al menos 1 día son obligatorios.");
			return "redirect:/admin/paquetes";
		}
		try {
			PaqueteTuristico paquete = new PaqueteTuristico(nombre.trim(),
				vacio(descripcion) ? null : descripcion.trim(), precio, duracionDias);
			paquete.setActivo(activo);
			paquete.setDestinos(List.of(destinoService.obtener(destinoId)));
			PaqueteTuristico guardado = paqueteService.crear(paquete);
			redirect.addFlashAttribute("mensaje", "Paquete agregado con id " + guardado.getId() + ".");
		} catch (RecursoNoEncontradoException | AccesoDatosException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/paquetes";
	}

	/**
	 * Activa o desactiva un paquete. No se borra: las reservas viejas siguen apuntando a él, y un paquete
	 * inactivo deja de salir en el catálogo.
	 */
	@PostMapping("/paquetes/{id}/activo")
	public String cambiarActivo(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			PaqueteTuristico paquete = paqueteService.obtener(id);
			paquete.setActivo(!paquete.isActivo());
			paqueteService.actualizar(id, paquete);
			redirect.addFlashAttribute("mensaje", "El paquete " + id + " ahora está "
				+ (paquete.isActivo() ? "activo (sale en el catálogo)." : "inactivo (ya no sale en el catálogo)."));
		} catch (RecursoNoEncontradoException | AccesoDatosException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/paquetes";
	}

	// ---------- Servicios ----------

	@GetMapping("/servicios")
	public String servicios(@RequestParam(required = false) String tipo, Model model) {
		List<FilaServicio> filas = servicioService.listar(false).stream()
			.filter(s -> vacio(tipo) || FormatoWeb.tipo(s).equalsIgnoreCase(tipo))
			.sorted(Comparator.comparing(FormatoWeb::tipo).thenComparing(ServicioReservable::getId))
			.map(s -> new FilaServicio(s.getId(), FormatoWeb.tipo(s), FormatoWeb.describir(s), s.calcularCosto(),
				s.isDisponible()))
			.toList();
		model.addAttribute("servicios", filas);
		model.addAttribute("tipo", tipo);
		return "admin/servicios";
	}

	/**
	 * Marca un servicio como disponible o no disponible. Las clases de los servicios son inmutables, así que
	 * se guarda una copia con el mismo id (igual que hace ServicioReservableService al actualizar).
	 */
	@PostMapping("/servicios/{id}/disponibilidad")
	public String cambiarDisponibilidad(@PathVariable String id, @RequestParam(required = false) String tipo,
			RedirectAttributes redirect) {
		try {
			ServicioReservable servicio = servicioService.obtener(id);
			boolean nuevo = !servicio.isDisponible();
			switch (servicio) {
				case Vuelo v -> servicioService.actualizarVuelo(id, new VueloRequest(v.getPrecio(), nuevo,
					v.getAerolinea(), v.getOrigen(), v.getDestino(), v.getFechaSalida(), v.getFechaLlegada()));
				case Hotel h -> servicioService.actualizarHotel(id, new HotelRequest(h.getPrecio(), nuevo,
					h.getNombre(), h.getDireccion(), h.getTipoHabitacion(), h.getNoches()));
				case Transporte t -> servicioService.actualizarTransporte(id, new TransporteRequest(t.getPrecio(),
					nuevo, t.getTipo(), t.getOrigen(), t.getDestino(), t.getFecha()));
				default -> throw new IllegalArgumentException("Tipo de servicio desconocido: " + id);
			}
			redirect.addFlashAttribute("mensaje", "El servicio " + id + " ahora está "
				+ (nuevo ? "disponible." : "no disponible."));
		} catch (RecursoNoEncontradoException | AccesoDatosException | IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		// Solo se repite el filtro si es uno de los tres tipos (no se copia texto libre a la URL)
		return TIPOS.stream().filter(t -> t.equalsIgnoreCase(tipo)).findFirst()
			.map(t -> "redirect:/admin/servicios?tipo=" + t)
			.orElse("redirect:/admin/servicios");
	}

	private static final List<String> TIPOS = List.of("Vuelo", "Hotel", "Transporte");

	private static boolean vacio(String texto) {
		return texto == null || texto.isBlank();
	}
}
