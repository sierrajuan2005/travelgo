package com.travelgo.service;

import com.travelgo.dao.CuponDAO;
import com.travelgo.dto.CuponAplicadoDTO;
import com.travelgo.dto.CuponRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Cupon;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/** Reglas de negocio de los cupones de descuento. */
@Service
public class CuponService {

    private final CuponDAO cuponDAO;

    public CuponService(CuponDAO cuponDAO) {
        this.cuponDAO = cuponDAO;
    }

    public Cupon crear(CuponRequest request) {
        Cupon nuevo = new Cupon(request.codigo(), request.tipo(), request.valor(),
                request.fechaInicio(), request.fechaFin(), request.usosMaximos());
        if (cuponDAO.buscarPorCodigo(nuevo.getCodigo()).isPresent()) {
            throw new IllegalStateException("Ya existe un cupón con el código " + nuevo.getCodigo() + ".");
        }
        return cuponDAO.guardar(nuevo);
    }

    public List<Cupon> listar() {
        return cuponDAO.listarTodos();
    }

    public Cupon obtenerPorCodigo(String codigo) {
        String normalizado = Cupon.normalizarCodigo(codigo);
        return cuponDAO.buscarPorCodigo(normalizado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupón no encontrado: " + normalizado));
    }

    /** Devuelve el cupón solo si se puede usar hoy; si no, explica que no está vigente. */
    public Cupon obtenerVigente(String codigo) {
        Cupon cupon = obtenerPorCodigo(codigo);
        if (!cupon.estaVigente(LocalDate.now())) {
            throw new IllegalStateException(
                    "El cupón " + cupon.getCodigo() + " no está vigente (venció, se agotó o fue desactivado).");
        }
        return cupon;
    }

    /** Calcula cuánto descontaría el cupón sobre un subtotal, sin registrar su uso. */
    public CuponAplicadoDTO simular(String codigo, double subtotal) {
        Cupon cupon = obtenerVigente(codigo);
        double descuento = cupon.calcularDescuento(subtotal);
        return new CuponAplicadoDTO(cupon.getCodigo(), subtotal, descuento, subtotal - descuento);
    }

    public Cupon desactivar(String id) {
        Cupon cupon = cuponDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cupón no encontrado: " + id));
        cupon.desactivar();
        return cuponDAO.guardar(cupon);
    }
    /** Gasta un uso del cupón y lo guarda. */
    public Cupon registrarUso(Cupon cupon) {
        cupon.registrarUso();
        return cuponDAO.guardar(cupon);
    }
}