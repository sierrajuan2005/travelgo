package com.travelgo.service;

import com.travelgo.dao.ReservaDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.dto.ReservaRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reglas de negocio de las reservas. */
@Service
public class ReservaService {

    private final ReservaDAO reservaDAO;
    private final ServicioReservableDAO servicioDAO;
    private final UsuarioDAO usuarioDAO;

    public ReservaService(ReservaDAO reservaDAO, ServicioReservableDAO servicioDAO, UsuarioDAO usuarioDAO) {
        this.reservaDAO = reservaDAO;
        this.servicioDAO = servicioDAO;
        this.usuarioDAO = usuarioDAO;
    }

    /** Crea la reserva; si un servicio no existe o no está disponible, no se guarda nada. */
    public Reserva crearReserva(ReservaRequest request) {
        Usuario usuario = usuarioDAO.buscarPorId(request.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + request.usuarioId()));

        Reserva reserva = new Reserva(usuario);
        for (String idServicio : request.serviciosIds()) {
            ServicioReservable servicio = servicioDAO.buscarPorId(idServicio)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Servicio no encontrado: " + idServicio));
            reserva.agregarServicio(servicio);
        }
        return reservaDAO.guardar(reserva);
    }

    public Reserva obtener(String id) {
        return reservaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada: " + id));
    }

    public List<Reserva> listar() {
        return reservaDAO.listarTodas();
    }

    public List<Reserva> listarPorUsuario(String usuarioId) {
        return reservaDAO.listarPorUsuario(usuarioId);
    }

    public Reserva confirmarReserva(String id) {
        Reserva reserva = obtener(id);
        reserva.confirmarReserva();
        return reservaDAO.guardar(reserva);
    }

    public Reserva cancelarReserva(String id) {
        Reserva reserva = obtener(id);
        reserva.cancelarReserva();
        return reservaDAO.guardar(reserva);
    }
}