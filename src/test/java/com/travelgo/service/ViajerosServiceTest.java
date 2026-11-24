package com.travelgo.service;

import com.travelgo.dao.ActividadDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.model.Actividad;
import com.travelgo.model.Destino;
import com.travelgo.model.Gusto;
import com.travelgo.model.Pago;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import com.travelgo.service.ViajerosService.DatosViajero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglas al guardar los viajeros: solo actividades del destino, máximo de personas y nada si ya se pagó. */
class ViajerosServiceTest {

    private ReservaDAO reservaDAO;
    private ActividadDAO actividadDAO;
    private PagoDAO pagoDAO;
    private ViajerosService servicio;
    private Reserva reserva;

    private final Actividad bares = new Actividad("roma-bares", 10L, "Noche de bares", "Tres bares",
            Gusto.FIESTA, 180000, 4);

    @BeforeEach
    void preparar() {
        reservaDAO = mock(ReservaDAO.class);
        actividadDAO = mock(ActividadDAO.class);
        pagoDAO = mock(PagoDAO.class);
        servicio = new ViajerosService(reservaDAO, actividadDAO, pagoDAO);

        Destino roma = new Destino("Roma", "Italia", "Italia", "Roma");
        roma.setId(10L);
        PaqueteTuristico paquete = new PaqueteTuristico("Roma 5 días", "Roma", new BigDecimal("5000000"), 5);
        paquete.setId(1L);
        paquete.setDestinos(List.of(roma));
        Usuario usuario = new Usuario("u1", "Ana", "ana@correo.com", "secreto", RolUsuario.CLIENTE);
        reserva = new Reserva(usuario, paquete);

        when(actividadDAO.listarPorDestinos(anyCollection())).thenReturn(List.of(bares));
        when(pagoDAO.buscarPorReservaId(anyString())).thenReturn(Optional.empty());
        when(reservaDAO.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    @DisplayName("Guarda los viajeros y el total suma sus actividades")
    void guarda() {
        Reserva guardada = servicio.guardarViajeros(reserva, List.of(
                new DatosViajero("Ana", EnumSet.of(Gusto.FIESTA), List.of("roma-bares")),
                new DatosViajero("Luis", EnumSet.of(Gusto.RELIGION), List.of())));

        assertEquals(2, guardada.getViajeros().size());
        assertEquals(180000, guardada.getTotal(), 0.001);
        verify(reservaDAO).guardar(reserva);
    }

    @Test
    @DisplayName("No acepta una actividad de otro destino (formulario cambiado a mano)")
    void actividadAjena() {
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarViajeros(reserva,
                List.of(new DatosViajero("Ana", EnumSet.of(Gusto.FIESTA), List.of("cun-coco-bongo")))));
        verify(reservaDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Pide al menos un viajero y como máximo ocho")
    void cantidad() {
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarViajeros(reserva, List.of()));
        List<DatosViajero> nueve = Collections.nCopies(9, new DatosViajero("Ana", EnumSet.noneOf(Gusto.class), List.of()));
        assertThrows(IllegalArgumentException.class, () -> servicio.guardarViajeros(reserva, nueve));
    }

    @Test
    @DisplayName("Una reserva pagada ya no cambia sus viajeros")
    void pagada() {
        when(pagoDAO.buscarPorReservaId(anyString())).thenReturn(Optional.of(mock(Pago.class)));
        assertThrows(IllegalStateException.class, () -> servicio.guardarViajeros(reserva,
                List.of(new DatosViajero("Ana", EnumSet.noneOf(Gusto.class), List.of()))));
    }
}
