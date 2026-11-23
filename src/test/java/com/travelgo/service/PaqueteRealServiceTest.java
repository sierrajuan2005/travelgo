package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dto.ArmarPaqueteRequest;
import com.travelgo.dto.PaqueteRealRespuestaDTO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.hotel.TarifaAgencia;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import com.travelgo.proveedor.moneda.TasaCambio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.travelgo.proveedor.hotel.ClasificadorGama;
import com.travelgo.proveedor.hotel.GamaHotel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas de PaqueteRealService con todos los proveedores y DAO simulados (no llama a internet ni a la BD). */
@ExtendWith(MockitoExtension.class)
class PaqueteRealServiceTest {

    private static final String CLAVE = "g297476-d8021569";

    @Mock private ProveedorHoteles proveedorHoteles;
    @Mock private ProveedorTasaCambio proveedorTasa;
    @Mock private ServicioReservableDAO servicioDAO;
    @Mock private PaqueteDAO paqueteDAO;
    @Mock private DestinoDAO destinoDAO;
    @Mock private PaqueteServicioDAO paqueteServicioDAO;
    @Mock private ClasificadorGama clasificador;
    @InjectMocks
    private PaqueteRealService service;

    private final LocalDate entrada = LocalDate.now().plusDays(30);
    private final LocalDate salida = entrada.plusDays(3);

    private ArmarPaqueteRequest request() {
        return new ArmarPaqueteRequest("Cartagena económica", "Hotel ibis frente al mar",
                List.of(1L), "Cartagena", CLAVE, entrada, salida, null, null, null);
    }

    private HotelResumen resumen() {
        return new HotelResumen(CLAVE, "ibis Cartagena Marbella", "Hotel", "https://foto",
                "https://www.tripadvisor.com/Hotel_Review-ibis", 10.4367, -75.5356, 4.0);
    }

    private OfertaHotel oferta() {
        TarifaAgencia mejor = new TarifaAgencia("Trip.com", 70.0);
        return new OfertaHotel(CLAVE, mejor, List.of(new TarifaAgencia("Booking.com", 78.0)), "USD",
                entrada, salida, 3, 1, 2, Instant.now(), "Xotelo (precios de TripAdvisor)");
    }

    private void destinoExiste() {
        when(destinoDAO.buscarPorId(1L))
                .thenReturn(Optional.of(new Destino("Cartagena", "Caribe", "Colombia", "Cartagena")));
    }

    @Test
    @DisplayName("Arma el paquete: convierte a pesos, guarda el hotel con su referencia y lo une al paquete")
    void armaElPaquete() {
        destinoExiste();
        when(proveedorHoteles.buscarHoteles("g297476", 100)).thenReturn(List.of(resumen()));
        when(proveedorHoteles.consultarTarifa(CLAVE, entrada, salida, 1, 2)).thenReturn(Optional.of(oferta()));
        when(proveedorTasa.obtenerUsdACop()).thenReturn(new TasaCambio(4000.0, "open.er-api.com", Instant.now()));
        when(servicioDAO.guardar(any(Hotel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paqueteDAO.guardar(any(PaqueteTuristico.class))).thenAnswer(inv -> {
            PaqueteTuristico p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });
        when(clasificador.gamaDe(any(Hotel.class))).thenReturn(GamaHotel.ECONOMICO);

        PaqueteRealRespuestaDTO respuesta = service.armar(request());

        ArgumentCaptor<Hotel> captor = ArgumentCaptor.forClass(Hotel.class);
        verify(servicioDAO).guardar(captor.capture());
        Hotel hotel = captor.getValue();
        assertEquals(280000.0, hotel.getPrecio(), 0.001);            // 70 USD x 4000
        assertEquals(840000.0, hotel.calcularCosto(), 0.001);        // x 3 noches
        assertEquals(CLAVE, hotel.getIdExterno());
        assertEquals("https://www.tripadvisor.com/Hotel_Review-ibis", hotel.getEnlaceReserva());
        assertTrue(hotel.getFuente().contains("Trip.com"));
        assertTrue(hotel.getTipoHabitacion().contains("2 adultos"));
        assertTrue(hotel.getDireccion().startsWith("Cartagena"));

        assertEquals(7L, respuesta.id());
        assertEquals(840000.0, respuesta.precioReferencia(), 0.001);
        assertEquals("COP", respuesta.moneda());
        assertEquals(4, respuesta.duracionDias());                    // 3 noches = 4 días
        assertEquals(1, respuesta.servicios().size());
        assertEquals(GamaHotel.ECONOMICO, respuesta.gama());
        verify(paqueteServicioDAO).vincular(7L, hotel.getId());
    }

    @Test
    @DisplayName("Un destino inexistente lanza RecursoNoEncontradoException y no guarda nada")
    void destinoInexistente() {
        when(destinoDAO.buscarPorId(1L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.armar(request()));

        verify(servicioDAO, never()).guardar(any());
        verify(paqueteDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Un hotel que no está en la lista de su ciudad lanza RecursoNoEncontradoException")
    void hotelNoEncontrado() {
        destinoExiste();
        when(proveedorHoteles.buscarHoteles("g297476", 100)).thenReturn(List.of());

        assertThrows(RecursoNoEncontradoException.class, () -> service.armar(request()));

        verify(servicioDAO, never()).guardar(any());
        verify(paqueteDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Sin tarifas para esas fechas lanza RecursoNoEncontradoException y no guarda nada")
    void sinTarifas() {
        destinoExiste();
        when(proveedorHoteles.buscarHoteles("g297476", 100)).thenReturn(List.of(resumen()));
        when(proveedorHoteles.consultarTarifa(CLAVE, entrada, salida, 1, 2)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.armar(request()));

        verify(servicioDAO, never()).guardar(any());
        verify(paqueteDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Si la fuente de tasas de cambio falla, lanza ProveedorNoDisponibleException y no guarda nada")
    void tasaDeCambioCaida() {
        destinoExiste();
        when(proveedorHoteles.buscarHoteles("g297476", 100)).thenReturn(List.of(resumen()));
        when(proveedorHoteles.consultarTarifa(CLAVE, entrada, salida, 1, 2)).thenReturn(Optional.of(oferta()));
        when(proveedorTasa.obtenerUsdACop()).thenThrow(new ProveedorNoDisponibleException("sin conexión"));

        assertThrows(ProveedorNoDisponibleException.class, () -> service.armar(request()));

        verify(servicioDAO, never()).guardar(any());
        verify(paqueteDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Una clave de hotel sin el formato esperado lanza IllegalArgumentException")
    void claveInvalida() {
        destinoExiste();
        ArmarPaqueteRequest mala = new ArmarPaqueteRequest("Paquete", null, List.of(1L), "Cartagena",
                "singuion", entrada, salida, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.armar(mala));
    }

    @Test
    @DisplayName("La descripción de la habitación dice lo que se consultó, no un tipo inventado")
    void descripcionDeHabitacion() {
        assertEquals("Habitación para 2 adultos (tarifa más baja disponible)",
                PaqueteRealService.describirHabitacion(1, 2));
        assertEquals("2 habitaciones para 1 adulto (tarifa más baja disponible)",
                PaqueteRealService.describirHabitacion(2, 1));
    }
}