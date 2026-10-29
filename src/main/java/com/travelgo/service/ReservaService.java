package com.travelgo.service;

import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.dto.ReservaRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Reserva;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import org.springframework.stereotype.Service;
import com.travelgo.model.PaqueteTuristico;
import org.springframework.security.access.AccessDeniedException;


import java.util.List;

/** Reglas de negocio de las reservas. */
@Service
public class ReservaService {

    private final ReservaDAO reservaDAO;
    private final ServicioReservableDAO servicioDAO;
    private final UsuarioDAO usuarioDAO;
    private final PaqueteDAO paqueteDAO;

    public ReservaService(ReservaDAO reservaDAO,ServicioReservableDAO servicioDAO,UsuarioDAO usuarioDAO,PaqueteDAO paqueteDAO) {
        this.reservaDAO = reservaDAO;
        this.servicioDAO = servicioDAO;
        this.usuarioDAO = usuarioDAO;
        this.paqueteDAO = paqueteDAO;
    }

    /** Crea la reserva; si un servicio no existe o no está disponible, no se guarda nada. */
    public Reserva crearReserva(ReservaRequest request, String correoAutenticado) {
        Usuario usuario = usuarioDAO.buscarPorId(correoAutenticado)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Usuario no encontrado"));

        PaqueteTuristico paquete = request.paqueteId() == null
                ? null
                : paqueteDAO.buscarPorId(request.paqueteId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Paquete no encontrado: " + request.paqueteId()));

        Reserva reserva = new Reserva(usuario, paquete);

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

    public Reserva obtenerPropia(String id, String correo) {
        Reserva reserva = obtener(id);
        verificarPropietario(reserva, correo);
        return reserva;
    }

    public List<Reserva> listarPropias(String correo) {
        Usuario usuario = usuarioDAO.buscarPorCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario autenticado no encontrado"));

        return reservaDAO.listarPorUsuario(usuario.getId());
    }

    public Reserva confirmarPropia(String id, String correo) {
        Reserva reserva = obtenerPropia(id, correo);
        reserva.confirmarReserva();
        return reservaDAO.guardar(reserva);
    }

    public Reserva cancelarPropia(String id, String correo) {
        Reserva reserva = obtenerPropia(id, correo);
        reserva.cancelarReserva();
        return reservaDAO.guardar(reserva);
    }

    private void verificarPropietario(Reserva reserva, String correo) {
        if (!reserva.getUsuario().getCorreo().equalsIgnoreCase(correo)) {
            throw new AccessDeniedException(
                    "No tienes permiso para acceder a esta reserva");
        }
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