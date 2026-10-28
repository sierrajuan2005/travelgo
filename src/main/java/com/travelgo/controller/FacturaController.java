package com.travelgo.controller;

import com.travelgo.dto.FacturaRespuestaDTO;
import com.travelgo.service.FacturaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de facturas. */
@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService service;

    public FacturaController(FacturaService service) {
        this.service = service;
    }

    @GetMapping
    public List<FacturaRespuestaDTO> listar() {
        return service.listar().stream().map(FacturaRespuestaDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public FacturaRespuestaDTO obtener(@PathVariable String id) {
        return FacturaRespuestaDTO.desde(service.obtener(id));
    }

    @GetMapping("/reserva/{reservaId}")
    public FacturaRespuestaDTO obtenerPorReserva(@PathVariable String reservaId) {
        return FacturaRespuestaDTO.desde(service.obtenerPorReserva(reservaId));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<FacturaRespuestaDTO> listarPorUsuario(@PathVariable String usuarioId) {
        return service.listarPorUsuario(usuarioId).stream().map(FacturaRespuestaDTO::desde).toList();
    }
}