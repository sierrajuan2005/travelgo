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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de ServicioReservableService con el DAO simulado. */
@ExtendWith(MockitoExtension.class)
class ServicioReservableServiceTest {

    private static final LocalDateTime SALIDA = LocalDateTime.of(2026, 12, 1, 8, 0);
    private static final LocalDateTime LLEGADA = LocalDateTime.of(2026, 12, 1, 9, 30);

    @Mock
    private ServicioReservableDAO servicioDAO;

    @InjectMocks
    private ServicioReservableService service;

    private VueloRequest vueloRequest() {
        return new VueloRequest(300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);
    }

    private HotelRequest hotelRequest() {
        return new HotelRequest(100.0, true, "Hotel Mar", "Calle 1", "Doble", 3);
    }

    private TransporteRequest transporteRequest() {
        return new TransporteRequest(50.0, true, "Bus", "Bogotá", "Tunja", SALIDA);
    }

    private void guardarDevuelveElMismo() {
        when(servicioDAO.guardar(any(ServicioReservable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- crear ----------

    @Test
    @DisplayName("Crear vuelo válido genera un id y lo guarda")
    void crearVuelo() {
        guardarDevuelveElMismo();

        Vuelo vuelo = service.crearVuelo(vueloRequest());

        assertNotNull(vuelo.getId());
        assertEquals("Avianca", vuelo.getAerolinea());
        assertEquals(300.0, vuelo.calcularCosto(), 0.001);
        verify(servicioDAO).guardar(vuelo);
    }

    @Test
    @DisplayName("Crear hotel válido calcula el costo como precio por noches")
    void crearHotel() {
        guardarDevuelveElMismo();

        Hotel hotel = service.crearHotel(hotelRequest());

        assertEquals(300.0, hotel.calcularCosto(), 0.001);
        verify(servicioDAO).guardar(hotel);
    }

    @Test
    @DisplayName("Crear transporte válido lo guarda")
    void crearTransporte() {
        guardarDevuelveElMismo();

        Transporte transporte = service.crearTransporte(transporteRequest());

        assertEquals("Bus", transporte.getTipo());
        verify(servicioDAO).guardar(transporte);
    }

    @Test
    @DisplayName("Si 'disponible' no viene en la petición, el servicio queda disponible")
    void disponiblePorDefecto() {
        guardarDevuelveElMismo();

        Vuelo vuelo = service.crearVuelo(
                new VueloRequest(300.0, null, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA));

        assertTrue(vuelo.isDisponible());
    }

    @Test
    @DisplayName("Crear un servicio con disponible=false lo guarda no disponible")
    void crearNoDisponible() {
        guardarDevuelveElMismo();

        Vuelo vuelo = service.crearVuelo(
                new VueloRequest(300.0, false, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA));

        assertFalse(vuelo.isDisponible());
    }

    @Test
    @DisplayName("Crear un servicio sin precio lanza IllegalArgumentException y no guarda")
    void crearSinPrecio() {
        VueloRequest sinPrecio = new VueloRequest(null, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);

        assertThrows(IllegalArgumentException.class, () -> service.crearVuelo(sinPrecio));
        verify(servicioDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear un servicio con precio negativo lanza IllegalArgumentException")
    void crearPrecioNegativo() {
        HotelRequest negativo = new HotelRequest(-5.0, true, "Hotel Mar", "Calle 1", "Doble", 3);

        assertThrows(IllegalArgumentException.class, () -> service.crearHotel(negativo));
        verify(servicioDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear un hotel sin noches lanza IllegalArgumentException")
    void crearHotelSinNoches() {
        HotelRequest sinNoches = new HotelRequest(100.0, true, "Hotel Mar", "Calle 1", "Doble", null);

        assertThrows(IllegalArgumentException.class, () -> service.crearHotel(sinNoches));
        verify(servicioDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear un hotel con 0 noches lanza IllegalArgumentException")
    void crearHotelCeroNoches() {
        HotelRequest cero = new HotelRequest(100.0, true, "Hotel Mar", "Calle 1", "Doble", 0);

        assertThrows(IllegalArgumentException.class, () -> service.crearHotel(cero));
    }

    @Test
    @DisplayName("Crear un vuelo con la llegada antes de la salida lanza IllegalArgumentException")
    void crearVueloFechasInvertidas() {
        VueloRequest invertido = new VueloRequest(300.0, true, "Avianca", "Bogotá", "Cartagena", LLEGADA, SALIDA);

        assertThrows(IllegalArgumentException.class, () -> service.crearVuelo(invertido));
    }

    @Test
    @DisplayName("Crear un transporte con origen igual al destino lanza IllegalArgumentException")
    void crearTransporteMismoOrigenDestino() {
        TransporteRequest igual = new TransporteRequest(50.0, true, "Bus", "Bogotá", "bogotá", SALIDA);

        assertThrows(IllegalArgumentException.class, () -> service.crearTransporte(igual));
    }

    // ---------- obtener / listar ----------

    @Test
    @DisplayName("Obtener un servicio existente lo devuelve")
    void obtenerExistente() {
        Vuelo vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));

        assertSame(vuelo, service.obtener("v1"));
    }

    @Test
    @DisplayName("Obtener un servicio inexistente lanza RecursoNoEncontradoException")
    void obtenerInexistente() {
        when(servicioDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener("x"));
    }

    @Test
    @DisplayName("Listar con soloDisponibles=true usa listarDisponibles")
    void listarSoloDisponibles() {
        List<ServicioReservable> disponibles = List.of(
                new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA));
        when(servicioDAO.listarDisponibles()).thenReturn(disponibles);

        assertEquals(disponibles, service.listar(true));
        verify(servicioDAO, never()).listarTodos();
    }

    @Test
    @DisplayName("Listar con soloDisponibles=false usa listarTodos")
    void listarTodos() {
        List<ServicioReservable> todos = List.of(
                new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA));
        when(servicioDAO.listarTodos()).thenReturn(todos);

        assertEquals(todos, service.listar(false));
        verify(servicioDAO, never()).listarDisponibles();
    }

    // ---------- actualizar ----------

    @Test
    @DisplayName("Actualizar un vuelo conserva su id y reemplaza los datos")
    void actualizarVuelo() {
        Vuelo existente = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(existente));
        guardarDevuelveElMismo();

        Vuelo actualizado = service.actualizarVuelo("v1",
                new VueloRequest(450.0, true, "LATAM", "Bogotá", "Cartagena", SALIDA, LLEGADA));

        assertEquals("v1", actualizado.getId());
        assertEquals("LATAM", actualizado.getAerolinea());
        assertEquals(450.0, actualizado.getPrecio(), 0.001);
    }

    @Test
    @DisplayName("Actualizar un vuelo inexistente lanza RecursoNoEncontradoException")
    void actualizarInexistente() {
        when(servicioDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> service.actualizarVuelo("x", vueloRequest()));
        verify(servicioDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Actualizar un hotel usando el id de un vuelo lanza IllegalArgumentException")
    void actualizarTipoIncorrecto() {
        Vuelo vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));

        assertThrows(IllegalArgumentException.class, () -> service.actualizarHotel("v1", hotelRequest()));
        verify(servicioDAO, never()).guardar(any());
    }

    // ---------- eliminar ----------

    @Test
    @DisplayName("Eliminar un servicio existente lo borra en el DAO")
    void eliminarExistente() {
        Vuelo vuelo = new Vuelo("v1", 300.0, true, "Avianca", "Bogotá", "Cartagena", SALIDA, LLEGADA);
        when(servicioDAO.buscarPorId("v1")).thenReturn(Optional.of(vuelo));

        service.eliminar("v1");

        verify(servicioDAO).eliminar("v1");
    }

    @Test
    @DisplayName("Eliminar un servicio inexistente lanza RecursoNoEncontradoException y no borra")
    void eliminarInexistente() {
        when(servicioDAO.buscarPorId("x")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.eliminar("x"));
        verify(servicioDAO, never()).eliminar(any());
    }
}
