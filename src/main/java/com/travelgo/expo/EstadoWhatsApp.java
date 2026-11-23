package com.travelgo.expo;

import java.util.Locale;

/**
 * Estado de un mensaje de WhatsApp, traducido a lo que entiende la página del visitante.
 *
 * @param estado      ENVIANDO, ENTREGADO, LEIDO, SIN_ACTIVAR (no ha escrito al número) o FALLIDO
 * @param codigoError código del proveedor cuando falla (Twilio 63015 = no se unió al sandbox); null si no hay
 */
public record EstadoWhatsApp(String estado, Integer codigoError) {

    public static final String ENVIANDO = "ENVIANDO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String LEIDO = "LEIDO";
    public static final String SIN_ACTIVAR = "SIN_ACTIVAR";
    public static final String FALLIDO = "FALLIDO";

    /** Twilio: el número no se ha unido al sandbox, o pasaron más de 24 horas desde su último mensaje. */
    private static final int NO_SE_UNIO_AL_SANDBOX = 63015;
    private static final int FUERA_DE_LAS_24_HORAS = 63016;

    /** Traduce el estado de un mensaje de Twilio (queued, sent, delivered, read, undelivered, failed...). */
    public static EstadoWhatsApp deTwilio(String estadoTwilio, Integer codigoError) {
        String e = estadoTwilio == null ? "" : estadoTwilio.toLowerCase(Locale.ROOT);
        return switch (e) {
            case "delivered" -> new EstadoWhatsApp(ENTREGADO, null);
            case "read" -> new EstadoWhatsApp(LEIDO, null);
            case "failed", "undelivered", "canceled" -> new EstadoWhatsApp(
                    codigoError != null && (codigoError == NO_SE_UNIO_AL_SANDBOX || codigoError == FUERA_DE_LAS_24_HORAS)
                            ? SIN_ACTIVAR : FALLIDO, codigoError);
            default -> new EstadoWhatsApp(ENVIANDO, null); // accepted, queued, sending, sent
        };
    }

    /** true si ya no va a cambiar (no hace falta seguir preguntando). */
    public boolean terminado() {
        return !ENVIANDO.equals(estado);
    }

    /** true si se puede intentar otra vez (falló o la persona no había activado WhatsApp). */
    public boolean sePuedeReintentar() {
        return SIN_ACTIVAR.equals(estado) || FALLIDO.equals(estado);
    }
}
