package com.travelgo.web;

import com.travelgo.util.Textos;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.destinos.Consulta;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.service.InfoDestinoService;
import com.travelgo.service.PaqueteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo web con los paquetes de la base de datos (PaqueteService) y la información real de cada destino
 * (InfoDestinoService: foto, clima y atracciones). Lo puede ver cualquiera, sin iniciar sesión.
 */
@Controller
public class CatalogoWebController {

	private final PaqueteService paqueteService;
	private final InfoDestinoService infoDestinoService;
	private final FormatoWeb formato;

	public CatalogoWebController(PaqueteService paqueteService, InfoDestinoService infoDestinoService,
			FormatoWeb formato) {
		this.paqueteService = paqueteService;
		this.infoDestinoService = infoDestinoService;
		this.formato = formato;
	}

	/**
	 * Paquetes activos, con filtros opcionales por destino (ciudad, país o nombre), precio y duración.
	 */
	@GetMapping("/catalogo")
	public String listar(@RequestParam(required = false) String destino,
			@RequestParam(required = false) BigDecimal precioMin,
			@RequestParam(required = false) BigDecimal precioMax,
			@RequestParam(required = false) Integer duracionMax, Model model) {
		List<PaqueteTuristico> paquetes = filtrar(paqueteService.listar(true), destino, precioMin, precioMax,
			duracionMax);

		// Una foto por paquete (la de su destino principal). Si la API falla, la tarjeta muestra un ícono.
		Map<Long, FotoDestino> fotos = new HashMap<>();
		for (PaqueteTuristico paquete : paquetes) {
			Destino principal = formato.destinoPrincipal(paquete);
			if (principal != null) {
				infoDestinoService.fotoSiSePuede(principal)
					.map(Consulta::datos)
					.ifPresent(foto -> fotos.put(paquete.getId(), foto));
			}
		}

		model.addAttribute("paquetes", paquetes);
		model.addAttribute("fotos", fotos);
		model.addAttribute("destino", destino);
		model.addAttribute("precioMin", precioMin);
		model.addAttribute("precioMax", precioMax);
		model.addAttribute("duracionMax", duracionMax);
		return "catalogo/lista";
	}

	/**
	 * Detalle de un paquete activo con la foto, el clima y las atracciones reales de su destino.
	 */
	@GetMapping("/catalogo/{id}")
	public String detalle(@PathVariable Long id, Model model) {
		PaqueteTuristico paquete = paqueteActivo(id);
		Destino principal = formato.destinoPrincipal(paquete);
		model.addAttribute("paquete", paquete);
		model.addAttribute("destino", principal);
		model.addAttribute("info", principal == null ? null : infoDestinoService.consultar(principal));
		return "catalogo/detalle";
	}

	private PaqueteTuristico paqueteActivo(Long id) {
		try {
			PaqueteTuristico paquete = paqueteService.obtener(id);
			if (!paquete.isActivo()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			}
			return paquete;
		} catch (RecursoNoEncontradoException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}

	/**
	 * Filtro de la interfaz. Cuando Integrante 4 publique la búsqueda en el backend (Sprint 4), este método
	 * se cambia por esa llamada.
	 */
	static List<PaqueteTuristico> filtrar(List<PaqueteTuristico> paquetes, String destino, BigDecimal precioMin,
			BigDecimal precioMax, Integer duracionMax) {
		String texto = Textos.normalizar(destino);
		return paquetes.stream()
			.filter(p -> texto.isEmpty() || p.getDestinos().stream().anyMatch(d ->
				Textos.normalizar(d.getCiudad()).contains(texto)
					|| Textos.normalizar(d.getPais()).contains(texto)
					|| Textos.normalizar(d.getNombre()).contains(texto)))
			.filter(p -> precioMin == null || p.getPrecio().compareTo(precioMin) >= 0)
			.filter(p -> precioMax == null || p.getPrecio().compareTo(precioMax) <= 0)
			.filter(p -> duracionMax == null || p.getDuracionDias() <= duracionMax)
			.toList();
	}

}
