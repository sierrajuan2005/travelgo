package com.travelgo.controller;

import com.travelgo.dto.FacturaRespuestaDTO;
import com.travelgo.service.FacturaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import com.travelgo.model.RolUsuario;

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
    public List<FacturaRespuestaDTO> listar(Authentication autenticacion) {
        if (esPersonal(autenticacion)) {
            return service.listar().stream()
                    .map(FacturaRespuestaDTO::desde).toList();
        }

        return service.listarPropias(autenticacion.getName()).stream()
                .map(FacturaRespuestaDTO::desde).toList();
    }

    @GetMapping("/{id}")
    public FacturaRespuestaDTO obtener(
            @PathVariable String id, Authentication autenticacion) {
        if (esPersonal(autenticacion)) {
            return FacturaRespuestaDTO.desde(service.obtener(id));
        }

        return FacturaRespuestaDTO.desde(
                service.obtenerPropia(id, autenticacion.getName()));
    }

    @GetMapping("/reserva/{reservaId}")
    public FacturaRespuestaDTO obtenerPorReserva(
            @PathVariable String reservaId, Authentication autenticacion) {
        if (esPersonal(autenticacion)) {
            return FacturaRespuestaDTO.desde(
                    service.obtenerPorReserva(reservaId));
        }

        return FacturaRespuestaDTO.desde(
                service.obtenerPorReservaPropia(
                        reservaId, autenticacion.getName()));
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<FacturaRespuestaDTO> listarPorUsuario(
            @PathVariable String usuarioId, Authentication autenticacion) {
        if (esPersonal(autenticacion)) {
            return service.listarPorUsuario(usuarioId).stream()
                    .map(FacturaRespuestaDTO::desde).toList();
        }

        return service.listarPorUsuarioPropio(
                usuarioId, autenticacion.getName()).stream()
                .map(FacturaRespuestaDTO::desde).toList();
    }

    private boolean esPersonal(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(autoridad ->
                        autoridad.equals("ROLE_" + RolUsuario.AGENTE.name())
                        || autoridad.equals("ROLE_" + RolUsuario.ADMINISTRADOR.name()));
    }
}