package com.travelgo.integracion;

import com.travelgo.model.Vuelo;
import java.time.LocalDate;
import java.util.List;

public interface ProveedorVuelos {
    List<Vuelo> buscarVuelos(
        String origen,
        String destino,
        LocalDate fechaSalida,
        String codigoAerolinea
    );

    String obtenerEnlaceReserva(String idExterno);
}
