package com.travelgo.model;

/**
 * Lo que le gusta hacer a un viajero. Con esto cada persona del grupo recibe su propio plan:
 * en Roma uno puede ir de fiesta y otro a los museos, aunque viajen en la misma reserva.
 */
public enum Gusto {
    FIESTA("Fiesta y vida nocturna", "bi-music-note-beamed", true),
    CULTURA("Museos y arte", "bi-palette", false),
    HISTORIA("Historia y monumentos", "bi-bank", false),
    RELIGION("Iglesias y templos", "bi-brightness-alt-high", false),
    NATURALEZA("Naturaleza y playa", "bi-tree", false),
    GASTRONOMIA("Comida local", "bi-cup-hot", false),
    COMPRAS("Compras", "bi-bag", false),
    FAMILIA("Planes en familia", "bi-balloon", false);

    private final String etiqueta;
    private final String icono;
    private final boolean deNoche;

    Gusto(String etiqueta, String icono, boolean deNoche) {
        this.etiqueta = etiqueta;
        this.icono = icono;
        this.deNoche = deNoche;
    }

    /** Nombre para mostrar en la web. */
    public String getEtiqueta() { return etiqueta; }

    /** Clase de Bootstrap Icons. */
    public String getIcono() { return icono; }

    /** true si el plan va mejor de noche (la fiesta); el resto se pone de mañana o de tarde. */
    public boolean isDeNoche() { return deNoche; }
}
