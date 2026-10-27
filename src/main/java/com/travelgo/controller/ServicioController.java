package com.travelgo.controller;

import com.travelgo.dto.HotelRequest;
import com.travelgo.dto.TransporteRequest;
import com.travelgo.dto.VueloRequest;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import com.travelgo.service.ServicioReservableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Endpoints REST de vuelos, hoteles y transportes. */
@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioReservableService service;

    public ServicioController(ServicioReservableService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServicioReservable> listar(@RequestParam(defaultValue = "true") boolean soloDisponibles) {
        return service.listar(soloDisponibles);
    }

    @GetMapping("/{id}")
    public ServicioReservable obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping("/vuelos")
    public ResponseEntity<Vuelo> crearVuelo(@RequestBody VueloRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearVuelo(request));
    }

    @PostMapping("/hoteles")
    public ResponseEntity<Hotel> crearHotel(@RequestBody HotelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearHotel(request));
    }

    @PostMapping("/transportes")
    public ResponseEntity<Transporte> crearTransporte(@RequestBody TransporteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTransporte(request));
    }

    @PutMapping("/vuelos/{id}")
    public Vuelo actualizarVuelo(@PathVariable String id, @RequestBody VueloRequest request) {
        return service.actualizarVuelo(id, request);
    }

    @PutMapping("/hoteles/{id}")
    public Hotel actualizarHotel(@PathVariable String id, @RequestBody HotelRequest request) {
        return service.actualizarHotel(id, request);
    }

    @PutMapping("/transportes/{id}")
    public Transporte actualizarTransporte(@PathVariable String id, @RequestBody TransporteRequest request) {
        return service.actualizarTransporte(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}