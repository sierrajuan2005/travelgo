package com.travelgo.recordatorio;

import java.time.LocalDateTime;

/**
 * Registro de un aviso enviado (una fila de la tabla {@code envio_recordatorio}).
 * Sirve para no repetir el mismo aviso de una reserva por el mismo canal.
 */
public record EnvioRecordatorio(
        String id,
        String usuarioId,
        String reservaId,
        TipoAviso tipo,
        CanalEnvio canal,
        EstadoEnvio estado,
        LocalDateTime fechaEnvio) {
}
