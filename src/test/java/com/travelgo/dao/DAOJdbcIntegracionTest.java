package com.travelgo.dao;

import com.travelgo.model.EstadoFactura;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Factura;
import com.travelgo.model.MetodoPago;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Prueba contra la base de datos real: guarda y recupera usuario, servicio, reserva, factura y pago. */
@SpringBootTest
@Transactional
class DaoJdbcIntegracionTest {

    @Autowired private UsuarioDAO usuarioDAO;
    @Autowired private ServicioReservableDAO servicioDAO;
    @Autowired private ReservaDAO reservaDAO;
    @Autowired private FacturaDAO facturaDAO;
    @Autowired private PagoDAO pagoDAO;

    @Test
    @DisplayName("El flujo usuario, servicio, reserva, factura y pago se guarda y se recupera igual")
    void guardaYRecuperaElFlujoCompleto() {
        Usuario usuario = usuarioDAO.guardar(new Usuario(UUID.randomUUID().toString(), "Ana",
                "ana-" + UUID.randomUUID() + "@test.com", "secreto", RolUsuario.CLIENTE));
        Vuelo vuelo = servicioDAO.guardar(new Vuelo(UUID.randomUUID().toString(), 300.0, true,
                "Avianca", "Bogotá", "Cartagena",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 1, 9, 30)));

        Reserva reserva = new Reserva(usuario, null);
        reserva.agregarServicio(vuelo);
        reservaDAO.guardar(reserva);
        Factura factura = facturaDAO.guardar(new Factura(reserva));
        Pago pago = pagoDAO.guardar(new Pago(factura, MetodoPago.EFECTIVO));

        Reserva leida = reservaDAO.buscarPorId(reserva.getId()).orElseThrow();
        assertEquals(1, leida.getServicios().size());
        assertEquals(300.0, leida.getTotal(), 0.001);
        assertEquals(EstadoReserva.PENDIENTE, leida.getEstadoEnum());
        assertEquals(usuario.getCorreo(), leida.getUsuario().getCorreo());

        Factura facturaLeida = facturaDAO.buscarPorReservaId(reserva.getId()).orElseThrow();
        assertEquals(factura.getNumero(), facturaLeida.getNumero());
        assertEquals(EstadoFactura.PENDIENTE, facturaLeida.getEstado());

        assertEquals(pago.getId(), pagoDAO.buscarPorReservaId(reserva.getId()).orElseThrow().getId());
        assertEquals(1, reservaDAO.listarPorUsuario(usuario.getId()).size());
        assertEquals(1, facturaDAO.listarPorUsuario(usuario.getId()).size());
    }
}