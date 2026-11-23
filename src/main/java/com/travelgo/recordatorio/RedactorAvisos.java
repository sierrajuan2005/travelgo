package com.travelgo.recordatorio;

import com.travelgo.model.Factura;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.web.FormatoWeb;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Escribe el texto de cada aviso (asunto, texto del correo y texto corto del SMS). Separado del servicio para
 * que cambiar un mensaje no obligue a tocar las reglas ni los canales.
 */
@Component
public class RedactorAvisos {

    private static final String FIRMA = "\n\nTravelGo · Este es un aviso automático. "
            + "Puedes desactivar los avisos en \"Mis avisos\" dentro de la página.";

    private final FormatoWeb formato;

    public RedactorAvisos(FormatoWeb formato) {
        this.formato = formato;
    }

    /**
     * Redacta el aviso.
     *
     * @param tipo    tipo de aviso
     * @param reserva reserva a la que se refiere
     * @param factura factura de la reserva, si existe (para el resumen del pago)
     * @param pago    pago de la reserva, si existe
     */
    public Aviso redactar(TipoAviso tipo, Reserva reserva, Optional<Factura> factura, Optional<Pago> pago) {
        String nombre = reserva.getUsuario().getNombre();
        String viaje = nombreDelViaje(reserva);
        String codigo = codigoCorto(reserva);
        String salida = ReglasRecordatorio.inicioDelViaje(reserva)
                .map(formato::fechaHora)
                .orElse("la fecha de tu reserva");

        return switch (tipo) {
            case VIAJE_7_DIAS -> new Aviso(tipo,
                    "Falta una semana para tu viaje: " + viaje,
                    "Hola " + nombre + ",\n\nTu viaje \"" + viaje + "\" empieza el " + salida
                            + ". Revisa tus documentos y el clima del destino en TravelGo."
                            + "\n\nReserva: " + codigo + FIRMA,
                    "TravelGo: tu viaje " + viaje + " empieza el " + salida + ". Reserva " + codigo);
            case VIAJE_1_DIA -> new Aviso(tipo,
                    "Mañana empieza tu viaje: " + viaje,
                    "Hola " + nombre + ",\n\nMañana empieza tu viaje \"" + viaje + "\" (" + salida
                            + "). Llega con tiempo y lleva tu documento de identidad."
                            + "\n\nReserva: " + codigo + FIRMA,
                    "TravelGo: manana empieza tu viaje " + viaje + " (" + salida + "). Reserva " + codigo);
            case VIAJE_HORAS -> new Aviso(tipo,
                    "Tu viaje empieza en unas horas: " + viaje,
                    "Hola " + nombre + ",\n\nTu viaje \"" + viaje + "\" empieza hoy a las " + salida
                            + ". ¡Buen viaje!\n\nReserva: " + codigo + FIRMA,
                    "TravelGo: tu viaje " + viaje + " sale hoy " + salida + ". Buen viaje!");
            case PAGO_PENDIENTE -> new Aviso(tipo,
                    "Tienes un pago pendiente: " + viaje,
                    "Hola " + nombre + ",\n\nTu reserva " + codigo + " (\"" + viaje + "\"), creada el "
                            + reserva.getFechaReserva() + ", todavía no está pagada. Total: "
                            + formato.cop(reserva.getTotal())
                            + ".\nPuedes pagarla en TravelGo, en \"Mis reservas\"." + FIRMA,
                    "TravelGo: tu reserva " + codigo + " tiene un pago pendiente de "
                            + formato.cop(reserva.getTotal()) + ". Pagala en Mis reservas.");
            case PAGO_CONFIRMADO -> new Aviso(tipo,
                    "Pago confirmado: " + viaje,
                    "Hola " + nombre + ",\n\nRecibimos el pago de tu reserva " + codigo + " (\"" + viaje
                            + "\").\n\n" + resumenFactura(factura, pago, reserva) + FIRMA,
                    "TravelGo: pago confirmado de la reserva " + codigo + " por "
                            + formato.cop(pago.map(Pago::getMonto).orElse(reserva.getTotal())) + ".");
        };
    }

    private String resumenFactura(Optional<Factura> factura, Optional<Pago> pago, Reserva reserva) {
        StringBuilder texto = new StringBuilder("Resumen de la factura\n");
        if (factura.isPresent()) {
            Factura f = factura.get();
            texto.append("  Número: ").append(f.getNumero()).append('\n')
                    .append("  Subtotal: ").append(formato.cop(f.getSubtotal())).append('\n');
            if (f.getDescuento() > 0) {
                texto.append("  Descuento")
                        .append(f.getCupon() == null ? "" : " (cupón " + f.getCupon().getCodigo() + ")")
                        .append(": -").append(formato.cop(f.getDescuento())).append('\n');
            }
            texto.append("  Total: ").append(formato.cop(f.getTotal())).append('\n');
        } else {
            texto.append("  Total: ").append(formato.cop(reserva.getTotal())).append('\n');
        }
        pago.ifPresent(p -> texto.append("  Método: ").append(FormatoWeb.nombreMetodo(p.getMetodo())).append('\n')
                .append("  Fecha del pago: ").append(p.getFechaPago()).append('\n'));
        texto.append("\nPuedes ver o imprimir la factura en \"Mis reservas\".");
        return texto.toString();
    }

    /** Nombre del paquete o, si la reserva no tiene paquete, "tu viaje". */
    static String nombreDelViaje(Reserva reserva) {
        return reserva.getPaqueteTuristico() != null && reserva.getPaqueteTuristico().getNombre() != null
                ? reserva.getPaqueteTuristico().getNombre()
                : "tu viaje";
    }

    /** Los primeros 8 caracteres del id, como en la lista de reservas de la web. */
    static String codigoCorto(Reserva reserva) {
        String id = reserva.getId();
        return "#" + (id.length() > 8 ? id.substring(0, 8) : id).toUpperCase();
    }
}
