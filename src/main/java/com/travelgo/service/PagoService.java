package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dto.PagoRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Factura;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import org.springframework.stereotype.Service;

import java.util.List;

/** Reglas de negocio de los pagos. */
@Service
public class PagoService {

    private final PagoDAO pagoDAO;
    private final FacturaDAO facturaDAO;
    private final ReservaDAO reservaDAO;

    public PagoService(PagoDAO pagoDAO, FacturaDAO facturaDAO, ReservaDAO reservaDAO) {
        this.pagoDAO = pagoDAO;
        this.facturaDAO = facturaDAO;
        this.reservaDAO = reservaDAO;
    }

    /**
     * Paga una reserva: la confirma, registra el pago y genera la factura.
     * Si la reserva ya está pagada, está cancelada o no tiene servicios, no se guarda nada.
     */
    public Factura pagar(PagoRequest request) {
        Reserva reserva = reservaDAO.buscarPorId(request.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Reserva no encontrada: " + request.reservaId()));

        if (pagoDAO.buscarPorReservaId(reserva.getId()).isPresent()) {
            throw new IllegalStateException("La reserva " + reserva.getId() + " ya fue pagada.");
        }

        reserva.confirmarReserva(); // lanza excepción si está cancelada o sin servicios
        reservaDAO.guardar(reserva);

        Pago pago = pagoDAO.guardar(new Pago(reserva, request.metodo()));
        return facturaDAO.guardar(new Factura(pago));
    }

    public Pago obtener(String id) {
        return pagoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pago no encontrado: " + id));
    }

    public Pago obtenerPorReserva(String reservaId) {
        return pagoDAO.buscarPorReservaId(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La reserva " + reservaId + " no tiene pagos."));
    }

    public List<Pago> listar() {
        return pagoDAO.listarTodos();
    }
}