package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Factura;
import org.springframework.stereotype.Service;

import java.util.List;

/** Consulta de facturas. */
@Service
public class FacturaService {

    private final FacturaDAO facturaDAO;

    public FacturaService(FacturaDAO facturaDAO) {
        this.facturaDAO = facturaDAO;
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
}