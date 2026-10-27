package com.travelgo.controller;

import com.travelgo.dto.ReservaRequest;
import com.travelgo.dto.ReservaRespuestaDTO;
import com.travelgo.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de reservas. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService service;

    public ReservaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ReservaRespuestaDTO> crear(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservaRespuestaDTO.desde(service.crearReserva(request)));
    }

    @GetMapping
    public List<ReservaRespuestaDTO> listar() {
        return service.listar().stream().map(ReservaRespuestaDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ReservaRespuestaDTO obtener(@PathVariable String id) {
        return ReservaRespuestaDTO.desde(service.obtener(id));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<ReservaRespuestaDTO> listarPorUsuario(@PathVariable String usuarioId) {
        return service.listarPorUsuario(usuarioId).stream().map(ReservaRespuestaDTO::desde).toList();
    }

    @PutMapping("/{id}/confirmar")
    public ReservaRespuestaDTO confirmar(@PathVariable String id) {
        return ReservaRespuestaDTO.desde(service.confirmarReserva(id));
    }

    @PutMapping("/{id}/cancelar")
    public ReservaRespuestaDTO cancelar(@PathVariable String id) {
        return ReservaRespuestaDTO.desde(service.cancelarReserva(id));
    }
}