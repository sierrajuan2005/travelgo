package com.travelgo.service;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.dto.PagoRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Cupon;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Factura;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reglas de negocio de los pagos. */
@Service
public class PagoService {

    private final PagoDAO pagoDAO;
    private final FacturaDAO facturaDAO;
    private final ReservaDAO reservaDAO;
    private final CuponService cuponService;

    /** Versión sin cupones (la usa la consola de pruebas, que arma los objetos a mano). */
    public PagoService(PagoDAO pagoDAO, FacturaDAO facturaDAO, ReservaDAO reservaDAO) {
        this(pagoDAO, facturaDAO, reservaDAO, null);
    }

    @Autowired
    public PagoService(PagoDAO pagoDAO, FacturaDAO facturaDAO, ReservaDAO reservaDAO,
                       CuponService cuponService) {
        this.pagoDAO = pagoDAO;
        this.facturaDAO = facturaDAO;
        this.reservaDAO = reservaDAO;
        this.cuponService = cuponService;
    }

    /**
     * Paga una reserva: obtiene (o emite) su factura, aplica el cupón si viene uno, registra el pago,
     * marca la factura como pagada, confirma la reserva y gasta un uso del cupón.
     * Todo va en una transacción: se guarda todo o nada.
     */
    @Transactional
    public Pago pagar(PagoRequest request) {
        Reserva reserva = reservaDAO.buscarPorId(request.reservaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Reserva no encontrada: " + request.reservaId()));

        if (pagoDAO.buscarPorReservaId(reserva.getId()).isPresent()) {
            throw new IllegalStateException("La reserva " + reserva.getId() + " ya fue pagada.");
        }
        if (reserva.getEstadoEnum() == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("No se puede pagar una reserva cancelada.");
        }

        Factura factura = facturaDAO.buscarPorReservaId(reserva.getId())
                .orElseGet(() -> new Factura(reserva)); // falla si la reserva no tiene servicios

        Cupon cupon = null;
        if (request.codigoCupon() != null && !request.codigoCupon().isBlank()) {
            if (cuponService == null) {
                throw new IllegalStateException("Los cupones no están disponibles en este contexto.");
            }
            cupon = cuponService.obtenerVigente(request.codigoCupon()); // falla si no está vigente
            factura.aplicarCupon(cupon);
        }

        Pago pago = new Pago(factura, request.metodo()); // cobra el total ya descontado

        factura.marcarPagada();
        reserva.confirmarReserva();
        if (cupon != null) {
            cuponService.registrarUso(cupon);
        }

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