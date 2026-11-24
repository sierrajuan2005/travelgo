package com.travelgo.expo;

/**
 * Envía el viaje del visitante por WhatsApp. Hoy hay un adaptador (Twilio, {@link TwilioWhatsApp}); para usar
 * la API oficial de Meta se crea otro adaptador sin tocar el servicio (principio abierto/cerrado).
 *
 * Regla de WhatsApp (anti-spam): una empresa no puede escribirle a alguien de la nada. La persona tiene que
 * escribirle primero al número de la empresa; desde ese momento hay 24 horas para responderle con mensajes
 * libres (texto e imagen). Por eso el visitante primero "activa" WhatsApp con {@link #enlaceActivacion()}.
 */
public interface CanalWhatsApp {

    /** false si faltan las credenciales: la página no ofrece WhatsApp y la app sigue funcionando. */
    boolean configurado();

    /**
     * Enlace que abre WhatsApp con el mensaje de activación ya escrito (por ejemplo "join palabra-palabra"
     * al número del sandbox). Vacío si no hace falta activar.
     */
    String enlaceActivacion();

    /**
     * Envía el mensaje. Que lo acepte no quiere decir que llegue: el resultado real se ve con {@link #estado}.
     *
     * @param telefono  número en formato internacional (+573001234567)
     * @param texto     cuerpo del mensaje
     * @param urlImagen imagen pública que acompaña el mensaje (foto del destino); null si no hay
     * @return id del mensaje en el proveedor
     * @throws WhatsAppException si el proveedor lo rechaza o no responde
     */
    String enviar(String telefono, String texto, String urlImagen);

    /**
     * Estado actual de un mensaje en el proveedor.
     *
     * @throws WhatsAppException si el proveedor no responde
     */
    EstadoWhatsApp estado(String idMensaje);

    /** Falla al hablar con el proveedor de WhatsApp. */
    class WhatsAppException extends RuntimeException {
        public WhatsAppException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }
}
