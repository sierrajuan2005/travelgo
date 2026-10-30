package com.travelgo.maqueta;

import com.travelgo.dto.PagoRequest;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Versión web de las opciones "Armar una reserva" y "Pagar una reserva" de la consola: formulario de reserva,
 * mis reservas, pago y factura. Usa el mismo backend que la consola (BackendConsola), con los services reales de
 * pagos y facturas, y los permisos de docs/matriz_permisos.md:
 * el Cliente ve y paga solo sus reservas, el Agente las de todos y el Administrador no reserva ni paga.
 */
@Controller
public class ReservaMaquetaController {

	private final UsuarioService usuarioService;

	public ReservaMaquetaController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	/**
	 * Una fila de la tabla de reservas.
	 */
	public record FilaReserva(String id, String idCorto, String paquete, String cliente, double total,
			String estado, boolean pagada) {
	}

	/**
	 * Una línea de la factura: un servicio y su costo.
	 */
	public record LineaFactura(String descripcion, double valor) {
	}

	/**
	 * Muestra el formulario de reserva con los vuelos, hoteles y transportes disponibles del paquete.
	 */
	@GetMapping("/reserva")
	public String formulario(@RequestParam(name = "paquete", required = false) String paqueteId,
			Authentication autenticacion, Model model) {
		usuarioQueReserva(autenticacion);
		PaqueteMaqueta paquete = paqueteId == null ? null : PaqueteMaqueta.buscar(paqueteId);
		if (paquete == null) {
			return "redirect:/catalogo";
		}

		ServiciosPaqueteMaqueta servicios = ServiciosPaqueteMaqueta.de(paquete);
		model.addAttribute("paquete", paquete);
		model.addAttribute("vuelos", servicios.vuelosDisponibles());
		model.addAttribute("hoteles", servicios.hotelesDisponibles());
		model.addAttribute("transportes", servicios.transportesDisponibles());
		return "reserva/formulario";
	}

	/**
	 * Guarda la reserva PENDIENTE del usuario que inició sesión con el paquete y los servicios elegidos.
	 * Un id vacío significa "sin ese servicio".
	 */
	@PostMapping("/reserva")
	public String guardar(@RequestParam String paqueteId,
			@RequestParam(required = false) String vueloId,
			@RequestParam(required = false) String hotelId,
			@RequestParam(required = false) String transporteId,
			Authentication autenticacion, RedirectAttributes redirect) {
		Usuario usuario = usuarioQueReserva(autenticacion);
		PaqueteMaqueta paquete = PaqueteMaqueta.buscar(paqueteId);
		if (paquete == null) {
			return "redirect:/catalogo";
		}

		ServiciosPaqueteMaqueta serviciosPaquete = ServiciosPaqueteMaqueta.de(paquete);
		List<ServicioReservableMaqueta> servicios = new ArrayList<>();
		for (String id : new String[] {vueloId, hotelId, transporteId}) {
			if (id != null && !id.isBlank()) {
				ServicioReservableMaqueta servicio = serviciosPaquete.buscar(id);
				if (servicio != null) {
					servicios.add(servicio);
				}
			}
		}

		try {
			Reserva reserva = BackendConsola.guardarReserva(usuario, paquete, servicios);
			redirect.addFlashAttribute("mensaje", "Reserva " + FormatoMaqueta.idCorto(reserva.getId())
				+ " guardada (" + reserva.getEstado() + "). Ya puedes pagarla.");
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", "No se pudo guardar la reserva: " + e.getMessage());
			return "redirect:/reserva?paquete=" + paquete.getId();
		}
		return "redirect:/reservas";
	}

	/**
	 * Lista las reservas que el usuario puede ver. El Agente ve también el nombre del cliente.
	 */
	@GetMapping("/reservas")
	public String listar(Authentication autenticacion, Model model) {
		Usuario usuario = usuarioQueReserva(autenticacion);
		List<FilaReserva> filas = BackendConsola.reservasVisibles(usuario).stream()
			.map(reserva -> new FilaReserva(reserva.getId(), FormatoMaqueta.idCorto(reserva.getId()),
				FormatoMaqueta.nombrePaquete(reserva), reserva.getUsuario().getNombre(), reserva.getTotal(),
				reserva.getEstado(), BackendConsola.buscarFactura(reserva).isPresent()))
			.toList();
		model.addAttribute("reservas", filas);
		model.addAttribute("esAgente", usuario.getRol() == RolUsuario.AGENTE);
		return "reserva/lista";
	}

	/**
	 * Muestra el total y los métodos de pago. Si la reserva ya está pagada, muestra su factura.
	 */
	@GetMapping("/reservas/{id}/pagar")
	public String formularioPago(@PathVariable String id, Authentication autenticacion, Model model) {
		Reserva reserva = reservaVisible(autenticacion, id);
		if (BackendConsola.buscarFactura(reserva).isPresent()) {
			return "redirect:/reservas/" + id + "/factura";
		}
		model.addAttribute("reserva", reserva);
		model.addAttribute("idCorto", FormatoMaqueta.idCorto(reserva.getId()));
		model.addAttribute("paquete", FormatoMaqueta.nombrePaquete(reserva));
		model.addAttribute("lineas", lineas(reserva));
		model.addAttribute("metodos", metodosDePago());
		return "reserva/pago";
	}

	/**
	 * Paga la reserva con PagoService, con las mismas reglas que /api/pagos: una reserva se paga una sola vez, no se
	 * paga una reserva cancelada, y el pago confirma la reserva y genera la factura.
	 */
	@PostMapping("/reservas/{id}/pagar")
	public String pagar(@PathVariable String id, @RequestParam(required = false) MetodoPago metodo,
			Authentication autenticacion, RedirectAttributes redirect) {
		Reserva reserva = reservaVisible(autenticacion, id);
		if (metodo == null) {
			redirect.addFlashAttribute("error", "Elige un método de pago.");
			return "redirect:/reservas/" + id + "/pagar";
		}
		try {
			BackendConsola.PAGOS.pagar(new PagoRequest(reserva.getId(), metodo));
		} catch (IllegalArgumentException | IllegalStateException e) {
			redirect.addFlashAttribute("error", "No se pudo pagar: " + e.getMessage());
			return "redirect:/reservas/" + id + "/pagar";
		}
		redirect.addFlashAttribute("mensaje", "Pago registrado. Esta es tu factura.");
		return "redirect:/reservas/" + id + "/factura";
	}

	/**
	 * Muestra la factura: datos de la reserva, del cliente, cada servicio, el total y el pago.
	 */
	@GetMapping("/reservas/{id}/factura")
	public String factura(@PathVariable String id, Authentication autenticacion, Model model,
			RedirectAttributes redirect) {
		Reserva reserva = reservaVisible(autenticacion, id);
		Optional<Factura> factura = BackendConsola.buscarFactura(reserva);
		if (factura.isEmpty()) {
			redirect.addFlashAttribute("error", "La reserva todavía no tiene factura. Primero hay que pagarla.");
			return "redirect:/reservas";
		}
		Optional<Pago> pago = BackendConsola.buscarPago(reserva);
		model.addAttribute("factura", factura.get());
		model.addAttribute("reserva", reserva);
		model.addAttribute("idCorto", FormatoMaqueta.idCorto(reserva.getId()));
		model.addAttribute("paquete", FormatoMaqueta.nombrePaquete(reserva));
		model.addAttribute("lineas", lineas(reserva));
		model.addAttribute("pago", pago.orElse(null));
		model.addAttribute("metodo", pago.map(p -> FormatoMaqueta.nombreMetodo(p.getMetodo())).orElse(null));
		return "reserva/factura";
	}

	/**
	 * Usuario que inició sesión, si su rol puede reservar y pagar.
	 *
	 * @throws AccessDeniedException si es Administrador (Spring Security muestra la página 403)
	 */
	private Usuario usuarioQueReserva(Authentication autenticacion) {
		Usuario usuario = usuarioService.buscarPorCorreo(autenticacion.getName())
			.orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
		if (!BackendConsola.puedeReservar(usuario)) {
			throw new AccessDeniedException("El rol " + usuario.getRol() + " no reserva ni paga.");
		}
		return usuario;
	}

	/**
	 * Reserva que el usuario puede ver. Si no existe o es de otro cliente responde 404, para no revelar que existe.
	 */
	private Reserva reservaVisible(Authentication autenticacion, String id) {
		return BackendConsola.buscarReservaVisible(usuarioQueReserva(autenticacion), id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
	}

	private List<LineaFactura> lineas(Reserva reserva) {
		return reserva.getServicios().stream()
			.map(servicio -> new LineaFactura(FormatoMaqueta.describir(servicio), servicio.calcularCosto()))
			.toList();
	}

	private Map<MetodoPago, String> metodosDePago() {
		Map<MetodoPago, String> metodos = new LinkedHashMap<>();
		for (MetodoPago metodo : MetodoPago.values()) {
			metodos.put(metodo, FormatoMaqueta.nombreMetodo(metodo));
		}
		return metodos;
	}

}
