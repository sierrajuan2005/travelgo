package com.travelgo.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lo que envía el cliente a /api/chatbot. {@code sesionId} es null en el primer mensaje; después
 * se reenvía el que devolvió el servidor.
 */
public record ChatRequest(
		String sesionId,
		@NotBlank(message = "Escribe un mensaje")
		@Size(max = ChatbotService.MAX_MENSAJE, message = "El mensaje es demasiado largo")
		String mensaje) {
}
