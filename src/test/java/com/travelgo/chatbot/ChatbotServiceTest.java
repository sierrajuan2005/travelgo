package com.travelgo.chatbot;

import com.travelgo.chatbot.estrategia.ComoPagarEstrategia;
import com.travelgo.chatbot.estrategia.EstadoReservaEstrategia;
import com.travelgo.chatbot.estrategia.NoEntendidaEstrategia;
import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.ReservaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pruebas del motor del chatbot: sesiones, mensajes guardados y privacidad. Sin base de datos. */
@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

	/** DAO en memoria para probar sin PostgreSQL. */
	static class ChatDAOMemoria implements ChatDAO {
		final List<ChatSesion> sesiones = new ArrayList<>();
		final List<Mensaje> mensajes = new ArrayList<>();

		@Override
		public ChatSesion crearSesion(ChatSesion sesion) {
			sesiones.add(sesion);
			return sesion;
		}

		@Override
		public Optional<ChatSesion> buscarSesion(String id) {
			return sesiones.stream().filter(s -> s.id().equals(id)).findFirst();
		}

		@Override
		public void actualizarActividad(String sesionId, LocalDateTime fecha) {
		}

		@Override
		public void guardarMensaje(Mensaje mensaje) {
			mensajes.add(mensaje);
		}

		@Override
		public List<Mensaje> listarMensajes(String sesionId) {
			return mensajes.stream().filter(m -> m.sesionId().equals(sesionId)).toList();
		}
	}

	@Mock
	private UsuarioDAO usuarioDAO;
	@Mock
	private DestinoDAO destinoDAO;
	@Mock
	private ReservaService reservaService;

	private ChatDAOMemoria chatDAO;
	private ChatbotService service;

	@BeforeEach
	void preparar() {
		lenient().when(usuarioDAO.buscarPorCorreo("ana@correo.com"))
			.thenReturn(Optional.of(new Usuario("u-ana", "Ana", "ana@correo.com", "hash", RolUsuario.CLIENTE)));
		lenient().when(usuarioDAO.buscarPorCorreo("bruno@correo.com"))
			.thenReturn(Optional.of(new Usuario("u-bruno", "Bruno", "bruno@correo.com", "hash", RolUsuario.CLIENTE)));
		chatDAO = new ChatDAOMemoria();
		service = new ChatbotService(chatDAO, usuarioDAO, destinoDAO, new ExtractorDatos(), List.of(
			new EstadoReservaEstrategia(reservaService), new ComoPagarEstrategia(), new NoEntendidaEstrategia()));
	}

	@Test
	@DisplayName("El primer mensaje abre una sesión y guarda el mensaje y la respuesta")
	void primerMensaje() {
		RespuestaChat r = service.responder(null, "¿Cómo pago?", "ana@correo.com", CanalChat.WEB);

		assertEquals(1, chatDAO.sesiones.size());
		assertEquals("u-ana", chatDAO.sesiones.get(0).usuarioId());
		assertEquals(r.sesionId(), chatDAO.sesiones.get(0).id());
		List<Mensaje> guardados = chatDAO.listarMensajes(r.sesionId());
		assertEquals(2, guardados.size());
		assertEquals(Mensaje.USUARIO, guardados.get(0).emisor());
		assertEquals(Mensaje.BOT, guardados.get(1).emisor());
		assertEquals("COMO_PAGAR", guardados.get(1).intencion());
		assertTrue(guardados.get(0).fecha().isBefore(guardados.get(1).fecha()));
	}

	@Test
	@DisplayName("Con el id de la sesión, la conversación continúa en la misma sesión")
	void continuaSesion() {
		RespuestaChat primera = service.responder(null, "¿Cómo pago?", "ana@correo.com", CanalChat.WEB);
		RespuestaChat segunda = service.responder(primera.sesionId(), "asdf", "ana@correo.com", CanalChat.WEB);

		assertEquals(primera.sesionId(), segunda.sesionId());
		assertEquals(1, chatDAO.sesiones.size());
		assertEquals(4, chatDAO.listarMensajes(primera.sesionId()).size());
	}

	@Test
	@DisplayName("La sesión de otro usuario no se reutiliza: se abre una nueva")
	void sesionAjena() {
		RespuestaChat deAna = service.responder(null, "¿Cómo pago?", "ana@correo.com", CanalChat.WEB);

		RespuestaChat deBruno = service.responder(deAna.sesionId(), "hola", "bruno@correo.com", CanalChat.WEB);
		RespuestaChat visitante = service.responder(deAna.sesionId(), "hola", null, CanalChat.WEB);

		assertNotEquals(deAna.sesionId(), deBruno.sesionId());
		assertNotEquals(deAna.sesionId(), visitante.sesionId());
		assertEquals(2, chatDAO.listarMensajes(deAna.sesionId()).size());
	}

	@Test
	@DisplayName("Un visitante no ve datos privados y no se consulta nada")
	void privadoSinSesion() {
		RespuestaChat r = service.responder(null, "¿Cómo va mi reserva?", null, CanalChat.WEB);

		assertTrue(r.respuesta().contains("iniciar sesión"));
		verifyNoInteractions(reservaService);
		assertEquals(null, chatDAO.sesiones.get(0).usuarioId());
	}

	@Test
	@DisplayName("Una pregunta que no entiende ofrece sugerencias")
	void noEntendida() {
		RespuestaChat r = service.responder(null, "asdf qwerty", null, CanalChat.WEB);

		assertTrue(r.respuesta().contains("No entendí"));
		assertTrue(r.sugerencias().contains("¿Cómo pago?"));
		assertEquals("NO_ENTENDIDA", chatDAO.mensajes.get(1).intencion());
	}

	@Test
	@DisplayName("Un mensaje vacío o demasiado largo se rechaza sin guardar nada")
	void mensajeInvalido() {
		assertThrows(IllegalArgumentException.class, () -> service.responder(null, "  ", null, CanalChat.WEB));
		assertThrows(IllegalArgumentException.class, () -> service.responder(null, null, null, CanalChat.WEB));
		assertThrows(IllegalArgumentException.class,
			() -> service.responder(null, "a".repeat(ChatbotService.MAX_MENSAJE + 1), null, CanalChat.WEB));
		assertTrue(chatDAO.sesiones.isEmpty());
		assertTrue(chatDAO.mensajes.isEmpty());
	}

	@Test
	@DisplayName("El historial lo ve solo quien abrió la sesión")
	void historial() {
		RespuestaChat r = service.responder(null, "¿Cómo pago?", "ana@correo.com", CanalChat.WEB);

		assertEquals(2, service.historial(r.sesionId(), "ana@correo.com").size());
		assertThrows(RecursoNoEncontradoException.class, () -> service.historial(r.sesionId(), "bruno@correo.com"));
		assertThrows(RecursoNoEncontradoException.class, () -> service.historial(r.sesionId(), null));
		assertThrows(RecursoNoEncontradoException.class, () -> service.historial("no-existe", "ana@correo.com"));
	}
}
