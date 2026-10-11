package com.travelgo.travelgo.maqueta;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maqueta del catálogo con datos de ejemplo.
 * En el Sprint 2 se reemplaza por el PaqueteController de Integrante 2.
 */
@Controller
public class CatalogoMaquetaController {

	@GetMapping("/catalogo")
	public String listar(Model model) {
		model.addAttribute("paquetes", PaqueteMaqueta.todos());
		return "catalogo/lista";
	}

	@GetMapping("/catalogo/{id}")
	public String detalle(@PathVariable Long id, Model model) {
		PaqueteMaqueta paquete = PaqueteMaqueta.buscar(id);
		if (paquete == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("paquete", paquete);
		return "catalogo/detalle";
	}

}
