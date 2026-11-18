package com.travelgo.proveedor.moneda;

/** Contrato de cualquier fuente de tasas de cambio. Cambiar de API es crear otro adaptador. */
public interface ProveedorTasaCambio {

    /** Tasa actual de dólares a pesos colombianos. */
    TasaCambio obtenerUsdACop();
}