package com.travelgo.chatbot;

import java.util.List;

/** Respuesta del endpoint /api/chatbot. */
public record RespuestaChat(String sesionId, String respuesta, List<String> sugerencias) {
}
