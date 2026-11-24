package com.travelgo.expo;

import java.util.regex.Pattern;

/**
 * Valida y normaliza el celular que el visitante deja para recibir su postal y ofertas (por WhatsApp).
 *
 * - Colombia: 10 dígitos que empiezan por 3 ("300 123 4567", "+57 300-123-4567") → "+573001234567".
 * - Otro país: con "+" y el indicativo, entre 8 y 15 dígitos en total (formato E.164).
 */
public final class TelefonoContacto {

    private static final Pattern SEPARADORES = Pattern.compile("[\\s().-]");
    private static final Pattern CELULAR_COLOMBIA = Pattern.compile("3\\d{9}");
    private static final Pattern INTERNACIONAL = Pattern.compile("\\+[1-9]\\d{7,14}");

    private TelefonoContacto() {
    }

    /**
     * @return el número en formato internacional, por ejemplo "+573001234567"
     * @throws IllegalArgumentException con un mensaje para mostrar en el celular
     */
    public static String normalizar(String telefono) {
        String limpio = telefono == null ? "" : SEPARADORES.matcher(telefono.strip()).replaceAll("");
        if (limpio.startsWith("+57")) {
            limpio = limpio.substring(3);
        } else if (limpio.startsWith("57") && limpio.length() == 12) {
            limpio = limpio.substring(2);
        }
        if (CELULAR_COLOMBIA.matcher(limpio).matches()) {
            return "+57" + limpio;
        }
        if (INTERNACIONAL.matcher(limpio).matches()) {
            return limpio;
        }
        throw new IllegalArgumentException(
                "Escribe tu celular de 10 dígitos (ej: 300 123 4567) o con el indicativo de tu país (+...).");
    }

    /** "+573001234567" → "+57 300 ••• 4567", para mostrarlo sin exponerlo completo. */
    public static String enmascarar(String telefono) {
        if (telefono == null || telefono.length() < 8) {
            return "";
        }
        if (telefono.startsWith("+57") && telefono.length() == 13) {
            return "+57 " + telefono.substring(3, 6) + " ••• " + telefono.substring(9);
        }
        return telefono.substring(0, 4) + " ••• " + telefono.substring(telefono.length() - 4);
    }
}
