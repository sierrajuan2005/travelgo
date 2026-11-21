package com.travelgo.chatbot;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoint del chatbot. Lo usa el widget de la web (Integrante 3) y servirá para otros canales.
 * <pre>
 * POST /api/chatbot                      {"sesionId": null, "mensaje": "paquetes baratos para Bogotá"}
 * GET  /api/chatbot/{sesionId}/mensajes  historial de la conversación
 * </pre>
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

	private final ChatbotService service;

	public ChatbotController(ChatbotService service) {
		this.service = service;
	}

	@PostMapping
	public RespuestaChat conversar(@Valid @RequestBody ChatRequest request, Authentication autenticacion) {
		return service.responder(request.sesionId(), request.mensaje(), correo(autenticacion), CanalChat.WEB);
	}

	@GetMapping("/{sesionId}/mensajes")
	public List<Mensaje> historial(@PathVariable String sesionId, Authentication autenticacion) {
		return service.historial(sesionId, correo(autenticacion));
	}

	private String correo(Authentication autenticacion) {
		return autenticacion == null ? null : autenticacion.getName();
	}
}
