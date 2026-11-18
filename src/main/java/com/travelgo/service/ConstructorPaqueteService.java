package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.dto.ArmarPaqueteRequest;
import com.travelgo.dto.PaqueteRealRespuestaDTO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Transporte;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import com.travelgo.proveedor.moneda.ProveedorTasaCambio;
import com.travelgo.proveedor.moneda.TasaCambio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Constructor de paquetes de la web (Sprint 4, Integrante 3). Agrega a {@link PaqueteRealService}
 * (Integrante 2, que arma el paquete con el hotel real) lo que necesita la pantalla:
 * <ul>
 *   <li><b>Buscar</b>: vuelos (Ignav) y hoteles con su tarifa ya en pesos (Xotelo + tasa de cambio real),
 *       para que el administrador compare y elija.</li>
 *   <li><b>Armar</b>: guarda una copia del vuelo elegido (con su enlace de reserva) y luego delega en
 *       {@code PaqueteRealService.armar}, pasándole el vuelo como servicio extra. Todo en una transacción.</li>
 * </ul>
 * Precio de referencia = vuelo de ida (1 adulto) + hotel (1 habitación, 2 adultos) × noches.
 * <p>
 * GRASP: Fabricación Pura que coordina proveedores y services para la pantalla; no repite el armado del
 * hotel, que es de PaqueteRealService. SOLID (D): depende de interfaces ({@link ProveedorVuelos},
 * {@link ProveedorHoteles}, {@link ProveedorTasaCambio}), no de Ignav, Xotelo ni open.er-api.
 */
@Service
public class ConstructorPaqueteService {

    private static final Logger log = LoggerFactory.getLogger(ConstructorPaqueteService.class);
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Hoteles que se piden a Xotelo (luego se consulta la tarifa de cada uno). */
    static final int LIMITE_HOTELES = 8;

    private final ProveedorVuelos proveedorVuelos;
    private final ProveedorHoteles proveedorHoteles;
    private final ProveedorTasaCambio proveedorTasa;
    private final PaqueteRealService paqueteRealService;
    private final DestinoDAO destinoDAO;
    private final ServicioReservableDAO servicioDAO;
    private final PaqueteServicioDAO paqueteServicioDAO;

    public ConstructorPaqueteService(ProveedorVuelos proveedorVuelos, ProveedorHoteles proveedorHoteles,
            ProveedorTasaCambio proveedorTasa, PaqueteRealService paqueteRealService, DestinoDAO destinoDAO,
            ServicioReservableDAO servicioDAO, PaqueteServicioDAO paqueteServicioDAO) {
        this.proveedorVuelos = proveedorVuelos;
        this.proveedorHoteles = proveedorHoteles;
        this.proveedorTasa = proveedorTasa;
        this.paqueteRealService = paqueteRealService;
        this.destinoDAO = destinoDAO;
        this.servicioDAO = servicioDAO;
        this.paqueteServicioDAO = paqueteServicioDAO;
    }

    /**
     * Datos de la búsqueda: destino de TravelGo, aeropuertos (IATA), clave de la ciudad en Xotelo y fechas.
     */
    public record Solicitud(Long destinoId, String origen, String codigoDestino, String claveHoteles,
                            LocalDate ida, LocalDate vuelta) {

        /** Noches de hotel entre la ida y la vuelta. */
        public int noches() {
            return (int) ChronoUnit.DAYS.between(ida, vuelta);
        }

        /** @throws IllegalArgumentException si falta algo o las fechas no tienen sentido */
        public void validar() {
            if (destinoId == null) {
                throw new IllegalArgumentException("Elige un destino.");
            }
            if (vacio(origen) || vacio(codigoDestino)) {
                throw new IllegalArgumentException("Los códigos de aeropuerto de origen y destino son obligatorios.");
            }
            if (origen.trim().equalsIgnoreCase(codigoDestino.trim())) {
                throw new IllegalArgumentException("El origen y el destino no pueden ser el mismo aeropuerto.");
            }
            if (vacio(claveHoteles)) {
                throw new IllegalArgumentException("La clave de la ciudad para los hoteles es obligatoria.");
            }
            if (ida == null || vuelta == null) {
                throw new IllegalArgumentException("Las fechas de ida y vuelta son obligatorias.");
            }
            if (ida.isBefore(LocalDate.now(ZONA))) {
                throw new IllegalArgumentException("La fecha de ida no puede estar en el pasado.");
            }
            if (!vuelta.isAfter(ida)) {
                throw new IllegalArgumentException("La vuelta debe ser después de la ida.");
            }
            if (noches() > 30) {
                throw new IllegalArgumentException("El paquete puede tener como máximo 30 noches.");
            }
        }
    }

    /** Un hotel con su tarifa para las fechas pedidas, ya en COP. */
    public record OpcionHotel(HotelResumen hotel, OfertaHotel oferta, double precioNocheCop, double totalCop) {
    }

    /**
     * Lo que encontró la búsqueda. {@code tasa} es la tasa USD→COP usada (null si no se pudo consultar).
     * Los avisos explican por qué falta algo (sin llave, API caída...).
     */
    public record Resultado(Destino destino, List<Vuelo> vuelos, List<OpcionHotel> hoteles, TasaCambio tasa,
                            List<String> avisos) {
    }

    /**
     * Busca vuelos y hoteles reales. Nunca falla por una API: si una no responde, devuelve la lista vacía
     * y un aviso.
     *
     * @throws IllegalArgumentException     si la solicitud no es válida
     * @throws RecursoNoEncontradoException si el destino no existe
     */
    public Resultado buscar(Solicitud solicitud) {
        solicitud.validar();
        Destino destino = destino(solicitud.destinoId());
        List<String> avisos = new ArrayList<>();

        List<Vuelo> vuelos = buscarVuelos(solicitud, avisos);
        TasaCambio tasa = null;
        List<OpcionHotel> hoteles = List.of();
        try {
            tasa = proveedorTasa.obtenerUsdACop();
            hoteles = buscarHoteles(solicitud, tasa, avisos);
        } catch (RuntimeException e) {
            log.warn("Sin tasa de cambio: {}", e.getMessage());
            avisos.add("No se pudo consultar la tasa de cambio USD → COP, así que no se pueden mostrar los precios "
                    + "de los hoteles. Intenta de nuevo en un momento.");
        }
        return new Resultado(destino, vuelos, hoteles, tasa, avisos);
    }

    /**
     * Guarda una copia del vuelo elegido y arma el paquete con el hotel elegido usando
     * {@link PaqueteRealService#armar}. Todo o nada.
     *
     * @param vueloId    id del vuelo elegido en la búsqueda (puede ser null: paquete solo con hotel)
     * @param hotelClave clave del hotel elegido (obligatoria: PaqueteRealService arma alrededor del hotel)
     * @throws IllegalArgumentException si faltan datos
     * @throws IllegalStateException    si la búsqueda de vuelos venció (Ignav guarda los resultados 30 minutos)
     */
    @Transactional
    public PaqueteRealRespuestaDTO armar(Solicitud solicitud, String vueloId, String hotelClave, String nombre,
            String descripcion) {
        solicitud.validar();
        if (vacio(nombre)) {
            throw new IllegalArgumentException("El nombre del paquete es obligatorio.");
        }
        if (vacio(hotelClave)) {
            throw new IllegalArgumentException("Elige un hotel: el paquete se arma alrededor del hotel.");
        }
        Destino destino = destino(solicitud.destinoId());

        List<String> extras = new ArrayList<>();
        String textoVuelo = null;
        if (!vacio(vueloId)) {
            Vuelo vuelo = copiaDelVuelo(solicitud, vueloId.trim());
            servicioDAO.guardar(vuelo);
            extras.add(vuelo.getId());
            textoVuelo = "Vuelo " + vuelo.getAerolinea() + " " + vuelo.getOrigen() + " → " + vuelo.getDestino()
                    + " el " + vuelo.getFechaSalida().format(FECHA);
        }

        String texto = vacio(descripcion) ? descripcionAutomatica(textoVuelo, solicitud) : descripcion.trim();
        ArmarPaqueteRequest request = new ArmarPaqueteRequest(nombre.trim(), recortar(texto),
                List.of(destino.getId()), destino.getCiudad(), hotelClave.trim(), solicitud.ida(), solicitud.vuelta(),
                ProveedorHoteles.HABITACIONES_POR_DEFECTO, ProveedorHoteles.ADULTOS_POR_DEFECTO, extras);
        PaqueteRealRespuestaDTO paquete = paqueteRealService.armar(request);
        log.info("Paquete {} armado desde la web con {} servicio(s), precio {}", paquete.id(),
                paquete.servicios().size(), paquete.precioReferencia());
        return paquete;
    }

    /**
     * Servicios guardados con el paquete, primero el vuelo, luego el hotel y luego el transporte.
     * Vacío si el paquete se creó a mano.
     */
    public List<ServicioReservable> serviciosDe(Long paqueteId) {
        return paqueteServicioDAO.listarServicios(paqueteId).stream()
                .sorted(Comparator.comparingInt(ConstructorPaqueteService::orden))
                .toList();
    }

    private static int orden(ServicioReservable s) {
        return switch (s) {
            case Vuelo v -> 1;
            case Hotel h -> 2;
            case Transporte t -> 3;
            default -> 4;
        };
    }

    // ---------- Búsqueda ----------

    private List<Vuelo> buscarVuelos(Solicitud s, List<String> avisos) {
        try {
            List<Vuelo> vuelos = proveedorVuelos.buscarVuelos(s.origen().trim(), s.codigoDestino().trim(), s.ida(), null)
                    .stream().sorted(Comparator.comparingDouble(Vuelo::getPrecio)).toList();
            if (vuelos.isEmpty()) {
                avisos.add("Ignav no devolvió vuelos directos con precio en COP para " + s.origen().toUpperCase()
                        + " → " + s.codigoDestino().toUpperCase() + " el " + s.ida().format(FECHA) + ".");
            }
            return vuelos;
        } catch (IllegalStateException e) {
            avisos.add("No se pueden buscar vuelos: falta la llave de Ignav (variable de entorno IGNAV_API_KEY). "
                    + "Puedes armar el paquete solo con el hotel.");
        } catch (RuntimeException e) {
            log.warn("Fallo al buscar vuelos: {}", e.getMessage());
            avisos.add("No se pudo consultar Ignav: " + e.getMessage());
        }
        return List.of();
    }

    private List<OpcionHotel> buscarHoteles(Solicitud s, TasaCambio tasa, List<String> avisos) {
        List<HotelResumen> encontrados;
        try {
            encontrados = proveedorHoteles.buscarHoteles(s.claveHoteles().trim(), LIMITE_HOTELES);
        } catch (RuntimeException e) {
            log.warn("Fallo al buscar hoteles: {}", e.getMessage());
            avisos.add("No se pudo consultar Xotelo: " + e.getMessage());
            return List.of();
        }

        List<OpcionHotel> opciones = new ArrayList<>();
        int sinTarifa = 0;
        for (HotelResumen hotel : encontrados) {
            try {
                Optional<OpcionHotel> opcion = proveedorHoteles.consultarTarifa(hotel.clave(), s.ida(), s.vuelta(),
                                ProveedorHoteles.HABITACIONES_POR_DEFECTO, ProveedorHoteles.ADULTOS_POR_DEFECTO)
                        .flatMap(oferta -> opcion(hotel, oferta, tasa));
                if (opcion.isPresent()) {
                    opciones.add(opcion.get());
                } else {
                    sinTarifa++;
                }
            } catch (RuntimeException e) {
                sinTarifa++;
            }
        }
        if (encontrados.isEmpty()) {
            avisos.add("Xotelo no encontró hoteles para la clave " + s.claveHoteles() + ".");
        } else if (sinTarifa > 0) {
            avisos.add(sinTarifa + " de " + encontrados.size() + " hoteles no tienen tarifa para esas fechas.");
        }
        opciones.sort(Comparator.comparingDouble(OpcionHotel::totalCop));
        return opciones;
    }

    /** Mismo cálculo que PaqueteRealService: tarifa por noche × habitaciones, convertida a pesos. */
    private Optional<OpcionHotel> opcion(HotelResumen hotel, OfertaHotel oferta, TasaCambio tasa) {
        double noche = oferta.mejorTarifa().precioPorNoche() * oferta.habitaciones();
        Double nocheCop;
        if ("USD".equalsIgnoreCase(oferta.moneda())) {
            nocheCop = tasa.convertirUsdACop(noche);
        } else if ("COP".equalsIgnoreCase(oferta.moneda())) {
            nocheCop = noche;
        } else {
            return Optional.empty();
        }
        return Optional.of(new OpcionHotel(hotel, oferta, nocheCop, nocheCop * oferta.noches()));
    }

    // ---------- Armado ----------

    /**
     * Vuelve a pedir la misma búsqueda (Ignav la tiene en caché 30 minutos, con los mismos ids) y copia el vuelo
     * elegido con su enlace de reserva.
     */
    private Vuelo copiaDelVuelo(Solicitud s, String vueloId) {
        Vuelo elegido;
        try {
            elegido = proveedorVuelos.buscarVuelos(s.origen().trim(), s.codigoDestino().trim(), s.ida(), null).stream()
                    .filter(v -> v.getId().equals(vueloId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "La búsqueda de vuelos venció (dura 30 minutos). Busca otra vez y elige el vuelo."));
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new ProveedorNoDisponibleException("No se pudo consultar Ignav: " + e.getMessage(), e);
        }

        String enlace = null;
        try {
            enlace = proveedorVuelos.obtenerEnlaceReserva(elegido.getIdExterno());
        } catch (RuntimeException e) {
            log.warn("Sin enlace de reserva para el vuelo {}: {}", elegido.getIdExterno(), e.getMessage());
        }
        // Disponible en TravelGo: el administrador lo ofrece como referencia (no se compra el tiquete aquí)
        return new Vuelo(UUID.randomUUID().toString(), elegido.getPrecio(), true, elegido.getAerolinea(),
                elegido.getOrigen(), elegido.getDestino(), elegido.getFechaSalida(), elegido.getFechaLlegada(),
                elegido.getFuente(), elegido.getIdExterno(), elegido.getFechaConsulta(), enlace);
    }

    /** Descripción automática cuando el administrador no escribe una. */
    private static String descripcionAutomatica(String textoVuelo, Solicitud s) {
        String hotel = s.noches() + " noche(s) de hotel";
        return (textoVuelo == null ? hotel : textoVuelo + " + " + hotel)
                + ". Precio de referencia consultado el " + LocalDate.now(ZONA).format(FECHA) + ".";
    }

    private Destino destino(Long id) {
        return destinoDAO.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Destino no encontrado: " + id));
    }

    private static String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 497) + "...";
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
