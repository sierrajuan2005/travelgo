package com.travelgo.maqueta;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Versión web del panel de administración de la consola: listar, agregar y eliminar destinos y paquetes, y cambiar
 * la disponibilidad de los servicios de un paquete. Usa los mismos datos de ejemplo que la consola.
 * Solo entra el Administrador: SecurityConfig protege /admin/**.
 * Si una regla no se cumple (dato inválido o registro en uso), vuelve a la lista con el motivo.
 */
@Controller
@RequestMapping("/admin")
public class AdminMaquetaController {

	@GetMapping
	public String inicio() {
		return "redirect:/admin/destinos";
	}

	@GetMapping("/destinos")
	public String destinos(Model model) {
		model.addAttribute("destinos", DestinoMaqueta.todos());
		return "admin/destinos";
	}

	@PostMapping("/destinos")
	public String agregarDestino(@RequestParam String nombre, @RequestParam String descripcion,
			@RequestParam String pais, @RequestParam String ciudad, RedirectAttributes redirect) {
		try {
			DestinoMaqueta destino = DestinoMaqueta.agregar(nombre, descripcion, pais, ciudad);
			redirect.addFlashAttribute("mensaje", "Destino agregado con id " + destino.getId() + ".");
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/destinos";
	}

	@PostMapping("/destinos/{id}/eliminar")
	public String eliminarDestino(@PathVariable String id, RedirectAttributes redirect) {
		try {
			DestinoMaqueta.eliminar(id);
			redirect.addFlashAttribute("mensaje", "Destino " + id + " eliminado.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/destinos";
	}

	@GetMapping("/paquetes")
	public String paquetes(Model model) {
		model.addAttribute("paquetes", PaqueteMaqueta.todos());
		model.addAttribute("destinos", DestinoMaqueta.todos());
		return "admin/paquetes";
	}

	/**
	 * Agrega un paquete. Si falta el precio o la duración se toman como 0, y PaqueteMaqueta.agregar explica el error.
	 */
	@PostMapping("/paquetes")
	public String agregarPaquete(@RequestParam String nombre, @RequestParam String descripcion,
			@RequestParam(required = false) Double precio, @RequestParam(required = false) Integer duracionDias,
			@RequestParam String destinoId, @RequestParam(defaultValue = "false") boolean activo,
			RedirectAttributes redirect) {
		try {
			PaqueteMaqueta paquete = PaqueteMaqueta.agregar(nombre, descripcion, precio == null ? 0 : precio,
				duracionDias == null ? 0 : duracionDias, activo, DestinoMaqueta.buscar(destinoId));
			redirect.addFlashAttribute("mensaje", "Paquete agregado con id " + paquete.getId() + ".");
		} catch (IllegalArgumentException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/paquetes";
	}

	@PostMapping("/paquetes/{id}/eliminar")
	public String eliminarPaquete(@PathVariable String id, RedirectAttributes redirect) {
		try {
			PaqueteMaqueta.eliminar(id);
			redirect.addFlashAttribute("mensaje", "Paquete " + id + " eliminado.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/admin/paquetes";
	}

	/**
	 * Muestra los servicios del paquete elegido (o del primero activo). Los servicios no disponibles no aparecen al
	 * armar una reserva.
	 */
	@GetMapping("/servicios")
	public String servicios(@RequestParam(required = false) String paquete, Model model) {
		PaqueteMaqueta elegido = paquete == null ? null : PaqueteMaqueta.buscar(paquete);
		if (elegido == null && !PaqueteMaqueta.activos().isEmpty()) {
			elegido = PaqueteMaqueta.activos().get(0);
		}
		model.addAttribute("paquetes", PaqueteMaqueta.activos());
		model.addAttribute("paquete", elegido);
		model.addAttribute("servicios", elegido == null ? null : ServiciosPaqueteMaqueta.de(elegido).todos());
		return "admin/servicios";
	}

	@PostMapping("/servicios/{paqueteId}/{servicioId}/disponibilidad")
	public String cambiarDisponibilidad(@PathVariable String paqueteId, @PathVariable String servicioId,
			RedirectAttributes redirect) {
		PaqueteMaqueta paquete = PaqueteMaqueta.buscar(paqueteId);
		ServicioReservableMaqueta servicio = paquete == null ? null : ServiciosPaqueteMaqueta.de(paquete).buscar(servicioId);
		if (servicio == null) {
			redirect.addFlashAttribute("error", "No existe un servicio con ese id en este paquete.");
		} else {
			servicio.cambiarDisponibilidad();
			redirect.addFlashAttribute("mensaje", "El servicio " + servicio.getId() + " ahora está "
				+ (servicio.isDisponible() ? "disponible." : "no disponible."));
		}
		return "redirect:/admin/servicios?paquete=" + paqueteId;
	}

}
