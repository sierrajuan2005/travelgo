package com.travelgo.chatbot;

import java.time.LocalDateTime;

/** Un mensaje de una conversación. {@code emisor} es USUARIO o BOT. */
public record Mensaje(String id, String sesionId, String emisor, String texto, String intencion,
		LocalDateTime fecha) {

	public static final String USUARIO = "USUARIO";
	public static final String BOT = "BOT";
}
