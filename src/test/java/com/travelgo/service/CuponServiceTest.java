package com.travelgo.service;

import com.travelgo.dao.CuponDAO;
import com.travelgo.dto.CuponAplicadoDTO;
import com.travelgo.dto.CuponRequest;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Cupon;
import com.travelgo.model.TipoCupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de CuponService con el DAO simulado. */
@ExtendWith(MockitoExtension.class)
class CuponServiceTest {

    @Mock
    private CuponDAO cuponDAO;

    @InjectMocks
    private CuponService service;

    private final LocalDate hoy = LocalDate.now();

    private Cupon cuponVigente() {
        return new Cupon("VERANO10", TipoCupon.PORCENTAJE, 10, hoy.minusDays(1), hoy.plusDays(30), 5);
    }

    @Test
    @DisplayName("Crear un cupón nuevo lo guarda")
    void crear() {
        CuponRequest request = new CuponRequest("verano10", TipoCupon.PORCENTAJE, 10.0,
                hoy, hoy.plusDays(30), 5);
        when(cuponDAO.buscarPorCodigo("VERANO10")).thenReturn(Optional.empty());
        when(cuponDAO.guardar(any(Cupon.class))).thenAnswer(inv -> inv.getArgument(0));

        Cupon creado = service.crear(request);

        assertEquals("VERANO10", creado.getCodigo());
        verify(cuponDAO).guardar(any(Cupon.class));
    }

    @Test
    @DisplayName("Crear un cupón con un código repetido lanza IllegalStateException y no guarda")
    void crearRepetido() {
        CuponRequest request = new CuponRequest("VERANO10", TipoCupon.PORCENTAJE, 10.0,
                hoy, hoy.plusDays(30), 5);
        when(cuponDAO.buscarPorCodigo("VERANO10")).thenReturn(Optional.of(cuponVigente()));

        assertThrows(IllegalStateException.class, () -> service.crear(request));

        verify(cuponDAO, never()).guardar(any());
    }

    @Test
    @DisplayName("Un cupón inexistente lanza RecursoNoEncontradoException")
    void noExiste() {
        when(cuponDAO.buscarPorCodigo("NOEXISTE")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorCodigo("noexiste"));
    }

    @Test
    @DisplayName("Un cupón vencido no se puede usar: lanza IllegalStateException")
    void vencido() {
        Cupon vencido = new Cupon("VENCIDO", TipoCupon.PORCENTAJE, 10, hoy.minusDays(10), hoy.minusDays(1), 5);
        when(cuponDAO.buscarPorCodigo("VENCIDO")).thenReturn(Optional.of(vencido));

        assertThrows(IllegalStateException.class, () -> service.obtenerVigente("VENCIDO"));
    }

    @Test
    @DisplayName("Simular calcula el descuento y el total, sin gastar un uso")
    void simular() {
        Cupon cupon = cuponVigente();
        when(cuponDAO.buscarPorCodigo("VERANO10")).thenReturn(Optional.of(cupon));

        CuponAplicadoDTO resultado = service.simular("verano10", 1050000);

        assertEquals(105000.0, resultado.descuento(), 0.001);
        assertEquals(945000.0, resultado.totalConDescuento(), 0.001);
        assertEquals(0, cupon.getUsosActuales());
    }
}