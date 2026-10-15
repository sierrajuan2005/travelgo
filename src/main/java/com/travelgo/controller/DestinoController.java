package com.travelgo.controller;
import com.travelgo.model.Destino;
import com.travelgo.service.DestinoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST del catálogo de destinos. */
@RestController
@RequestMapping("/api/destinos")
public class DestinoController {

    private final DestinoService service;

    public DestinoController(DestinoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Destino> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Destino obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Destino> crear(@Valid @RequestBody Destino destino) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(destino));
    }

    @PutMapping("/{id}")
    public Destino actualizar(@PathVariable Long id, @Valid @RequestBody Destino destino) {
        return service.actualizar(id, destino);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}