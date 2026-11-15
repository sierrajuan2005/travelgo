package com.travelgo.dto;

import com.travelgo.model.Cupon;
import com.travelgo.model.TipoCupon;

import java.time.LocalDate;

/** Respuesta con los datos de un cupón. */
public record CuponRespuestaDTO(String id, String codigo, TipoCupon tipo, double valor,
                                LocalDate fechaInicio, LocalDate fechaFin,
                                int usosMaximos, int usosActuales, boolean activo, boolean vigente) {

    public static CuponRespuestaDTO desde(Cupon c) {
        return new CuponRespuestaDTO(c.getId(), c.getCodigo(), c.getTipo(), c.getValor(),
                c.getFechaInicio(), c.getFechaFin(), c.getUsosMaximos(), c.getUsosActuales(),
                c.isActivo(), c.estaVigente(LocalDate.now()));
    }
}

