package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dto.PagoRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Factura;
import com.travelgo.model.Hotel;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de PagoService: reglas de pago (una sola vez, no pagar canceladas,
 * el pago confirma la reserva y genera la factura). Los DAO se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoDAO pagoDAO;
    @Mock
    private FacturaDAO facturaDAO;
    @Mock
    private ReservaDAO reservaDAO;

    @InjectMocks
    private PagoService service;

    private Usuario usuario;
    private Vuelo vuelo;
    private Hotel hotel;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
        vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30));
        hotel = new Hotel("h1", 100.0, true, "Hotel Mar", "Calle 1", "Doble", 3);
    }

    private Reserva reservaConServicios() {
        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        reserva.agregarServicio(hotel);
        return reserva; // total 600
    }

    private void guardadosDevuelvenElMismo() {
        when(pagoDAO.guardar(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(facturaDAO.guardar(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- pagar: camino feliz ----------

    @Test
    @DisplayName("Pagar una reserva pendiente: la confirma, registra el pago y genera la factura")
    void pagarReservaValida() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.empty());
        guardadosDevuelvenElMismo();

        Factura factura = service.pagar(new PagoRequest(reserva.getId(), MetodoPago.TARJETA_CREDITO));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoEnum());
        assertNotNull(factura.getNumero());
        assertSame(reserva, factura.getReserva());
        assertEquals(600.0, factura.getTotal(), 0.001);
        assertEquals(600.0, factura.getPago().getMonto(), 0.001);
        assertEquals(MetodoPago.TARJETA_CREDITO, factura.getPago().getMetodo());
    }

    @Test
    @DisplayName("Pagar guarda primero la reserva confirmada, luego el pago y al final la factura")
    void pagarGuardaEnOrden() {
        Reserva reserva = reservaConServicios();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.empty());
        guardadosDevuelvenElMismo();

        service.pagar(new PagoRequest(reserva.getId(), MetodoPago.EFECTIVO));

        InOrder orden = inOrder(reservaDAO, pagoDAO, facturaDAO);
        orden.verify(reservaDAO).guardar(reserva);
        orden.verify(pagoDAO).guardar(any(Pago.class));
        orden.verify(facturaDAO).guardar(any(Factura.class));
    }

    @Test
    @DisplayName("Una reserva ya confirmada por el agente (sin pago) todavía se puede pagar")
    void pagarReservaYaConfirmada() {
        Reserva reserva = reservaConServicios();
        reserva.confirmarReserva();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.empty());
        guardadosDevuelvenElMismo();

        Factura factura = service.pagar(new PagoRequest(reserva.getId(), MetodoPago.TRANSFERENCIA));

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstadoEnum());
        assertEquals(600.0, factura.getTotal(), 0.001);
    }

    // ---------- pagar: reglas de negocio ----------

    @Test
    @DisplayName("Pagar una reserva inexistente lanza RecursoNoEncontradoException y no guarda nada")
    void pagarReservaInexistente() {
        when(reservaDAO.buscarPorId("no-existe")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.pagar(new PagoRequest("no-existe", MetodoPago.EFECTIVO)));

        verify(reservaDAO, never()).guardar(any());
        verify(pagoDAO, never()).guardar(any());
        verify(facturaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Una reserva se paga una sola vez: el segundo pago lanza IllegalStateException y no guarda nada")
    void pagarDosVeces() {
        Reserva reserva = reservaConServicios();
        reserva.confirmarReserva();
        Pago pagoExistente = new Pago(reserva, MetodoPago.TARJETA_DEBITO);
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.of(pagoExistente));

        assertThrows(IllegalStateException.class,
                () -> service.pagar(new PagoRequest(reserva.getId(), MetodoPago.TARJETA_DEBITO)));

        verify(reservaDAO, never()).guardar(any());
        verify(pagoDAO, never()).guardar(any());
        verify(facturaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("No se puede pagar una reserva cancelada: lanza IllegalStateException y no guarda nada")
    void pagarReservaCancelada() {
        Reserva reserva = reservaConServicios();
        reserva.cancelarReserva();
        when(reservaDAO.buscarPorId(reserva.getId())).thenReturn(Optional.of(reserva));
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.pagar(new PagoRequest(reserva.getId(), MetodoPago.EFECTIVO)));

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstadoEnum());
        verify(reservaDAO, never()).guardar(any());
        verify(pagoDAO, never()).guardar(any());
        verify(facturaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("No se puede pagar una reserva sin servicios: lanza IllegalStateException y no guarda nada")
    void pagarReservaSinServicios() {
        Reserva vacia = new Reserva(usuario, null);
        when(reservaDAO.buscarPorId(vacia.getId())).thenReturn(Optional.of(vacia));
        when(pagoDAO.buscarPorReservaId(vacia.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> service.pagar(new PagoRequest(vacia.getId(), MetodoPago.EFECTIVO)));

        assertEquals(EstadoReserva.PENDIENTE, vacia.getEstadoEnum());
        verify(pagoDAO, never()).guardar(any());
        verify(facturaDAO, never()).guardar(any());
    }

    // ---------- consultas ----------

    @Test
    @DisplayName("Obtener un pago existente lo devuelve")
    void obtenerExistente() {
        Reserva reserva = reservaConServicios();
        Pago pago = new Pago(reserva, MetodoPago.EFECTIVO);
        when(pagoDAO.buscarPorId(pago.getId())).thenReturn(Optional.of(pago));

        assertSame(pago, service.obtener(pago.getId()));
    }

    @Test
    @DisplayName("Obtener un pago inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(pagoDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener("x"));
    }

    @Test
    @DisplayName("Obtener el pago de una reserva existente lo devuelve")
    void obtenerPorReservaExistente() {
        Reserva reserva = reservaConServicios();
        Pago pago = new Pago(reserva, MetodoPago.EFECTIVO);
        when(pagoDAO.buscarPorReservaId(reserva.getId())).thenReturn(Optional.of(pago));

        assertSame(pago, service.obtenerPorReserva(reserva.getId()));
    }

    @Test
    @DisplayName("Obtener el pago de una reserva sin pagos lanza RecursoNoEncontradoException")
    void obtenerPorReservaSinPago() {
        when(pagoDAO.buscarPorReservaId("r1")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorReserva("r1"));
    }

    @Test
    @DisplayName("Listar delega en el DAO")
    void listar() {
        List<Pago> pagos = List.of(new Pago(reservaConServicios(), MetodoPago.EFECTIVO));
        when(pagoDAO.listarTodos()).thenReturn(pagos);

        assertEquals(pagos, service.listar());
    }
}
