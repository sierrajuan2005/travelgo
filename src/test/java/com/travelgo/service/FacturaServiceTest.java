package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import com.travelgo.dao.UsuarioDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.security.access.AccessDeniedException;
import static org.mockito.Mockito.never;

/** Pruebas unitarias de FacturaService (solo consultas) con el DAO simulado. */
@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaDAO facturaDAO;

    @Mock
    private UsuarioDAO usuarioDAO;

    @InjectMocks
    private FacturaService service;

    private Factura factura;
    private Reserva reserva;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
        Vuelo vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30));
        reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        factura = new Factura(reserva);
    }

    @Test
    @DisplayName("Obtener una factura existente la devuelve")
    void obtenerExistente() {
        when(facturaDAO.buscarPorId(factura.getId())).thenReturn(Optional.of(factura));

        assertSame(factura, service.obtener(factura.getId()));
    }

    @Test
    @DisplayName("Obtener una factura inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(facturaDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener("x"));
    }

    @Test
    @DisplayName("Obtener la factura de una reserva existente la devuelve")
    void obtenerPorReservaExistente() {
        when(facturaDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.of(factura));

        assertSame(factura, service.obtenerPorReserva(reserva.getId()));
    }

    @Test
    @DisplayName("Obtener la factura de una reserva sin factura lanza RecursoNoEncontradoException")
    void obtenerPorReservaSinFactura() {
        when(facturaDAO.buscarPorReservaId("r1")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorReserva("r1"));
    }

    @Test
    @DisplayName("Listar delega en el DAO")
    void listar() {
        when(facturaDAO.listarTodas()).thenReturn(List.of(factura));

        assertEquals(List.of(factura), service.listar());
    }

    @Test
    @DisplayName("Listar por usuario delega en el DAO con el id del usuario")
    void listarPorUsuario() {
        when(facturaDAO.listarPorUsuario("u1")).thenReturn(List.of(factura));

        assertEquals(List.of(factura), service.listarPorUsuario("u1"));
        verify(facturaDAO).listarPorUsuario("u1");
    }


    @Test
    @DisplayName("Un cliente puede listar sus propias facturas")
    void listarPropias() {
        when(usuarioDAO.buscarPorCorreo("ana@correo.com"))
                .thenReturn(Optional.of(usuario));
        when(facturaDAO.listarPorUsuario("u1"))
                .thenReturn(List.of(factura));

        assertEquals(List.of(factura),
                service.listarPropias("ana@correo.com"));
    }

    @Test
    @DisplayName("Un cliente puede consultar su propia factura")
    void obtenerPropia() {
        when(usuarioDAO.buscarPorCorreo("ana@correo.com"))
                .thenReturn(Optional.of(usuario));
        when(facturaDAO.buscarPorId(factura.getId()))
                .thenReturn(Optional.of(factura));

        assertSame(factura,
                service.obtenerPropia(factura.getId(), "ana@correo.com"));
    }

    @Test
    @DisplayName("Un cliente no puede consultar una factura ajena")
    void obtenerFacturaAjena() {
        when(usuarioDAO.buscarPorCorreo("otro@correo.com"))
                .thenReturn(Optional.of(new Usuario(
                        "u2", "Carlos", "otro@correo.com",
                        "secreto", RolUsuario.CLIENTE
                )));
        when(facturaDAO.buscarPorId(factura.getId()))
                .thenReturn(Optional.of(factura));

        assertThrows(AccessDeniedException.class,
                () -> service.obtenerPropia(
                        factura.getId(), "otro@correo.com"
                ));
    }

    @Test
    @DisplayName("Un cliente no puede listar facturas de otro usuario")
    void listarFacturasDeOtroUsuario() {
        when(usuarioDAO.buscarPorCorreo("ana@correo.com"))
                .thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class,
                () -> service.listarPorUsuarioPropio("u2", "ana@correo.com"));

        verify(facturaDAO, never()).listarPorUsuario("u2");
    }

}
