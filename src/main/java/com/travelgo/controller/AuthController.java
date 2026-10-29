package com.travelgo.controller;

import com.travelgo.dto.RegistroDTO;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Locale;
import java.util.UUID;

/**
 * Muestra los formularios de inicio de sesión y registro, y valida el registro.
 */
@Controller
public class AuthController {

	private final UsuarioService usuarioService;
	private final PasswordEncoder passwordEncoder;

	public AuthController(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
		this.usuarioService = usuarioService;
		this.passwordEncoder = passwordEncoder;
	}

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
		if (registro.getPassword() != null
                && !registro.getPassword().equals(registro.getConfirmarPassword())) {
            result.rejectValue("confirmarPassword", "noCoincide",
                    "Las contraseñas no coinciden");
        }

        String correo = registro.getCorreo() == null
                ? ""
                : registro.getCorreo().trim().toLowerCase(Locale.ROOT);

        if (!correo.isBlank()
                && usuarioService.buscarPorCorreo(correo).isPresent()) {
            result.rejectValue("correo", "duplicado",
                    "Ya existe una cuenta con ese correo");
        }

		if (result.hasErrors()) {
			return "auth/registro";
		}

		Usuario usuario = new Usuario(
                UUID.randomUUID().toString(),
                registro.getNombre().trim(),
                correo,
                passwordEncoder.encode(registro.getPassword()),
                RolUsuario.CLIENTE
        );

        usuarioService.guardar(usuario);
		
		return "redirect:/login?registrado";
	}

}
