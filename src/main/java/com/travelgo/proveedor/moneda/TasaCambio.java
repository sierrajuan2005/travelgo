package com.travelgo.proveedor.moneda;

import java.time.Instant;

/** Tasa de cambio del dólar al peso colombiano, con su fuente y la hora de consulta. */
public record TasaCambio(double copPorUsd, String fuente, Instant fechaConsulta) {

    /** Convierte dólares a pesos, redondeando al peso. */
    public double convertirUsdACop(double usd) {
        return Math.round(usd * copPorUsd);
    }
}