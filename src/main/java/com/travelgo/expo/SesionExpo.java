package com.travelgo.expo;

import jakarta.servlet.http.HttpSession;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Recuerda en la sesión del celular qué viajes lanzó ese visitante (no tiene cuenta).
 * Sirve para dos cosas:
 * - Solo quien lanzó un viaje puede dejar o borrar su celular en él.
 * - El embudo del stand cuenta solo las visitas del propio visitante, no las del administrador ni las de
 *   alguien que abrió el enlace de otro.
 */
final class SesionExpo {

    private static final String ATRIBUTO = "travelgo.expo.viajes";

    private SesionExpo() {
    }

    static void recordar(HttpSession sesion, long viajeId) {
        synchronized (sesion) {
            viajes(sesion).add(viajeId);
        }
    }

    static boolean esPropio(HttpSession sesion, long viajeId) {
        synchronized (sesion) {
            return viajes(sesion).contains(viajeId);
        }
    }

    /** @throws IllegalArgumentException si el viaje no se lanzó desde este celular */
    static void exigirPropio(HttpSession sesion, long viajeId) {
        if (!esPropio(sesion, viajeId)) {
            throw new IllegalArgumentException("Este viaje no se lanzó desde este celular.");
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> viajes(HttpSession sesion) {
        Object guardado = sesion.getAttribute(ATRIBUTO);
        if (guardado instanceof Set<?> set) {
            return (Set<Long>) set;
        }
        Set<Long> nuevo = Collections.synchronizedSet(new LinkedHashSet<>());
        sesion.setAttribute(ATRIBUTO, nuevo);
        return nuevo;
    }
}
