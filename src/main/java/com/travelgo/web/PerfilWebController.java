package com.travelgo.web;

import com.travelgo.model.Usuario;
import com.travelgo.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

/**
 * "Mis avisos": el usuario con sesión cambia su celular y si acepta recordatorios por correo y por SMS
 * (Sprint 5, Integrante 3). Los campos del usuario los agregó el Integrante 1; aquí solo se editan.
 */
@Controller
public class PerfilWebController {

	/** Números, espacios, guiones, paréntesis y un "+" al inicio; entre 7 y 20 caracteres. */
	private static final Pattern TELEFONO = Pattern.compile("\\+?[0-9 ()-]{7,20}");

	private final UsuarioService usuarioService;

	public PerfilWebController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	@GetMapping("/perfil/avisos")
	public String verAvisos(Authentication auth, Model model) {
		model.addAttribute("usuario", usuarioActual(auth));
		return "perfil/avisos";
	}

	@PostMapping("/perfil/avisos")
	public String guardarAvisos(Authentication auth,
			@RequestParam(required = false) String telefono,
			@RequestParam(defaultValue = "false") boolean notificacionesCorreo,
			@RequestParam(defaultValue = "false") boolean notificacionesSms,
			RedirectAttributes flash) {
		Usuario usuario = usuarioActual(auth);
		String limpio = telefono == null ? "" : telefono.trim();

		if (!limpio.isEmpty() && !TELEFONO.matcher(limpio).matches()) {
			flash.addFlashAttribute("error", "El celular solo puede tener números, espacios y un \"+\" al inicio.");
			return "redirect:/perfil/avisos";
		}
		if (notificacionesSms && limpio.isEmpty()) {
			flash.addFlashAttribute("error", "Para recibir mensajes de texto escribe tu celular.");
			return "redirect:/perfil/avisos";
		}

		// Primero el teléfono: Usuario no deja activar el SMS sin teléfono.
		usuario.setTelefono(limpio);
		usuario.setNotificacionesCorreo(notificacionesCorreo);
		usuario.setNotificacionesSms(notificacionesSms);
		usuarioService.guardar(usuario);

		flash.addFlashAttribute("mensaje", "Guardamos tus preferencias de aviso.");
		return "redirect:/perfil/avisos";
	}

	private Usuario usuarioActual(Authentication auth) {
		return usuarioService.buscarPorCorreo(auth.getName())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión de nuevo."));
	}
}
