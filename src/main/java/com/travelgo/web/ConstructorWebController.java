package com.travelgo.web;

import com.travelgo.exception.AccesoDatosException;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.service.CodigosDestino;
import com.travelgo.service.ConstructorPaqueteService;
import com.travelgo.service.ConstructorPaqueteService.Solicitud;
import com.travelgo.service.DestinoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla del constructor de paquetes con datos reales (wireframe 7.5). Solo Administrador
 * (SecurityConfig protege /admin/**).
 * <ol>
 *   <li>GET sin fechas: formulario de búsqueda.</li>
 *   <li>GET con fechas: busca vuelos y hoteles reales y los muestra para elegir.</li>
 *   <li>POST: guarda el paquete con la copia del vuelo y del hotel elegidos.</li>
 * </ol>
 */
@Controller
@RequestMapping("/admin/constructor")
public class ConstructorWebController {

	private final ConstructorPaqueteService constructor;
	private final DestinoService destinoService;

	public ConstructorWebController(ConstructorPaqueteService constructor, DestinoService destinoService) {
		this.constructor = constructor;
		this.destinoService = destinoService;
	}

	@GetMapping
	public String formulario(@RequestParam(required = false) Long destinoId,
			@RequestParam(required = false) String origen,
			@RequestParam(required = false) String codigoDestino,
			@RequestParam(required = false) String claveHoteles,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ida,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vuelta,
			Model model) {
		List<Destino> destinos = destinoService.listar().stream()
			.sorted(Comparator.comparing(Destino::getId)).toList();
		Map<Long, CodigosDestino.Codigos> codigos = new LinkedHashMap<>();
		for (Destino d : destinos) {
			CodigosDestino.de(d.getCiudad()).ifPresent(c -> codigos.put(d.getId(), c));
		}

		// Valores por defecto: primer destino con códigos conocidos, salida desde Bogotá, viaje en 30 días
		if (destinoId == null && !codigos.isEmpty()) {
			destinoId = codigos.keySet().iterator().next();
		}
		CodigosDestino.Codigos sugeridos = destinoId == null ? null : codigos.get(destinoId);
		Solicitud solicitud = new Solicitud(destinoId,
			vacio(origen) ? "BOG" : origen.trim().toUpperCase(),
			!vacio(codigoDestino) ? codigoDestino.trim().toUpperCase() : sugeridos == null ? "" : sugeridos.iata(),
			!vacio(claveHoteles) ? claveHoteles.trim() : sugeridos == null ? "" : sugeridos.claveHoteles(),
			ida, vuelta);

		model.addAttribute("destinos", destinos);
		model.addAttribute("codigos", codigos);
		model.addAttribute("origenes", CodigosDestino.ORIGENES);
		model.addAttribute("solicitud", solicitud);
		model.addAttribute("idaSugerida", LocalDate.now().plusDays(30));
		model.addAttribute("vueltaSugerida", LocalDate.now().plusDays(34));
		model.addAttribute("tasa", constructor.tasaUsdCop());

		if (ida != null || vuelta != null) {
			try {
				model.addAttribute("resultado", constructor.buscar(solicitud));
			} catch (IllegalArgumentException | RecursoNoEncontradoException e) {
				model.addAttribute("error", e.getMessage());
			}
		}
		return "admin/constructor";
	}

	@PostMapping
	public String guardar(@RequestParam(required = false) Long destinoId,
			@RequestParam(required = false) String origen,
			@RequestParam(required = false) String codigoDestino,
			@RequestParam(required = false) String claveHoteles,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ida,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vuelta,
			@RequestParam(required = false) String vueloId,
			@RequestParam(required = false) String hotelClave,
			@RequestParam(required = false) String nombre,
			@RequestParam(required = false) String descripcion,
			RedirectAttributes redirect) {
		Solicitud solicitud = new Solicitud(destinoId, origen, codigoDestino, claveHoteles, ida, vuelta);
		try {
			PaqueteTuristico paquete = constructor.armar(solicitud, vueloId, hotelClave, nombre, descripcion);
			redirect.addFlashAttribute("mensaje", "Paquete \"" + paquete.getNombre() + "\" guardado con id "
				+ paquete.getId() + ". Ya sale en el catálogo con su precio de referencia.");
			return "redirect:/admin/paquetes";
		} catch (IllegalArgumentException | IllegalStateException | RecursoNoEncontradoException
				| ProveedorNoDisponibleException | AccesoDatosException e) {
			redirect.addFlashAttribute("error", "No se pudo guardar el paquete: " + e.getMessage());
			return "redirect:" + volverABuscar(solicitud);
		}
	}

	/** URL de la misma búsqueda, para volver a mostrar los resultados después de un error. */
	private static String volverABuscar(Solicitud s) {
		return UriComponentsBuilder.fromPath("/admin/constructor")
			.queryParamIfPresent("destinoId", java.util.Optional.ofNullable(s.destinoId()))
			.queryParamIfPresent("origen", java.util.Optional.ofNullable(s.origen()))
			.queryParamIfPresent("codigoDestino", java.util.Optional.ofNullable(s.codigoDestino()))
			.queryParamIfPresent("claveHoteles", java.util.Optional.ofNullable(s.claveHoteles()))
			.queryParamIfPresent("ida", java.util.Optional.ofNullable(s.ida()))
			.queryParamIfPresent("vuelta", java.util.Optional.ofNullable(s.vuelta()))
			.encode().build().toUriString();
	}

	private static boolean vacio(String texto) {
		return texto == null || texto.isBlank();
	}
}
