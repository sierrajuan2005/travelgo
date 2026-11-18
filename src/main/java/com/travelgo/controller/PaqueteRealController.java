package com.travelgo.controller;

import com.travelgo.dto.ArmarPaqueteRequest;
import com.travelgo.dto.PaqueteRealRespuestaDTO;
import com.travelgo.service.PaqueteRealService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Armado y consulta de paquetes con servicios reales. Armar debe quedar solo para el Administrador. */
@RestController
@RequestMapping("/api/paquetes-reales")
public class PaqueteRealController {

    private final PaqueteRealService service;

    public PaqueteRealController(PaqueteRealService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaqueteRealRespuestaDTO> armar(@Valid @RequestBody ArmarPaqueteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.armar(request));
    }

    @GetMapping("/{id}")
    public PaqueteRealRespuestaDTO detalle(@PathVariable Long id) {
        return service.detalle(id);
    }
}