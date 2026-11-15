
package com.travelgo.controller;

import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Vuelo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Consulta vuelos reales y sus enlaces externos de reserva.
 * Los precios son referencias y no garantizan disponibilidad.
 */
@RestController
@RequestMapping("/api/vuelos-reales")
public class VueloRealController {

    private final ProveedorVuelos proveedor;

    public VueloRealController(ProveedorVuelos proveedor) {
        this.proveedor = proveedor;
    }

    @GetMapping
    public List<Vuelo> buscar(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaSalida,
            @RequestParam(required = false) String codigoAerolinea) {

        return proveedor.buscarVuelos(
                origen,
                destino,
                fechaSalida,
                codigoAerolinea
        );
    }

    @GetMapping("/{idExterno}/enlace-reserva")
    public String obtenerEnlaceReserva(
            @PathVariable String idExterno) {

        String enlace = proveedor.obtenerEnlaceReserva(idExterno);

        if (enlace == null || enlace.isBlank()) {
            throw new RecursoNoEncontradoException(
                    "No se encontró un enlace de reserva para ese vuelo."
            );
        }

        return enlace;
    }
}
