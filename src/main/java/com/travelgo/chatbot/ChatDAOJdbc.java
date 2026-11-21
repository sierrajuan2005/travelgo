package com.travelgo.chatbot;

import com.travelgo.exception.AccesoDatosException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Implementación JDBC del DAO del chatbot (tablas chat_sesion y mensaje). */
@Repository
public class ChatDAOJdbc implements ChatDAO {

	private final JdbcTemplate jdbc;

	public ChatDAOJdbc(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public ChatSesion crearSesion(ChatSesion sesion) {
		try {
			jdbc.update("""
				INSERT INTO chat_sesion (id, usuario_id, canal, creada_en, ultima_actividad)
				VALUES (?, ?, ?, ?, ?)
				""", sesion.id(), sesion.usuarioId(), sesion.canal().name(),
				Timestamp.valueOf(sesion.creadaEn()), Timestamp.valueOf(sesion.ultimaActividad()));
			return sesion;
		} catch (DataAccessException e) {
			throw new AccesoDatosException("No se pudo crear la sesión del chatbot", e);
		}
	}

	@Override
	public Optional<ChatSesion> buscarSesion(String id) {
		try {
			return jdbc.query("""
				SELECT id, usuario_id, canal, creada_en, ultima_actividad FROM chat_sesion WHERE id = ?
				""", this::mapearSesion, id).stream().findFirst();
		} catch (DataAccessException e) {
			throw new AccesoDatosException("No se pudo consultar la sesión " + id, e);
		}
	}

	@Override
	public void actualizarActividad(String sesionId, LocalDateTime fecha) {
		try {
			jdbc.update("UPDATE chat_sesion SET ultima_actividad = ? WHERE id = ?", Timestamp.valueOf(fecha), sesionId);
		} catch (DataAccessException e) {
			throw new AccesoDatosException("No se pudo actualizar la sesión " + sesionId, e);
		}
	}

	@Override
	public void guardarMensaje(Mensaje mensaje) {
		try {
			jdbc.update("""
				INSERT INTO mensaje (id, sesion_id, emisor, texto, intencion, fecha) VALUES (?, ?, ?, ?, ?, ?)
				""", mensaje.id(), mensaje.sesionId(), mensaje.emisor(), mensaje.texto(), mensaje.intencion(),
				Timestamp.valueOf(mensaje.fecha()));
		} catch (DataAccessException e) {
			throw new AccesoDatosException("No se pudo guardar el mensaje", e);
		}
	}

	@Override
	public List<Mensaje> listarMensajes(String sesionId) {
		try {
			return jdbc.query("""
				SELECT id, sesion_id, emisor, texto, intencion, fecha FROM mensaje
				WHERE sesion_id = ? ORDER BY fecha, id
				""", this::mapearMensaje, sesionId);
		} catch (DataAccessException e) {
			throw new AccesoDatosException("No se pudo listar los mensajes de la sesión " + sesionId, e);
		}
	}

	private ChatSesion mapearSesion(ResultSet rs, int fila) throws SQLException {
		return new ChatSesion(rs.getString("id"), rs.getString("usuario_id"), CanalChat.valueOf(rs.getString("canal")),
			rs.getTimestamp("creada_en").toLocalDateTime(), rs.getTimestamp("ultima_actividad").toLocalDateTime());
	}

	private Mensaje mapearMensaje(ResultSet rs, int fila) throws SQLException {
		return new Mensaje(rs.getString("id"), rs.getString("sesion_id"), rs.getString("emisor"),
			rs.getString("texto"), rs.getString("intencion"), rs.getTimestamp("fecha").toLocalDateTime());
	}
}
