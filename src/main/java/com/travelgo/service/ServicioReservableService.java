package com.travelgo.service;

import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dto.HotelRequest;
import com.travelgo.dto.TransporteRequest;
import com.travelgo.dto.VueloRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Reglas de negocio de vuelos, hoteles y transportes. */
@Service
public class ServicioReservableService {

    private final ServicioReservableDAO servicioDAO;

    public ServicioReservableService(ServicioReservableDAO servicioDAO) {
        this.servicioDAO = servicioDAO;
    }

    public Vuelo crearVuelo(VueloRequest r) {
        return servicioDAO.guardar(construirVuelo(nuevoId(), r));
    }

    public Hotel crearHotel(HotelRequest r) {
        return servicioDAO.guardar(construirHotel(nuevoId(), r));
    }

    public Transporte crearTransporte(TransporteRequest r) {
        return servicioDAO.guardar(construirTransporte(nuevoId(), r));
    }

    // Las clases del dominio son inmutables: actualizar = reemplazar por una nueva con el mismo id.
    public Vuelo actualizarVuelo(String id, VueloRequest r) {
        verificarTipo(id, Vuelo.class);
        return servicioDAO.guardar(construirVuelo(id, r));
    }

    public Hotel actualizarHotel(String id, HotelRequest r) {
        verificarTipo(id, Hotel.class);
        return servicioDAO.guardar(construirHotel(id, r));
    }

    public Transporte actualizarTransporte(String id, TransporteRequest r) {
        verificarTipo(id, Transporte.class);
        return servicioDAO.guardar(construirTransporte(id, r));
    }

    public ServicioReservable obtener(String id) {
        return servicioDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
    }

    public List<ServicioReservable> listar(boolean soloDisponibles) {
        return soloDisponibles ? servicioDAO.listarDisponibles() : servicioDAO.listarTodos();
    }

    public void eliminar(String id) {
        obtener(id);
        servicioDAO.eliminar(id);
    }

    private Vuelo construirVuelo(String id, VueloRequest r) {
        return new Vuelo(id, precio(r.precio()), disponible(r.disponible()), r.aerolinea(),
                r.origen(), r.destino(), r.fechaSalida(), r.fechaLlegada());
    }

    private Hotel construirHotel(String id, HotelRequest r) {
        if (r.noches() == null) {
            throw new IllegalArgumentException("La cantidad de noches es obligatoria.");
        }
        return new Hotel(id, precio(r.precio()), disponible(r.disponible()), r.nombre(),
                r.direccion(), r.tipoHabitacion(), r.noches());
    }

    private Transporte construirTransporte(String id, TransporteRequest r) {
        return new Transporte(id, precio(r.precio()), disponible(r.disponible()), r.tipo(),
                r.origen(), r.destino(), r.fecha());
    }

    private void verificarTipo(String id, Class<? extends ServicioReservable> tipo) {
        if (!tipo.isInstance(obtener(id))) {
            throw new IllegalArgumentException(
                    "El servicio " + id + " no es de tipo " + tipo.getSimpleName());
        }
    }

    private double precio(Double precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio.");
        }
        return precio;
    }

    private boolean disponible(Boolean disponible) {
        return disponible == null || disponible;
    }

    private String nuevoId() {
        return UUID.randomUUID().toString();
    }
}