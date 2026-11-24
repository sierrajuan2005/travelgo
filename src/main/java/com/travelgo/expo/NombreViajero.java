package com.travelgo.expo;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Limpia y valida el nombre que el visitante escribe en el celular. El nombre sale en una pantalla grande
 * frente a todo el público, así que solo se aceptan letras y se bloquean groserías comunes (también escritas
 * con números, como "p3ndej0"). El administrador puede ocultar cualquier cosa que se escape (/admin/expo).
 */
public final class NombreViajero {

    public static final int MINIMO = 2;
    public static final int MAXIMO = 20;

    private static final Pattern PERMITIDO = Pattern.compile("[\\p{L} .'-]+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}");

    // Palabras que solo cuentan completas: "marica" está al inicio de "Maricarmen", que es un nombre real.
    private static final List<String> PALABRA_COMPLETA = List.of("marica", "maricas");
    // Raíces cortas: cuentan solo al inicio de una palabra ("Diputado" no es una grosería).
    private static final List<String> AL_INICIO_DE_PALABRA = List.of(
            "puta", "puto", "mierd", "verga", "culo", "pendej", "imbecil", "idiota", "estupid", "perra",
            "zorra", "polla", "nazi", "hitler", "porno", "sexo", "fuck", "shit", "bitch", "hpta");
    // Raíces largas: cuentan en cualquier parte, también con espacios en medio ("hijue puta").
    private static final List<String> EN_CUALQUIER_PARTE = List.of(
            "malparid", "hijueput", "gonorrea", "mamagueva", "careverga", "gonorr", "marikon", "maricon");
    private static final Pattern SEPARADORES = Pattern.compile("[^a-z]+");

    private NombreViajero() {
    }

    /**
     * Devuelve el nombre listo para guardar (sin espacios de más).
     *
     * @throws IllegalArgumentException con un mensaje para mostrar en el celular
     */
    public static String limpiar(String nombre) {
        String limpio = nombre == null ? "" : ESPACIOS.matcher(nombre.strip()).replaceAll(" ");
        if (limpio.length() < MINIMO) {
            throw new IllegalArgumentException("Escribe tu nombre (mínimo " + MINIMO + " letras).");
        }
        if (limpio.length() > MAXIMO) {
            throw new IllegalArgumentException("Tu nombre puede tener máximo " + MAXIMO + " caracteres.");
        }
        if (!PERMITIDO.matcher(limpio).matches()) {
            throw new IllegalArgumentException("Usa solo letras en tu nombre.");
        }
        if (esOfensivo(limpio)) {
            throw new IllegalArgumentException("Ese nombre no se puede mostrar en la pantalla. Prueba con otro.");
        }
        return limpio;
    }

    static boolean esOfensivo(String texto) {
        String base = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD);
        base = MARCAS.matcher(base).replaceAll("");
        base = base.replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a')
                .replace('5', 's').replace('7', 't').replace('@', 'a');
        for (String palabra : SEPARADORES.split(base)) {
            if (PALABRA_COMPLETA.contains(palabra) || AL_INICIO_DE_PALABRA.stream().anyMatch(palabra::startsWith)) {
                return true;
            }
        }
        String compacto = SEPARADORES.matcher(base).replaceAll("");
        return EN_CUALQUIER_PARTE.stream().anyMatch(compacto::contains);
    }
}
