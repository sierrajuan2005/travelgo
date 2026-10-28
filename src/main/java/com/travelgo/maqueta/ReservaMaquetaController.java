package com.travelgo.maqueta;

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

	/**
	 * Muestra el formulario de reserva con los vuelos, hoteles y transportes disponibles del paquete.
	 */
	@GetMapping("/reserva")
	public String formulario(@RequestParam(name = "paquete", required = false) String paqueteId, Model model) {
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
	 * Recibe la reserva. Todavía no la guarda.
	 */
	@PostMapping("/reserva")
	public String confirmar() {
		// TODO: crear la Reserva con ReservaService cuando Integrante 2 la mergee a develop
		return "redirect:/catalogo?reservado";
	}

}
