package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dto.ArmarPaqueteRequest;
import com.travelgo.dto.PaqueteRealRespuestaDTO;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import com.travelgo.proveedor.moneda.TasaCambio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Arma paquetes con un hotel real: consulta el precio de referencia, lo convierte a pesos, guarda una
 * copia del hotel con su fuente, su fecha de consulta y su enlace, y lo une al paquete.
 * No reserva nada en el hotel real.
 */
@Service
public class PaqueteRealService {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    private final ProveedorHoteles proveedorHoteles;
    private final ProveedorTasaCambio proveedorTasa;
    private final ServicioReservableDAO servicioDAO;
    private final PaqueteDAO paqueteDAO;
    private final DestinoDAO destinoDAO;
    private final PaqueteServicioDAO paqueteServicioDAO;

    public PaqueteRealService(ProveedorHoteles proveedorHoteles, ProveedorTasaCambio proveedorTasa,
                              ServicioReservableDAO servicioDAO, PaqueteDAO paqueteDAO,
                              DestinoDAO destinoDAO, PaqueteServicioDAO paqueteServicioDAO) {
        this.proveedorHoteles = proveedorHoteles;
        this.proveedorTasa = proveedorTasa;
        this.servicioDAO = servicioDAO;
        this.paqueteDAO = paqueteDAO;
        this.destinoDAO = destinoDAO;
        this.paqueteServicioDAO = paqueteServicioDAO;
    }

    /**
     * Todo va en una transacción: si cualquier paso falla (API caída, hotel sin tarifas, destino
     * inexistente), no se guarda nada. Las validaciones y consultas van antes de guardar.
     */
    @Transactional
    public PaqueteRealRespuestaDTO armar(ArmarPaqueteRequest request) {
        int habitaciones = request.habitaciones() == null
                ? ProveedorHoteles.HABITACIONES_POR_DEFECTO : request.habitaciones();
        int adultos = request.adultos() == null ? ProveedorHoteles.ADULTOS_POR_DEFECTO : request.adultos();

        List<Destino> destinos = resolverDestinos(request.destinosIds());
        List<ServicioReservable> extras = resolverServiciosExtra(request.serviciosExtraIds());

        HotelResumen resumen = buscarResumen(request.hotelClave());
        OfertaHotel oferta = proveedorHoteles
                .consultarTarifa(request.hotelClave(), request.entrada(), request.salida(), habitaciones, adultos)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No hay tarifas para ese hotel y esas fechas."));

        Hotel hotel = construirHotel(request, resumen, oferta, habitaciones, adultos);

        double total = hotel.calcularCosto();
        for (ServicioReservable extra : extras) {
            total += extra.calcularCosto();
        }

        PaqueteTuristico paquete = new PaqueteTuristico(request.nombre(), request.descripcion(),
                BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP), oferta.noches() + 1);
        paquete.setDestinos(destinos);

        servicioDAO.guardar(hotel);
        PaqueteTuristico guardado = paqueteDAO.guardar(paquete);
        paqueteServicioDAO.vincular(guardado.getId(), hotel.getId());
        for (ServicioReservable extra : extras) {
            paqueteServicioDAO.vincular(guardado.getId(), extra.getId());
        }

        List<ServicioReservable> servicios = new ArrayList<>();
        servicios.add(hotel);
        servicios.addAll(extras);
        return PaqueteRealRespuestaDTO.desde(guardado, servicios);
    }

    public PaqueteRealRespuestaDTO detalle(Long paqueteId) {
        PaqueteTuristico paquete = paqueteDAO.buscarPorId(paqueteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Paquete no encontrado: " + paqueteId));
        return PaqueteRealRespuestaDTO.desde(paquete, paqueteServicioDAO.listarServicios(paqueteId));
    }

    /** Convierte la oferta consultada en un Hotel con su referencia. El precio queda en pesos, por noche. */
    private Hotel construirHotel(ArmarPaqueteRequest request, HotelResumen resumen, OfertaHotel oferta,
                                 int habitaciones, int adultos) {
        double tarifaPorNoche = oferta.mejorTarifa().precioPorNoche() * habitaciones;
        double precioPorNocheCop;
        String fuente;

        if ("USD".equalsIgnoreCase(oferta.moneda())) {
            TasaCambio tasa = proveedorTasa.obtenerUsdACop();
            precioPorNocheCop = tasa.convertirUsdACop(tarifaPorNoche);
            fuente = String.format(Locale.ROOT, "Xotelo · %s · 1 USD = %.2f COP",
                    oferta.mejorTarifa().agencia(), tasa.copPorUsd());
        } else if ("COP".equalsIgnoreCase(oferta.moneda())) {
            precioPorNocheCop = tarifaPorNoche;
            fuente = "Xotelo · " + oferta.mejorTarifa().agencia();
        } else {
            throw new IllegalStateException("Moneda no soportada: " + oferta.moneda());
        }
        if (fuente.length() > 100) {
            fuente = fuente.substring(0, 100);
        }

        return new Hotel(UUID.randomUUID().toString(), precioPorNocheCop, true, resumen.nombre(),
        direccion(request.ciudadNombre(), resumen), describirHabitacion(habitaciones, adultos),
        oferta.noches(), fuente, request.hotelClave(),
        LocalDateTime.ofInstant(oferta.fechaConsulta(), ZONA), resumen.enlace(), request.entrada());
    }

    /** La fuente no informa el tipo de habitación: se describe lo que sí se consultó. */
    static String describirHabitacion(int habitaciones, int adultos) {
        String personas = adultos == 1 ? "1 adulto" : adultos + " adultos";
        String cuarto = habitaciones == 1 ? "Habitación" : habitaciones + " habitaciones";
        return cuarto + " para " + personas + " (tarifa más baja disponible)";
    }

    /** La fuente no entrega la dirección: se usa la ciudad y las coordenadas del hotel. */
    static String direccion(String ciudad, HotelResumen resumen) {
        if (resumen.latitud() == null || resumen.longitud() == null) {
            return ciudad;
        }
        return String.format(Locale.ROOT, "%s (lat %.4f, lon %.4f)", ciudad, resumen.latitud(), resumen.longitud());
    }

    /** Busca el hotel entre los de su ciudad (la clave del hotel empieza con la de la ciudad: g297476-d...). */
    private HotelResumen buscarResumen(String hotelClave) {
        int guion = hotelClave.indexOf('-');
        if (guion <= 0) {
            throw new IllegalArgumentException(
                    "La clave del hotel no tiene el formato esperado (por ejemplo, g297476-d8021569).");
        }
        String ciudadClave = hotelClave.substring(0, guion);
        return proveedorHoteles.buscarHoteles(ciudadClave, 100).stream()
                .filter(h -> hotelClave.equals(h.clave()))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el hotel " + hotelClave + " entre los hoteles de su ciudad."));
    }

    private List<Destino> resolverDestinos(List<Long> ids) {
        List<Destino> destinos = new ArrayList<>();
        for (Long id : ids) {
            destinos.add(destinoDAO.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Destino no encontrado: " + id)));
        }
        return destinos;
    }

    private List<ServicioReservable> resolverServiciosExtra(List<String> ids) {
        List<ServicioReservable> servicios = new ArrayList<>();
        if (ids == null) {
            return servicios;
        }
        for (String id : ids) {
            servicios.add(servicioDAO.buscarPorId(id)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id)));
        }
        return servicios;
    }
}