package com.travelgo.web;

import com.travelgo.dto.PagoRequest;
import com.travelgo.dto.ReservaRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.EstadoFactura;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import com.travelgo.service.FacturaService;
import com.travelgo.service.PagoService;
import com.travelgo.service.PaqueteService;
import com.travelgo.service.ReservaService;
import com.travelgo.service.ServicioReservableService;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reservas, pagos y facturas en la web, guardados en PostgreSQL con los services de Spring
 * (ReservaService, PagoService, FacturaService). Sigue docs/matriz_permisos.md:
 * <ul>
 *   <li>Cliente: reserva, ve y paga solo sus reservas.</li>
 *   <li>Agente: reserva, ve y paga las reservas de todos los clientes.</li>
 *   <li>Administrador: "no aplica" (recibe 403).</li>
 * </ul>
 */
@Controller
public class ReservaWebController {

	private final UsuarioService usuarioService;
	private final PaqueteService paqueteService;
	private final ServicioReservableService servicioService;
	private final ReservaService reservaService;
	private final PagoService pagoService;
	private final FacturaService facturaService;

	public ReservaWebController(UsuarioService usuarioService, PaqueteService paqueteService,
			ServicioReservableService servicioService, ReservaService reservaService, PagoService pagoService,
			FacturaService facturaService) {
		this.usuarioService = usuarioService;
		this.paqueteService = paqueteService;
		this.servicioService = servicioService;
		this.reservaService = reservaService;
		this.pagoService = pagoService;
		this.facturaService = facturaService;
	}

	/** Una fila de la tabla de reservas. */
	public record FilaReserva(String id, String idCorto, String paquete, String cliente, double total,
			String estado, boolean pagada) {
	}

	/** Una línea del detalle de pago o de la factura. */
	public record LineaFactura(String descripcion, double valor) {
	}

	/**
	 * Formulario para armar la reserva de un paquete: vuelo, hotel y transporte que llegan a su destino.
	 */
	@GetMapping("/reserva")
	public String formulario(@RequestParam(name = "paquete", required = false) Long paqueteId,
			Authentication autenticacion, Model model) {
		usuarioQueReserva(autenticacion);
		Optional<PaqueteTuristico> paquete = paqueteActivo(paqueteId);
		if (paquete.isEmpty()) {
			return "redirect:/catalogo";
		}
		OfertaPaquete oferta = OfertaPaquete.de(paquete.get(), servicioService.listar(true));
		model.addAttribute("paquete", paquete.get());
		model.addAttribute("vuelos", oferta.vuelos());
		model.addAttribute("hoteles", oferta.hoteles());
		model.addAttribute("transportes", oferta.transportes());
		return "reserva/formulario";
	}

	/**
	 * Guarda la reserva PENDIENTE en la base de datos. El dueño es el usuario que inició sesión
	 * (ReservaService lo busca por el correo autenticado, no por un dato del formulario).
	 */
	@PostMapping("/reserva")
	public String guardar(@RequestParam Long paqueteId,
			@RequestParam(required = false) String vueloId,
			@RequestParam(required = false) String hotelId,
			@RequestParam(required = false) String transporteId,
			Authentication autenticacion, RedirectAttributes redirect) {
		usuarioQueReserva(autenticacion);
		Optional<PaqueteTuristico> paquete = paqueteActivo(paqueteId);
		if (paquete.isEmpty()) {
			return "redirect:/catalogo";
		}
		// Solo se aceptan servicios de la oferta del paquete, aunque alguien cambie el formulario a mano
		OfertaPaquete oferta = OfertaPaquete.de(paquete.get(), servicioService.listar(true));
		List<String> serviciosIds = new ArrayList<>();
		for (String id : new String[] {vueloId, hotelId, transporteId}) {
			if (id != null && !id.isBlank()) {
				oferta.buscar(id).map(ServicioReservable::getId).ifPresent(serviciosIds::add);
			}
		}
		if (serviciosIds.isEmpty()) {
			redirect.addFlashAttribute("error", "Elige al menos un vuelo, un hotel o un transporte.");
			return "redirect:/reserva?paquete=" + paqueteId;
		}
		try {
			Reserva reserva = reservaService.crearReserva(new ReservaRequest(serviciosIds, paqueteId),
				autenticacion.getName());
			redirect.addFlashAttribute("mensaje", "Reserva " + FormatoWeb.idCorto(reserva.getId())
				+ " guardada (" + reserva.getEstado() + "). Ya puedes pagarla.");
		} catch (IllegalArgumentException | IllegalStateException | RecursoNoEncontradoException e) {
			redirect.addFlashAttribute("error", "No se pudo guardar la reserva: " + e.getMessage());
			return "redirect:/reserva?paquete=" + paqueteId;
		}
		return "redirect:/reservas";
	}

	/**
	 * Cliente: sus reservas. Agente: las de todos, con el nombre del cliente.
	 */
	@GetMapping("/reservas")
	public String listar(Authentication autenticacion, Model model) {
		Usuario usuario = usuarioQueReserva(autenticacion);
		boolean esAgente = usuario.getRol() == RolUsuario.AGENTE;
		List<Reserva> reservas = esAgente ? reservaService.listar() : reservaService.listarPropias(usuario.getCorreo());

		Set<String> pagadas = facturaService.listar().stream()
			.filter(f -> f.getEstado() == EstadoFactura.PAGADA)
			.map(f -> f.getReserva().getId())
			.collect(Collectors.toSet());

		List<FilaReserva> filas = reservas.stream()
			.sorted(Comparator.comparing(Reserva::getFechaReserva).reversed())
			.map(r -> new FilaReserva(r.getId(), FormatoWeb.idCorto(r.getId()), FormatoWeb.nombrePaquete(r),
				r.getUsuario().getNombre(), r.getTotal(), r.getEstado(), pagadas.contains(r.getId())))
			.toList();
		model.addAttribute("reservas", filas);
		model.addAttribute("esAgente", esAgente);
		return "reserva/lista";
	}

	@GetMapping("/reservas/{id}/pagar")
	public String formularioPago(@PathVariable String id, Authentication autenticacion, Model model) {
		Reserva reserva = reservaVisible(autenticacion, id);
		if (factura(reserva).filter(f -> f.getEstado() == EstadoFactura.PAGADA).isPresent()) {
			return "redirect:/reservas/" + id + "/factura";
		}
		model.addAttribute("reserva", reserva);
		model.addAttribute("idCorto", FormatoWeb.idCorto(reserva.getId()));
		model.addAttribute("paquete", FormatoWeb.nombrePaquete(reserva));
		model.addAttribute("lineas", lineas(reserva));
		model.addAttribute("metodos", metodosDePago());
		return "reserva/pago";
	}

	/**
	 * Pago simulado con PagoService: confirma la reserva y crea la factura, todo en una transacción.
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
			pagoService.pagar(new PagoRequest(reserva.getId(), metodo));
		} catch (IllegalArgumentException | IllegalStateException | RecursoNoEncontradoException e) {
			redirect.addFlashAttribute("error", "No se pudo pagar: " + e.getMessage());
			return "redirect:/reservas/" + id + "/pagar";
		}
		redirect.addFlashAttribute("mensaje", "Pago registrado. Esta es tu factura.");
		return "redirect:/reservas/" + id + "/factura";
	}

	@GetMapping("/reservas/{id}/factura")
	public String factura(@PathVariable String id, Authentication autenticacion, Model model,
			RedirectAttributes redirect) {
		Reserva reserva = reservaVisible(autenticacion, id);
		Optional<Factura> factura = factura(reserva);
		if (factura.isEmpty()) {
			redirect.addFlashAttribute("error", "La reserva todavía no tiene factura. Primero hay que pagarla.");
			return "redirect:/reservas";
		}
		Optional<Pago> pago = pago(reserva);
		model.addAttribute("factura", factura.get());
		model.addAttribute("reserva", reserva);
		model.addAttribute("idCorto", FormatoWeb.idCorto(reserva.getId()));
		model.addAttribute("paquete", FormatoWeb.nombrePaquete(reserva));
		model.addAttribute("lineas", lineas(reserva));
		model.addAttribute("pago", pago.orElse(null));
		model.addAttribute("metodo", pago.map(p -> FormatoWeb.nombreMetodo(p.getMetodo())).orElse(null));
		return "reserva/factura";
	}

	/**
	 * Usuario que inició sesión, si su rol puede reservar (Cliente o Agente).
	 *
	 * @throws AccessDeniedException si es Administrador o no existe (Spring Security responde 403)
	 */
	private Usuario usuarioQueReserva(Authentication autenticacion) {
		Usuario usuario = usuarioService.buscarPorCorreo(autenticacion.getName())
			.orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
		if (usuario.getRol() != RolUsuario.CLIENTE && usuario.getRol() != RolUsuario.AGENTE) {
			throw new AccessDeniedException("El rol " + usuario.getRol() + " no reserva ni paga.");
		}
		return usuario;
	}

	/**
	 * Reserva que el usuario puede ver: el Agente cualquiera; el Cliente solo las suyas
	 * (ReservaService.obtenerPropia lanza AccessDeniedException, que da 403).
	 */
	private Reserva reservaVisible(Authentication autenticacion, String id) {
		Usuario usuario = usuarioQueReserva(autenticacion);
		try {
			return usuario.getRol() == RolUsuario.AGENTE
				? reservaService.obtener(id)
				: reservaService.obtenerPropia(id, usuario.getCorreo());
		} catch (RecursoNoEncontradoException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	private Optional<PaqueteTuristico> paqueteActivo(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(paqueteService.obtener(id)).filter(PaqueteTuristico::isActivo);
		} catch (RecursoNoEncontradoException e) {
			return Optional.empty();
		}
	}

	private Optional<Factura> factura(Reserva reserva) {
		try {
			return Optional.of(facturaService.obtenerPorReserva(reserva.getId()));
		} catch (RecursoNoEncontradoException e) {
			return Optional.empty();
		}
	}

	private Optional<Pago> pago(Reserva reserva) {
		try {
			return Optional.of(pagoService.obtenerPorReserva(reserva.getId()));
		} catch (RecursoNoEncontradoException e) {
			return Optional.empty();
		}
	}

	private List<LineaFactura> lineas(Reserva reserva) {
		return reserva.getServicios().stream()
			.map(servicio -> new LineaFactura(FormatoWeb.describir(servicio), servicio.calcularCosto()))
			.toList();
	}

	private Map<MetodoPago, String> metodosDePago() {
		Map<MetodoPago, String> metodos = new LinkedHashMap<>();
		for (MetodoPago metodo : MetodoPago.values()) {
			metodos.put(metodo, FormatoWeb.nombreMetodo(metodo));
		}
		return metodos;
	}
}
