package com.travelgo.recordatorio;

/**
 * Tipos de aviso que envía TravelGo. Los nombres son los mismos del CHECK de la tabla
 * {@code envio_recordatorio} (schema.sql, Integrante 4).
 */
public enum TipoAviso {
    /** Faltan 7 días o menos para la salida (y más de 1 día). */
    VIAJE_7_DIAS,
    /** Falta 1 día o menos para la salida (y más de unas horas). */
    VIAJE_1_DIA,
    /** Faltan unas pocas horas para la salida. */
    VIAJE_HORAS,
    /** La reserva sigue sin pagar varios días después de crearla. */
    PAGO_PENDIENTE,
    /** El pago se registró; el aviso lleva el resumen de la factura. */
    PAGO_CONFIRMADO
}
