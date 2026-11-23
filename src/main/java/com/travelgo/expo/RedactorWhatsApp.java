package com.travelgo.expo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;

/**
 * Escribe el mensaje de WhatsApp que recibe el visitante después de lanzar su viaje en la ExpoSoftware.
 * Solo arma texto (sin llamar a nadie), así se prueba fácil.
 *
 * Con paquetes: los paquetes de TravelGo a su destino, con precio y enlace para reservar.
 * Sin paquetes: cuántas personas sueñan con ese destino y los paquetes más baratos que sí hay.
 */
public final class RedactorWhatsApp {

    /** WhatsApp corta los mensajes muy largos; Twilio acepta hasta 1.600 caracteres. */
    static final int MAXIMO_CARACTERES = 1600;
    static final int MAXIMO_PAQUETES = 3;

    private RedactorWhatsApp() {
    }

    /** Un paquete listo para el mensaje: precio en COP y enlace completo. */
    public record PaqueteMensaje(String nombre, int dias, double precio, String enlace) {
    }

    /**
     * @param nombre        nombre del visitante
     * @param destino       destino que eligió
     * @param temperatura   temperatura actual en el destino; null si no se pudo consultar
     * @param clima         descripción del clima ("Nublado"); null si no se pudo consultar
     * @param paquetes      paquetes a su destino (vacío si TravelGo no vende allí)
     * @param alternativas  paquetes más baratos de TravelGo, para cuando no hay al destino
     * @param sonadores     personas que soñaron con ese destino en la expo
     * @param enlaceDestino página de su destino (/expo/viaje/{id})
     */
    public static String mensaje(String nombre, DestinoSonado destino, Double temperatura, String clima,
                                 List<PaqueteMensaje> paquetes, List<PaqueteMensaje> alternativas,
                                 long sonadores, String enlaceDestino) {
        StringBuilder m = new StringBuilder();
        m.append("✈️ ¡Hola, ").append(nombre).append("! Tu avión de Valledupar a ")
                .append(destino.ciudad()).append(" ya aterrizó.\n\n");
        m.append("🎫 VUP → ").append(destino.aeropuerto()).append(" · ")
                .append(miles(destino.kmDesdeValledupar())).append(" km\n");
        if (temperatura != null) {
            m.append("🌤️ Hoy en ").append(destino.ciudad()).append(": ").append(Math.round(temperatura)).append(" °C");
            if (clima != null && !clima.isBlank()) {
                m.append(", ").append(clima.toLowerCase());
            }
            m.append('\n');
        }
        m.append('\n');

        if (!paquetes.isEmpty()) {
            m.append("🧳 *Hazlo real con TravelGo*:\n");
            agregarPaquetes(m, paquetes);
        } else {
            m.append("💜 Todavía no vendemos ").append(destino.ciudad()).append(", pero ")
                    .append(sonadores == 1 ? "eres la primera persona que sueña"
                            : "ya son " + sonadores + " personas soñando")
                    .append(" con ir. Te avisamos primero cuando lo lancemos.\n");
            if (!alternativas.isEmpty()) {
                m.append("\nMientras tanto, estos viajes sí los tenemos:\n");
                agregarPaquetes(m, alternativas);
            }
        }

        m.append("\n🌍 Tu destino con clima y lugares para visitar:\n").append(enlaceDestino).append('\n');
        m.append("\nTe escribimos porque lo autorizaste en el stand de TravelGo (ExpoSoftware). "
                + "Si no quieres más mensajes, bórralo en ese mismo enlace.");
        return recortar(m.toString());
    }

    private static void agregarPaquetes(StringBuilder m, List<PaqueteMensaje> paquetes) {
        paquetes.stream().limit(MAXIMO_PAQUETES).forEach(p -> m.append("• ").append(p.nombre())
                .append(" · ").append(p.dias()).append(" días · $ ").append(miles(Math.round(p.precio())))
                .append("\n  ").append(p.enlace()).append('\n'));
    }

    static String miles(long valor) {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
        simbolos.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", simbolos).format(valor);
    }

    private static String recortar(String texto) {
        return texto.length() <= MAXIMO_CARACTERES ? texto : texto.substring(0, MAXIMO_CARACTERES - 1) + "…";
    }
}
