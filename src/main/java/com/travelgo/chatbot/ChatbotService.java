package com.travelgo.chatbot;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * El motor del chatbot: abre o retoma la sesión, entiende el mensaje, elige la primera estrategia que
 * aplica, y guarda el mensaje y la respuesta. Los datos privados salen solo si hay un usuario con sesión.
 */
@Service
public class ChatbotService {

	public static final int MAX_MENSAJE = 500;

	private final ChatDAO chatDAO;
	private final UsuarioDAO usuarioDAO;
	private final DestinoDAO destinoDAO;
	private final ExtractorDatos extractor;
	private final List<EstrategiaChatbot> estrategias;

	/** Spring entrega las estrategias ordenadas por {@code @Order}; la de "no entendida" va de última. */
	public ChatbotService(ChatDAO chatDAO, UsuarioDAO usuarioDAO, DestinoDAO destinoDAO, ExtractorDatos extractor,
			List<EstrategiaChatbot> estrategias) {
		this.chatDAO = chatDAO;
		this.usuarioDAO = usuarioDAO;
		this.destinoDAO = destinoDAO;
		this.extractor = extractor;
		this.estrategias = List.copyOf(estrategias);
	}

	/**
	 * @param sesionId id devuelto antes por el servidor, o null en el primer mensaje
	 * @param correo   correo del usuario con sesión iniciada, o null si es un visitante
	 */
	public RespuestaChat responder(String sesionId, String mensaje, String correo, CanalChat canal) {
		if (mensaje == null || mensaje.isBlank()) {
			throw new IllegalArgumentException("Escribe un mensaje.");
		}
		if (mensaje.length() > MAX_MENSAJE) {
			throw new IllegalArgumentException("El mensaje es demasiado largo (máximo " + MAX_MENSAJE + " caracteres).");
		}
		String usuarioId = usuarioId(correo);
		ChatSesion sesion = sesionDe(sesionId, usuarioId, canal);

		DatosMensaje datos = extractor.extraer(mensaje, nombresDeDestinos());
		EstrategiaChatbot estrategia = estrategias.stream().filter(e -> e.aplica(datos)).findFirst()
			.orElseThrow(() -> new IllegalStateException("No hay una estrategia para responder"));
		RespuestaChatbot respuesta = estrategia.responder(datos, new ContextoChat(correo));

		LocalDateTime ahora = LocalDateTime.now();
		chatDAO.guardarMensaje(new Mensaje(UUID.randomUUID().toString(), sesion.id(), Mensaje.USUARIO, mensaje,
			estrategia.nombre(), ahora));
		// un microsegundo después, para que el orden de la conversación quede estable
		LocalDateTime despues = ahora.plusNanos(1000);
		chatDAO.guardarMensaje(new Mensaje(UUID.randomUUID().toString(), sesion.id(), Mensaje.BOT, respuesta.texto(),
			estrategia.nombre(), despues));
		chatDAO.actualizarActividad(sesion.id(), despues);

		return new RespuestaChat(sesion.id(), respuesta.texto(), respuesta.sugerencias());
	}

	/** Mensajes de una sesión. Solo los ve quien la abrió; para cualquier otro la sesión "no existe". */
	public List<Mensaje> historial(String sesionId, String correo) {
		String usuarioId = usuarioId(correo);
		ChatSesion sesion = chatDAO.buscarSesion(sesionId)
			.filter(s -> s.usuarioId() != null && Objects.equals(s.usuarioId(), usuarioId))
			.orElseThrow(() -> new RecursoNoEncontradoException("Conversación no encontrada"));
		return chatDAO.listarMensajes(sesion.id());
	}

	private ChatSesion sesionDe(String sesionId, String usuarioId, CanalChat canal) {
		if (sesionId != null && !sesionId.isBlank()) {
			// Una sesión ajena (o de otro usuario) no se reutiliza: se abre una nueva.
			var existente = chatDAO.buscarSesion(sesionId).filter(s -> Objects.equals(s.usuarioId(), usuarioId));
			if (existente.isPresent()) {
				return existente.get();
			}
		}
		LocalDateTime ahora = LocalDateTime.now();
		return chatDAO.crearSesion(new ChatSesion(UUID.randomUUID().toString(), usuarioId,
			canal == null ? CanalChat.WEB : canal, ahora, ahora));
	}

	private String usuarioId(String correo) {
		if (correo == null || correo.isBlank()) {
			return null;
		}
		return usuarioDAO.buscarPorCorreo(correo).map(u -> u.getId()).orElse(null);
	}

	private List<String> nombresDeDestinos() {
		List<String> nombres = new ArrayList<>();
		for (Destino d : destinoDAO.listarTodos()) {
			for (String nombre : new String[] {d.getCiudad(), d.getNombre(), d.getPais()}) {
				if (nombre != null && !nombre.isBlank()) {
					nombres.add(nombre);
				}
			}
		}
		return nombres;
	}
}
