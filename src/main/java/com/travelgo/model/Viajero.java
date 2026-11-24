package com.travelgo.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Una persona que viaja en una reserva. Cada viajero tiene sus propios gustos y sus propias
 * actividades pagas, así el grupo no tiene que hacer todo junto.
 */
public class Viajero {

    public static final int LARGO_MAXIMO_NOMBRE = 40;

    private final String id;
    private final String nombre;
    private final Set<Gusto> gustos;
    private final List<Actividad> actividades;

    /** Viajero nuevo (todavía no está en la base de datos). */
    public Viajero(String nombre, Set<Gusto> gustos, List<Actividad> actividades) {
        this(UUID.randomUUID().toString(), nombre, gustos, actividades);
    }

    /** Reconstruye un viajero con los datos de la base de datos. */
    public Viajero(String id, String nombre, Set<Gusto> gustos, List<Actividad> actividades) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID del viajero es obligatorio.");
        }
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            throw new IllegalArgumentException("Cada viajero necesita un nombre.");
        }
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new IllegalArgumentException("El nombre \"" + limpio + "\" es muy largo (máximo "
                    + LARGO_MAXIMO_NOMBRE + " letras).");
        }
        this.id = id;
        this.nombre = limpio;
        this.gustos = gustos == null || gustos.isEmpty() ? EnumSet.noneOf(Gusto.class) : EnumSet.copyOf(gustos);
        this.actividades = new ArrayList<>();
        if (actividades != null) {
            for (Actividad actividad : actividades) {
                boolean repetida = this.actividades.stream().anyMatch(a -> a.id().equals(actividad.id()));
                if (!repetida) {
                    this.actividades.add(actividad);
                }
            }
        }
    }

    /** Suma de las actividades pagas de este viajero. */
    public double costoActividades() {
        return actividades.stream().mapToDouble(Actividad::precio).sum();
    }

    public String getId() { return id; }

    public String getNombre() { return nombre; }

    public Set<Gusto> getGustos() { return Collections.unmodifiableSet(gustos); }

    public List<Actividad> getActividades() { return Collections.unmodifiableList(actividades); }
}
