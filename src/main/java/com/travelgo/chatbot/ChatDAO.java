package com.travelgo.chatbot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Contrato de acceso a datos de las sesiones y mensajes del chatbot. */
public interface ChatDAO {

	ChatSesion crearSesion(ChatSesion sesion);

	Optional<ChatSesion> buscarSesion(String id);

	void actualizarActividad(String sesionId, LocalDateTime fecha);

	void guardarMensaje(Mensaje mensaje);

	List<Mensaje> listarMensajes(String sesionId);
}
