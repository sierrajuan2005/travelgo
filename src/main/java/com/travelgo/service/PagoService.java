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
     * Paga una reserva: obtiene (o emite) su factura, registra el pago,
     * marca la factura como pagada y confirma la reserva.
     * Todas las validaciones van antes de modificar algo, para no dejar datos a medias.
     */
    public Pago pagar(PagoRequest request) {
        Reserva reserva = reservaDAO.buscarPorId(request.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Reserva no encontrada: " + request.reservaId()));

        if (pagoDAO.buscarPorReservaId(reserva.getId()).isPresent()) {
            throw new IllegalStateException("La reserva " + reserva.getId() + " ya fue pagada.");
        }
        if ("CANCELADA".equalsIgnoreCase(reserva.getEstado())) {
            throw new IllegalStateException("No se puede pagar una reserva cancelada.");
        }

        Factura factura = facturaDAO.buscarPorReservaId(reserva.getId()) // busca la factura
                .orElseGet(() -> new Factura(reserva)); // falla si la reserva no tiene servicios y si no exite la crea
        Pago pago = new Pago(factura, request.metodo()); // falla si la factura no está pendiente

        factura.marcarPagada();
        reserva.confirmarReserva();

        reservaDAO.guardar(reserva);
        facturaDAO.guardar(factura);
        return pagoDAO.guardar(pago);
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