package com.travelgo.chatbot;

import java.util.List;

/** Lo que una estrategia contesta: el texto y los botones de sugerencia. */
public record RespuestaChatbot(String texto, List<String> sugerencias) {

	/** Opciones que el chatbot siempre sabe manejar. */
	public static final List<String> SUGERENCIAS_BASE = List.of("Paquetes baratos", "Paquetes de lujo",
		"¿Hay cupones?", "¿Cómo pago?", "¿Cómo va mi reserva?");

	public static RespuestaChatbot de(String texto) {
		return new RespuestaChatbot(texto, SUGERENCIAS_BASE);
	}
}
