package com.travelgo.controller;

import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Consulta de hoteles reales y sus precios de referencia. No reserva nada. */
@RestController
@RequestMapping("/api/hoteles-reales")
public class HotelRealController {

    private final ProveedorHoteles proveedor;

    public HotelRealController(ProveedorHoteles proveedor) {
        this.proveedor = proveedor;
    }

    @GetMapping
    public List<HotelResumen> listar(@RequestParam String ciudad,
                                     @RequestParam(defaultValue = "30") int limite) {
        return proveedor.buscarHoteles(ciudad, limite);
    }

    @GetMapping("/{hotelClave}/tarifa")
    public OfertaHotel tarifa(@PathVariable String hotelClave,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate entrada,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salida,
                              @RequestParam(defaultValue = "1") int habitaciones,
                              @RequestParam(defaultValue = "2") int adultos) {
        return proveedor.consultarTarifa(hotelClave, entrada, salida, habitaciones, adultos)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay tarifas para ese hotel y esas fechas."));
    }
}