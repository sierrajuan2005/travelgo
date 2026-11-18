package com.travelgo.controller;

import com.travelgo.model.Destino;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.destinos.Consulta;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.service.InfoDestinoService;
import com.travelgo.service.PaqueteService;
import com.travelgo.web.FormatoWeb;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Página de inicio: carrusel con fotos reales de los destinos (Wikimedia Commons) y paquetes destacados.
 * Si una foto no se puede consultar, ese destino no sale en el carrusel; si no hay ninguna, la portada
 * usa un degradado.
 */
@Controller
public class HomeController {

	/** Fotos del carrusel (el diseño pide de 3 a 4). */
	static final int MAX_DIAPOSITIVAS = 4;
	static final int MAX_DESTACADOS = 3;

	private final PaqueteService paqueteService;
	private final InfoDestinoService infoDestinoService;
	private final FormatoWeb formato;

	public HomeController(PaqueteService paqueteService, InfoDestinoService infoDestinoService, FormatoWeb formato) {
		this.paqueteService = paqueteService;
		this.infoDestinoService = infoDestinoService;
		this.formato = formato;
	}

	/**
	 * Una foto del carrusel o una tarjeta de paquete destacado.
	 *
	 * @param credito autor y licencia de la foto (se muestra al pasar el mouse por el ícono de información)
	 */
	public record Vitrina(Long paqueteId, String paquete, String ciudad, String pais, BigDecimal precio,
			int dias, String foto, String credito) {
	}

	@GetMapping("/")
	public String inicio(Model model) {
		List<Vitrina> conFoto = new ArrayList<>();
		List<Long> ciudadesVistas = new ArrayList<>();
		List<Vitrina> diapositivas = new ArrayList<>();

		// Los paquetes más baratos primero: así la portada muestra "desde $ ..."
		List<PaqueteTuristico> activos = paqueteService.listar(true).stream()
			.sorted(Comparator.comparing(PaqueteTuristico::getPrecio))
			.toList();
		for (PaqueteTuristico paquete : activos) {
			Destino destino = formato.destinoPrincipal(paquete);
			if (destino == null) {
				continue;
			}
			Optional<FotoDestino> foto = infoDestinoService.fotoSiSePuede(destino).map(Consulta::datos);
			if (foto.isEmpty()) {
				continue;
			}
			Vitrina vitrina = new Vitrina(paquete.getId(), paquete.getNombre(), destino.getCiudad(), destino.getPais(),
				paquete.getPrecio(), paquete.getDuracionDias(), foto.get().url(), credito(foto.get()));
			conFoto.add(vitrina);
			// Una diapositiva por ciudad, para que el carrusel no repita la misma foto
			if (!ciudadesVistas.contains(destino.getId()) && diapositivas.size() < MAX_DIAPOSITIVAS) {
				ciudadesVistas.add(destino.getId());
				diapositivas.add(vitrina);
			}
		}

		model.addAttribute("diapositivas", diapositivas);
		model.addAttribute("destacados", conFoto.stream().limit(MAX_DESTACADOS).toList());
		model.addAttribute("totalPaquetes", activos.size());
		return "index";
	}

	/** "Foto: autor · licencia (Wikimedia Commons)": la licencia CC BY pide citar al autor. */
	static String credito(FotoDestino foto) {
		StringBuilder texto = new StringBuilder("Foto: ").append(foto.autor());
		if (foto.licencia() != null) {
			texto.append(" · ").append(foto.licencia());
		}
		return texto.append(" (Wikimedia Commons)").toString();
	}
}
