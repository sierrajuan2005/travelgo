package com.travelgo.controller;

import com.travelgo.dto.PagoRequest;
import com.travelgo.dto.PagoRespuestaDTO;
import com.travelgo.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de pagos. */
@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService service;

    public PagoController(PagoService service) {
        this.service = service;
    }

    /** Paga una reserva y devuelve el pago junto con su factura. */
    @PostMapping
    public ResponseEntity<PagoRespuestaDTO> pagar(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PagoRespuestaDTO.desde(service.pagar(request)));
    }

    @GetMapping
    public List<PagoRespuestaDTO> listar() {
        return service.listar().stream().map(PagoRespuestaDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public PagoRespuestaDTO obtener(@PathVariable String id) {
        return PagoRespuestaDTO.desde(service.obtener(id));
    }

    @GetMapping("/reserva/{reservaId}")
    public PagoRespuestaDTO obtenerPorReserva(@PathVariable String reservaId) {
        return PagoRespuestaDTO.desde(service.obtenerPorReserva(reservaId));
    }
}