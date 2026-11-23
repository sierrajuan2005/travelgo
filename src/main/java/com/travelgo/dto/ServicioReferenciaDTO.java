package com.travelgo.dto;

import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.GamaHotel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Un servicio de un paquete, con su referencia externa y el aviso de que el precio puede cambiar. */
public record ServicioReferenciaDTO(String id, String tipo, String descripcion, double precio, double costo,
                                    String fuente, LocalDateTime fechaConsulta, String enlaceReserva,
                                    String aviso, String idExterno, GamaHotel gama) {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static ServicioReferenciaDTO desde(ServicioReservable s) {
        return desde(s, null);
    }

    /** La gama solo aplica a hoteles; para los demás servicios va null. */
    public static ServicioReferenciaDTO desde(ServicioReservable s, GamaHotel gama) {
        String aviso = s.getFuente() == null ? null
                : "Precio de referencia de " + s.getFuente() + ", consultado el "
                + (s.getFechaConsulta() == null ? "—" : s.getFechaConsulta().format(FORMATO))
                + ". Puede cambiar: confirma el precio en el enlace de reserva.";
        return new ServicioReferenciaDTO(s.getId(), s.getClass().getSimpleName(), describir(s),
                s.getPrecio(), s.calcularCosto(), s.getFuente(), s.getFechaConsulta(),
                s.getEnlaceReserva(), aviso, s.getIdExterno(), gama);
    }

    private static String describir(ServicioReservable s) {
        return switch (s) {
            case Hotel h -> h.getNombre() + " · " + h.getNoches() + " noche(s) · " + h.getTipoHabitacion();
            case Vuelo v -> "Vuelo " + v.getAerolinea() + ", " + v.getOrigen() + " → " + v.getDestino();
            case Transporte t -> t.getTipo() + ", " + t.getOrigen() + " → " + t.getDestino();
            default -> "Servicio " + s.getId();
        };
    }
}