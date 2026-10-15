package com.travelgo.travelgo.maqueta;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Maqueta del catálogo con datos de ejemplo.
 * En el Sprint 2 se reemplaza por el PaqueteController de Integrante 2.
 */
@Controller
public class CatalogoMaquetaController {

	/**
	 * Lista los paquetes activos, filtrados por destino y rango de precios si se envían.
	 */
	@GetMapping("/catalogo")
	public String listar(@RequestParam(required = false) String destino,
			@RequestParam(required = false) Double precioMin,
			@RequestParam(required = false) Double precioMax, Model model) {
		model.addAttribute("paquetes", PaqueteMaqueta.filtrar(destino, precioMin, precioMax));
		model.addAttribute("destino", destino);
		model.addAttribute("precioMin", precioMin);
		model.addAttribute("precioMax", precioMax);
		return "catalogo/lista";
	}

	/**
	 * Muestra el detalle de un paquete o la página 404 si no existe.
	 */
	@GetMapping("/catalogo/{id}")
	public String detalle(@PathVariable String id, Model model) {
		PaqueteMaqueta paquete = PaqueteMaqueta.buscar(id);
		if (paquete == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		model.addAttribute("paquete", paquete);
		return "catalogo/detalle";
	}

}
