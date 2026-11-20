package com.travelgo.service;

import com.travelgo.dto.MapaRespuestaDTO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.CiudadesMapa;
import com.travelgo.proveedor.lugar.LugarMapa;
import com.travelgo.proveedor.lugar.ProveedorLugares;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas de MapaService con los proveedores simulados. */
@ExtendWith(MockitoExtension.class)
class MapaServiceTest {

    @Mock private ProveedorLugares proveedorLugares;
    @Mock private ProveedorHoteles proveedorHoteles;

    private MapaService service;

    @BeforeEach
    void preparar() {
        service = new MapaService(new CiudadesMapa(), proveedorLugares, proveedorHoteles);
    }

    @Test
    @DisplayName("Sin categorías consulta las cinco: cuatro en Geoapify y los hoteles en Xotelo")
    void sinCategoriasConsultaTodas() {
        service.lugares("cartagena", null, 5000, 20);

        verify(proveedorLugares, times(4)).buscar(any(CategoriaMapa.class), anyDouble(), anyDouble(),
                eq(5000), eq(20));
        verify(proveedorHoteles).buscarHoteles("g297476", 20);
    }

    @Test
    @DisplayName("Con una sola categoría no consulta las demás fuentes")
    void soloRestaurantes() {
        LugarMapa restaurante = new LugarMapa("1", "Restaurante", CategoriaMapa.RESTAURANTE, 10.4, -75.5,
                "dirección", null, null, "Geoapify");
        when(proveedorLugares.buscar(eq(CategoriaMapa.RESTAURANTE), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(restaurante));

        MapaRespuestaDTO respuesta = service.lugares("cartagena", Set.of(CategoriaMapa.RESTAURANTE), 3000, 10);

        assertEquals(1, respuesta.lugares().size());
        verifyNoInteractions(proveedorHoteles);
    }

    @Test
    @DisplayName("Los hoteles sin coordenadas no se dibujan")
    void hotelesSinCoordenadas() {
        when(proveedorHoteles.buscarHoteles("g297476", 10)).thenReturn(List.of(
                new HotelResumen("g297476-d1", "Hotel A", "Hotel", null, "https://ta/1", 10.4, -75.5, 4.5),
                new HotelResumen("g297476-d2", "Hotel B", "Hotel", null, "https://ta/2", null, null, 4.0)));

        MapaRespuestaDTO respuesta = service.lugares("cartagena", Set.of(CategoriaMapa.HOTEL), 3000, 10);

        assertEquals(1, respuesta.lugares().size());
        assertEquals(CategoriaMapa.HOTEL, respuesta.lugares().get(0).categoria());
    }

    @Test
    @DisplayName("Si una fuente falla, devuelve las demás y un aviso")
    void unaFuenteFalla() {
        when(proveedorLugares.buscar(eq(CategoriaMapa.CAFE), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenThrow(new ProveedorNoDisponibleException("sin llave"));

        MapaRespuestaDTO respuesta = service.lugares("cartagena",
                EnumSet.of(CategoriaMapa.CAFE, CategoriaMapa.RESTAURANTE), 3000, 10);

        assertEquals(1, respuesta.avisos().size());
    }

    @Test
    @DisplayName("Una ciudad que no está en el mapa lanza RecursoNoEncontradoException")
    void ciudadInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.lugares("narnia", null, 3000, 10));
    }
}