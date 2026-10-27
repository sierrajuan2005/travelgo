package com.travelgo.controller;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.service.PaqueteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST del catálogo de paquetes turísticos. */
@RestController
@RequestMapping("/api/paquetes")
public class PaqueteController {

    private final PaqueteService service;

    public PaqueteController(PaqueteService service) {
        this.service = service;
    }

    @GetMapping
    public List<PaqueteTuristico> listar(@RequestParam(defaultValue = "true") boolean soloActivos) {
        return service.listar(soloActivos);
    }

    @GetMapping("/{id}")
    public PaqueteTuristico obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PaqueteTuristico> crear(@Valid @RequestBody PaqueteTuristico paquete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(paquete));
    }

    @PutMapping("/{id}")
    public PaqueteTuristico actualizar(@PathVariable Long id, @Valid @RequestBody PaqueteTuristico paquete) {
        return service.actualizar(id, paquete);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}