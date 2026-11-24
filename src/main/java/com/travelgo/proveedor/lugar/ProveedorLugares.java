package com.travelgo.proveedor.lugar;

import java.util.List;

/** Contrato de cualquier fuente de lugares. Cambiar de API es crear otro adaptador. */
public interface ProveedorLugares {

    List<LugarMapa> buscar(CategoriaMapa categoria, double latitud, double longitud, int radioMetros, int limite);
}