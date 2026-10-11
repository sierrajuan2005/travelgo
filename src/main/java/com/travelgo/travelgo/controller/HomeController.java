package com.travelgo.travelgo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Muestra la página de inicio.
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String inicio() {
		return "index";
	}

}
