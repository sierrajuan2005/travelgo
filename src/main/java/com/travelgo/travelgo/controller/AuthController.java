package com.travelgo.travelgo.controller;

import com.travelgo.travelgo.dto.RegistroDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

	// El POST /login lo procesa Spring Security; aquí solo se muestra el formulario
	@GetMapping("/login")
	public String mostrarLogin() {
		return "auth/login";
	}

	@GetMapping("/registro")
	public String mostrarRegistro(Model model) {
		model.addAttribute("registro", new RegistroDTO());
		return "auth/registro";
	}

	@PostMapping("/registro")
	public String registrar(@Valid @ModelAttribute("registro") RegistroDTO registro, BindingResult result) {
		if (!result.hasFieldErrors("confirmarPassword")
				&& registro.getPassword() != null
				&& !registro.getPassword().equals(registro.getConfirmarPassword())) {
			result.rejectValue("confirmarPassword", "noCoincide", "Las contraseñas no coinciden");
		}

		if (result.hasErrors()) {
			return "auth/registro";
		}

		// TODO: guardar el usuario con UsuarioService cuando Integrante 1 lo mergee a develop
		return "redirect:/login?registrado";
	}

}
