package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Factura;
import org.springframework.stereotype.Service;
import com.travelgo.dao.UsuarioDAO;
import com.travelgo.model.Usuario;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

/** Consulta de facturas. */
@Service
public class FacturaService {

    private final FacturaDAO facturaDAO;
    private final UsuarioDAO usuarioDAO;

    public FacturaService(FacturaDAO facturaDAO, UsuarioDAO usuarioDAO) {
        this.facturaDAO = facturaDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public Factura obtener(String id) {
        return facturaDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Factura no encontrada: " + id));
    }

    public Factura obtenerPorReserva(String reservaId) {
        return facturaDAO.buscarPorReservaId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La reserva " + reservaId + " no tiene factura."));
    }

    public List<Factura> listar() {
        return facturaDAO.listarTodas();
    }

    public List<Factura> listarPorUsuario(String usuarioId) {
        return facturaDAO.listarPorUsuario(usuarioId);
    }

    /** Lista únicamente las facturas del usuario autenticado. */
    public List<Factura> listarPropias(String correo) {
        Usuario usuario = buscarUsuarioAutenticado(correo);
        return facturaDAO.listarPorUsuario(usuario.getId());
    }

    /** Obtiene una factura si pertenece al usuario autenticado. */
    public Factura obtenerPropia(String id, String correo) {
        Usuario usuario = buscarUsuarioAutenticado(correo);
        Factura factura = obtener(id);
        verificarPropietario(factura, correo);
        return factura;
    }

    /** Obtiene la factura de una reserva si pertenece al usuario autenticado. */
    public Factura obtenerPorReservaPropia(String reservaId, String correo) {
        Factura factura = obtenerPorReserva(reservaId);
        verificarPropietario(factura, correo);
        return factura;
    }

    /** Lista las facturas de un usuario, impidiendo consultar las de terceros. */
    public List<Factura> listarPorUsuarioPropio(String usuarioId, String correo) {
        Usuario usuario = buscarUsuarioAutenticado(correo);

        if (!usuario.getId().equals(usuarioId)) {
            throw new AccessDeniedException(
                "No tienes permiso para consultar las facturas de otro usuario");
        }
        return facturaDAO.listarPorUsuario(usuario.getId());
    }

    private Usuario buscarUsuarioAutenticado(String correo) {
        return usuarioDAO.buscarPorCorreo(correo)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Usuario autenticado no encontrado"));
    }

    private void verificarPropietario(Factura factura, String correo) {
        if (!factura.getReserva().getUsuario().getCorreo()
                .equalsIgnoreCase(correo)) {
            throw new AccessDeniedException(
                    "No tienes permiso para acceder a esta factura");
        }
    }
}