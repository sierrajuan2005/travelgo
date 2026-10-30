package com.travelgo.controller;

import com.travelgo.dto.ReservaRequest;
import com.travelgo.dto.ReservaRespuestaDTO;
import com.travelgo.service.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    public ResponseEntity<ReservaRespuestaDTO> crear(
            @Valid @RequestBody ReservaRequest request,
            Authentication autenticacion) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReservaRespuestaDTO.desde(
                    service.crearReserva(request, autenticacion.getName())));
}

    @GetMapping
    public List<ReservaRespuestaDTO> listar(Authentication autenticacion) {
        return service.listarPropias(autenticacion.getName())
                .stream().map(ReservaRespuestaDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public ReservaRespuestaDTO obtener(
            @PathVariable String id, Authentication autenticacion) {
        return ReservaRespuestaDTO.desde(
                service.obtenerPropia(id, autenticacion.getName()));
    }


    @PutMapping("/{id}/confirmar")
    public ReservaRespuestaDTO confirmar(
            @PathVariable String id, Authentication autenticacion) {
        return ReservaRespuestaDTO.desde(
                service.confirmarPropia(id, autenticacion.getName()));
    }

    @PutMapping("/{id}/cancelar")
    public ReservaRespuestaDTO cancelar(
            @PathVariable String id, Authentication autenticacion) {
        return ReservaRespuestaDTO.desde(
                service.cancelarPropia(id, autenticacion.getName()));
    }
}