package com.travelgo.chatbot;

import java.time.LocalDateTime;

/** Una conversación. {@code usuarioId} es null si quien escribió no había iniciado sesión. */
public record ChatSesion(String id, String usuarioId, CanalChat canal, LocalDateTime creadaEn,
		LocalDateTime ultimaActividad) {
}
