package com.travelgo.service;

import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.dto.ReservaRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Transporte;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ReservaService. Los DAO se simulan con Mockito, así que solo
 * se prueban las reglas de negocio del servicio.
 *
 * IMPORTANTE: el usuario de la reserva sale del usuario autenticado (su correo), no del body.
 * Estas pruebas esperan que el servicio lo busque con UsuarioDAO.buscarPorCorreo(...).
 * Con el código actual de develop, crearReserva llama a buscarPorId(correo) y por eso las
 * pruebas de "crearReserva" fallan hasta que Integrante 1 corrija esa línea (ver reporte, defecto D1).
 */
@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final String CORREO = "ana@correo.com";

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private ServicioReservableDAO servicioDAO;
    @Mock
    private UsuarioDAO usuarioDAO;
    @Mock
    private PaqueteDAO paqueteDAO;

    @InjectMocks
    private ReservaService service;

    private Usuario usuario;
    private Vuelo vuelo;
    private Hotel hotel;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("u1", "Ana", CORREO, "secreto", RolUsuario.CLIENTE);
        vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30));
        hotel = new Hotel("h1", 100.0, true, "Hotel Mar", "Calle 1", "Doble", 3);
    }

    private Reserva reservaConServicios() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        return reserva;
    }

    // ---------- crearReserva ----------

    @Test
    @DisplayName("Crear reserva con usuario autenticado y servicios válidos la guarda PENDIENTE con el total correcto")
    void crearReservaValida() {
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));
        when(servicioDAO.buscarPorId("h1")).thenReturn(Optional.of(hotel));
        when(reservaDAO.guardar(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reserva = service.crearReserva(new ReservaRequest(List.of("v1", "h1"), null), CORREO);

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoEnum());
        assertEquals(2, reserva.getServicios().size());
        assertEquals(600.0, reserva.getTotal(), 0.001); // vuelo 300 + hotel 100 x 3 noches
        assertSame(usuario, reserva.getUsuario());
        assertNull(reserva.getPaqueteTuristico());
        verify(reservaDAO).guardar(reserva);
    }

    @Test
    @DisplayName("Crear reserva con paquete lo asocia a la reserva")
    void crearReservaConPaquete() {
        PaqueteTuristico paquete = new PaqueteTuristico("Caribe", "Playa", new BigDecimal("500.00"), 5);
        paquete.setId(7L);
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(paqueteDAO.buscarPorId(7L)).thenReturn(Optional.of(paquete));
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));
        when(reservaDAO.guardar(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reserva = service.crearReserva(new ReservaRequest(List.of("v1"), 7L), CORREO);

        assertSame(paquete, reserva.getPaqueteTuristico());
    }

    @Test
    @DisplayName("Crear reserva con usuario autenticado inexistente lanza RecursoNoEncontradoException y no guarda")
    void crearReservaUsuarioInexistente() {
        when(usuarioDAO.buscarPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearReserva(new ReservaRequest(List.of("v1"), null), "nadie@correo.com"));

        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear reserva con paquete inexistente lanza RecursoNoEncontradoException y no guarda")
    void crearReservaPaqueteInexistente() {
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(paqueteDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearReserva(new ReservaRequest(List.of("v1"), 99L), CORREO));

        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear reserva con un servicio inexistente lanza RecursoNoEncontradoException y no guarda")
    void crearReservaServicioInexistente() {
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(servicioDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearReserva(new ReservaRequest(List.of("x"), null), CORREO));

        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear reserva con un servicio no disponible lanza IllegalStateException y no guarda")
    void crearReservaServicioNoDisponible() {
        Transporte agotado = new Transporte("t1", 50.0, false, "Bus", "Bogotá", "Tunja",
                LocalDateTime.of(2026, 12, 2, 6, 0));
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(servicioDAO.buscarPorId("t1")).thenReturn(Optional.of(agotado));

        assertThrows(IllegalStateException.class,
                () -> service.crearReserva(new ReservaRequest(List.of("t1"), null), CORREO));

        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Si el segundo servicio falla no se guarda la reserva (todo o nada)")
    void crearReservaFallaEnElSegundoServicio() {
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));
        when(servicioDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearReserva(new ReservaRequest(List.of("v1", "x"), null), CORREO));

        verify(reservaDAO, never()).guardar(any());
    }

    // ---------- consultas ----------

    @Test
    @DisplayName("Obtener una reserva existente la devuelve")
    void obtenerExistente() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertSame(reserva, service.obtener(reserva.getId()));
    }

    @Test
    @DisplayName("Obtener una reserva inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(reservaDAO.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener("no-existe"));
    }

    @Test
    @DisplayName("Listar delega en el DAO")
    void listar() {
        List<Reserva> reservas = List.of(reservaConServicios());
        when(reservaDAO.listarTodas()).thenReturn(reservas);

        assertEquals(reservas, service.listar());
    }

    @Test
    @DisplayName("Listar por usuario delega en el DAO con el id del usuario")
    void listarPorUsuario() {
        List<Reserva> reservas = List.of(reservaConServicios());
        when(reservaDAO.listarPorUsuario("u1")).thenReturn(reservas);

        assertEquals(reservas, service.listarPorUsuario("u1"));
    }

    // ---------- reservas propias (usuario autenticado) ----------

    @Test
    @DisplayName("El dueño puede obtener su reserva (el correo se compara sin importar mayúsculas)")
    void obtenerPropiaDelDueno() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertSame(reserva, service.obtenerPropia(reserva.getId(), "ANA@correo.com"));
    }

    @Test
    @DisplayName("Obtener la reserva de otro usuario lanza AccessDeniedException")
    void obtenerPropiaAjena() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class,
                () -> service.obtenerPropia(reserva.getId(), "otro@correo.com"));
    }

    @Test
    @DisplayName("Listar propias busca al usuario por correo y lista por su id")
    void listarPropias() {
        List<Reserva> reservas = List.of(reservaConServicios());
        when(usuarioDAO.buscarPorCorreo(CORREO)).thenReturn(Optional.of(usuario));
        when(reservaDAO.listarPorUsuario("u1")).thenReturn(reservas);

        assertEquals(reservas, service.listarPropias(CORREO));
    }

    @Test
    @DisplayName("Listar propias con un usuario inexistente lanza RecursoNoEncontradoException")
    void listarPropiasUsuarioInexistente() {
        when(usuarioDAO.buscarPorCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.listarPropias("nadie@correo.com"));
    }

    @Test
    @DisplayName("Confirmar la reserva propia la pasa a CONFIRMADA y la guarda")
    void confirmarPropia() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaDAO.guardar(reserva)).thenReturn(reserva);

        Reserva confirmada = service.confirmarPropia(reserva.getId(), CORREO);

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstadoEnum());
        verify(reservaDAO).guardar(reserva);
    }

    @Test
    @DisplayName("Confirmar la reserva de otro usuario lanza AccessDeniedException y no guarda")
    void confirmarPropiaAjena() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class,
                () -> service.confirmarPropia(reserva.getId(), "otro@correo.com"));

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoEnum());
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Cancelar la reserva propia la pasa a CANCELADA y la guarda")
    void cancelarPropia() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaDAO.guardar(reserva)).thenReturn(reserva);

        Reserva cancelada = service.cancelarPropia(reserva.getId(), CORREO);

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstadoEnum());
    }

    @Test
    @DisplayName("Cancelar la reserva de otro usuario lanza AccessDeniedException y no guarda")
    void cancelarPropiaAjena() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(AccessDeniedException.class,
                () -> service.cancelarPropia(reserva.getId(), "otro@correo.com"));

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstadoEnum());
        verify(reservaDAO, never()).guardar(any());
    }

    // ---------- confirmar / cancelar sin dueño (uso del agente) ----------

    @Test
    @DisplayName("Confirmar una reserva pendiente con servicios la pasa a CONFIRMADA y la guarda")
    void confirmarValida() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaDAO.guardar(reserva)).thenReturn(reserva);

        Reserva confirmada = service.confirmarReserva(reserva.getId());

        assertEquals(EstadoReserva.CONFIRMADA, confirmada.getEstadoEnum());
        verify(reservaDAO).guardar(reserva);
    }

    @Test
    @DisplayName("Confirmar una reserva inexistente lanza RecursoNoEncontradoException")
    void confirmarInexistente() {
        when(reservaDAO.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.confirmarReserva("no-existe"));
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Confirmar una reserva sin servicios lanza IllegalStateException y no guarda")
    void confirmarSinServicios() {
        Reserva vacia = new Reserva(usuario, null);
        when(reservaDAO.buscarPorId(vacia.getId())).thenReturn(Optional.of(vacia));

        assertThrows(IllegalStateException.class, () -> service.confirmarReserva(vacia.getId()));
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Confirmar una reserva cancelada lanza IllegalStateException y no guarda")
    void confirmarCancelada() {
        Reserva reserva = reservaConServicios();
        reserva.cancelarReserva();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.confirmarReserva(reserva.getId()));
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Cancelar una reserva la pasa a CANCELADA y la guarda")
    void cancelarValida() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(reservaDAO.guardar(reserva)).thenReturn(reserva);

        Reserva cancelada = service.cancelarReserva(reserva.getId());

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstadoEnum());
        verify(reservaDAO).guardar(reserva);
    }

    @Test
    @DisplayName("Cancelar una reserva ya cancelada lanza IllegalStateException y no guarda")
    void cancelarDosVeces() {
        Reserva reserva = reservaConServicios();
        reserva.cancelarReserva();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.cancelarReserva(reserva.getId()));
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Cancelar una reserva inexistente lanza RecursoNoEncontradoException")
    void cancelarInexistente() {
        when(reservaDAO.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.cancelarReserva("no-existe"));
    }
}
