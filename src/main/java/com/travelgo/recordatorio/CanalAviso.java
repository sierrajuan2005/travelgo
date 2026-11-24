package com.travelgo.recordatorio;

import com.travelgo.model.Usuario;

/**
 * Un canal de avisos (correo, SMS...). El {@link RecordatorioService} depende de esta interfaz y no de un canal
 * concreto (inversión de dependencias): agregar otro canal es crear otra clase que la implemente, sin tocar el
 * servicio (abierto/cerrado). Es una interfaz pequeña a propósito (segregación de interfaces).
 */
public interface CanalAviso {

    /** Qué canal es. Se guarda en {@code envio_recordatorio.canal}. */
    CanalEnvio canal();

    /**
     * Si el canal tiene sus credenciales. Sin ellas no se intenta enviar nada (la aplicación arranca igual en
     * el computador de cualquier integrante).
     */
    boolean configurado();

    /** Si el usuario aceptó avisos por este canal y dio el dato necesario (correo o teléfono). */
    boolean aceptaAvisos(Usuario usuario);

    /**
     * Envía el aviso al usuario.
     *
     * @throws CanalAvisoException si no se pudo entregar
     */
    void enviar(Usuario usuario, Aviso aviso);
}
