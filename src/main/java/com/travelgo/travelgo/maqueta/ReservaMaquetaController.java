package com.travelgo.travelgo.maqueta;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Maqueta del flujo de reserva con datos de ejemplo. No guarda nada.
 * En el Sprint 2 se reemplaza por el ReservaController de Integrante 2.
 */
@Controller
public class ReservaMaquetaController {

	@GetMapping("/reserva")
	public String formulario(@RequestParam(name = "paquete", required = false) Long paqueteId, Model model) {
		PaqueteMaqueta paquete = paqueteId == null ? null : PaqueteMaqueta.buscar(paqueteId);
		if (paquete == null) {
			return "redirect:/catalogo";
		}

		model.addAttribute("paquete", paquete);
		model.addAttribute("vuelos", OpcionServicioMaqueta.vuelos(paquete.getDestino()));
		model.addAttribute("hoteles", OpcionServicioMaqueta.hoteles(paquete.getDestino()));
		model.addAttribute("transportes", OpcionServicioMaqueta.transportes(paquete.getDestino()));
		return "reserva/formulario";
	}

	@PostMapping("/reserva")
	public String confirmar() {
		// TODO: crear la Reserva con ReservaService cuando Integrante 2 la mergee a develop
		return "redirect:/catalogo?reservado";
	}

}
