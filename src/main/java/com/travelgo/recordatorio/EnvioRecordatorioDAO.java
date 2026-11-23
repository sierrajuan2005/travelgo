package com.travelgo.recordatorio;

import java.util.List;

/** Acceso a la tabla {@code envio_recordatorio}: el registro de los avisos enviados. */
public interface EnvioRecordatorioDAO {

    /** Si ese aviso de esa reserva ya se envió bien por ese canal (los FALLIDO no cuentan: se reintentan). */
    boolean yaEnviado(String reservaId, TipoAviso tipo, CanalEnvio canal);

    /**
     * Guarda el resultado de un envío. Si ya había una fila para la misma reserva, tipo y canal (un intento
     * FALLIDO anterior), la actualiza.
     */
    void registrar(EnvioRecordatorio envio);

    /** Los últimos envíos, del más reciente al más antiguo. */
    List<EnvioRecordatorio> listarRecientes(int limite);
}
