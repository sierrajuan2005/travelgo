package com.travelgo.controller;

import com.travelgo.dto.CuponAplicadoDTO;
import com.travelgo.dto.CuponRequest;
import com.travelgo.dto.CuponRespuestaDTO;
import com.travelgo.service.CuponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de cupones. Crear y desactivar debe quedar solo para el Administrador (roles de Integrante 1). */
@RestController
@RequestMapping("/api/cupones")
public class CuponController {

    private final CuponService service;

    public CuponController(CuponService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CuponRespuestaDTO> crear(@Valid @RequestBody CuponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CuponRespuestaDTO.desde(service.crear(request)));
    }

    @GetMapping
    public List<CuponRespuestaDTO> listar() {
        return service.listar().stream().map(CuponRespuestaDTO::desde).toList();
    }

    @GetMapping("/{codigo}")
    public CuponRespuestaDTO obtener(@PathVariable String codigo) {
        return CuponRespuestaDTO.desde(service.obtenerPorCodigo(codigo));
    }

    /** Vista previa del descuento: GET /api/cupones/validar?codigo=VERANO10&subtotal=1050000 */
    @GetMapping("/validar")
    public CuponAplicadoDTO validar(@RequestParam String codigo, @RequestParam double subtotal) {
        return service.simular(codigo, subtotal);
    }

    @PutMapping("/{id}/desactivar")
    public CuponRespuestaDTO desactivar(@PathVariable String id) {
        return CuponRespuestaDTO.desde(service.desactivar(id));
    }
}