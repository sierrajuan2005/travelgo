package com.travelgo.chatbot;

/**
 * Quién escribe. {@code correo} es el del usuario con sesión iniciada, o null si es un visitante.
 * Las estrategias privadas solo responden cuando hay correo.
 */
public record ContextoChat(String correo) {

	public boolean autenticado() {
		return correo != null && !correo.isBlank();
	}
}
