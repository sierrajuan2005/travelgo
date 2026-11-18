package com.travelgo.service;

import com.travelgo.dao.DestinoDAO;
import com.travelgo.dao.PaqueteDAO;
import com.travelgo.dao.PaqueteServicioDAO;
import com.travelgo.dao.ServicioReservableDAO;
import com.travelgo.exception.ProveedorNoDisponibleException;
import com.travelgo.exception.RecursoNoEncontradoException;
import com.travelgo.integracion.ProveedorVuelos;
import com.travelgo.model.Destino;
import com.travelgo.model.Hotel;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Vuelo;
import com.travelgo.proveedor.hotel.HotelResumen;
import com.travelgo.proveedor.hotel.OfertaHotel;
import com.travelgo.proveedor.hotel.ProveedorHoteles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Constructor de paquetes con datos reales (Sprint 4). El administrador busca vuelos (Ignav) y hoteles (Xotelo)
 * para una ruta y unas fechas, elige uno de cada uno y el paquete se guarda con una <b>copia</b> de lo consultado:
 * precio, fecha de consulta, fuente y enlace de reserva. El catálogo ya no vuelve a llamar a las APIs.
 * <p>
 * Precio de referencia del paquete = vuelo de ida (1 adulto) + hotel (1 habitación, 2 adultos) × noches.
 * Xotelo responde en USD; se convierte a COP con una tasa fija configurable ({@code travelgo.moneda.usd-cop}).
 * <p>
 * GRASP: es una Fabricación Pura que coordina dos proveedores y cuatro DAO, para que ni el controlador ni
 * {@code PaqueteService} tengan que conocer las APIs. SOLID (D): depende de {@link ProveedorVuelos} y
 * {@link ProveedorHoteles}, no de Ignav ni de Xotelo.
 */
@Service
public class ConstructorPaqueteService {

    private static final Logger log = LoggerFactory.getLogger(ConstructorPaqueteService.class);
    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Hoteles que se piden a Xotelo (luego se consulta la tarifa de cada uno). */
    static final int LIMITE_HOTELES = 8;
    static final String TIPO_HABITACION = "Doble (2 adultos)";

    private final ProveedorVuelos proveedorVuelos;
    private final ProveedorHoteles proveedorHoteles;
    private final DestinoDAO destinoDAO;
    private final PaqueteDAO paqueteDAO;
    private final ServicioReservableDAO servicioDAO;
    private final PaqueteServicioDAO paqueteServicioDAO;
    private final BigDecimal tasaUsdCop;

    public ConstructorPaqueteService(ProveedorVuelos proveedorVuelos, ProveedorHoteles proveedorHoteles,
            DestinoDAO destinoDAO, PaqueteDAO paqueteDAO, ServicioReservableDAO servicioDAO,
            PaqueteServicioDAO paqueteServicioDAO,
            @Value("${travelgo.moneda.usd-cop:4000}") BigDecimal tasaUsdCop) {
        this.proveedorVuelos = proveedorVuelos;
        this.proveedorHoteles = proveedorHoteles;
        this.destinoDAO = destinoDAO;
        this.paqueteDAO = paqueteDAO;
        this.servicioDAO = servicioDAO;
        this.paqueteServicioDAO = paqueteServicioDAO;
        this.tasaUsdCop = tasaUsdCop;
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

    /** Lo que encontró la búsqueda. Los avisos explican por qué falta algo (sin llave, API caída...). */
    public record Resultado(Destino destino, List<Vuelo> vuelos, List<OpcionHotel> hoteles, List<String> avisos) {
    }

    /** Tasa con la que se convierten los precios en USD. */
    public BigDecimal tasaUsdCop() {
        return tasaUsdCop;
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
        List<OpcionHotel> hoteles = buscarHoteles(solicitud, avisos);
        return new Resultado(destino, vuelos, hoteles, avisos);
    }

    /**
     * Guarda el paquete con una copia del vuelo y del hotel elegidos (con su fecha de consulta, fuente y enlace),
     * y los une al paquete en la tabla paquete_servicio. Todo o nada.
     *
     * @param vueloId    id del vuelo elegido en la búsqueda (puede ser null si solo se elige hotel)
     * @param hotelClave clave del hotel elegido (puede ser null si solo se elige vuelo)
     * @throws IllegalArgumentException si no se eligió nada o faltan datos
     * @throws IllegalStateException    si la búsqueda venció (Ignav guarda los resultados 30 minutos)
     */
    @Transactional
    public PaqueteTuristico armar(Solicitud solicitud, String vueloId, String hotelClave, String nombre,
            String descripcion) {
        solicitud.validar();
        if (vacio(nombre)) {
            throw new IllegalArgumentException("El nombre del paquete es obligatorio.");
        }
        if (vacio(vueloId) && vacio(hotelClave)) {
            throw new IllegalArgumentException("Elige al menos un vuelo o un hotel.");
        }
        Destino destino = destino(solicitud.destinoId());

        List<ServicioReservable> servicios = new ArrayList<>();
        if (!vacio(vueloId)) {
            servicios.add(copiaDelVuelo(solicitud, vueloId.trim()));
        }
        if (!vacio(hotelClave)) {
            servicios.add(copiaDelHotel(solicitud, hotelClave.trim(), destino));
        }

        BigDecimal precio = BigDecimal.valueOf(servicios.stream().mapToDouble(ServicioReservable::calcularCosto).sum())
                .setScale(0, RoundingMode.HALF_UP);
        PaqueteTuristico paquete = new PaqueteTuristico(nombre.trim(),
                vacio(descripcion) ? resumen(servicios) : recortar(descripcion.trim()),
                precio, solicitud.noches() + 1);
        paquete.setDestinos(new ArrayList<>(List.of(destino)));
        paquete.setActivo(true);

        servicios.forEach(servicioDAO::guardar);
        PaqueteTuristico guardado = paqueteDAO.guardar(paquete);
        servicios.forEach(s -> paqueteServicioDAO.asociar(guardado.getId(), s.getId()));
        log.info("Paquete {} armado con {} servicio(s) reales, precio {}", guardado.getId(), servicios.size(), precio);
        return guardado;
    }

    /** Servicios guardados con el paquete (vuelo y hotel). Vacío si el paquete se creó a mano. */
    public List<ServicioReservable> serviciosDe(Long paqueteId) {
        List<ServicioReservable> servicios = new ArrayList<>();
        for (String id : paqueteServicioDAO.serviciosDe(paqueteId)) {
            servicioDAO.buscarPorId(id).ifPresent(servicios::add);
        }
        return servicios;
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
            avisos.add("No se pueden buscar vuelos: falta la llave de Ignav (variable de entorno IGNAV_API_KEY).");
        } catch (RuntimeException e) {
            log.warn("Fallo al buscar vuelos: {}", e.getMessage());
            avisos.add("No se pudo consultar Ignav: " + e.getMessage());
        }
        return List.of();
    }

    private List<OpcionHotel> buscarHoteles(Solicitud s, List<String> avisos) {
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
                        .flatMap(oferta -> opcion(hotel, oferta));
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

    private Optional<OpcionHotel> opcion(HotelResumen hotel, OfertaHotel oferta) {
        return aCop(oferta.mejorTarifa().precioPorNoche(), oferta.moneda())
                .map(noche -> new OpcionHotel(hotel, oferta, noche, noche * oferta.noches() * oferta.habitaciones()));
    }

    /** Convierte a COP (redondeado a pesos). Vacío si la moneda no es COP ni USD. */
    Optional<Double> aCop(double valor, String moneda) {
        if ("COP".equalsIgnoreCase(moneda)) {
            return Optional.of((double) Math.round(valor));
        }
        if ("USD".equalsIgnoreCase(moneda)) {
            return Optional.of(BigDecimal.valueOf(valor).multiply(tasaUsdCop)
                    .setScale(0, RoundingMode.HALF_UP).doubleValue());
        }
        return Optional.empty();
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

    /** Pide la tarifa del hotel elegido (Xotelo la tiene en caché) y la copia ya convertida a COP. */
    private Hotel copiaDelHotel(Solicitud s, String hotelClave, Destino destino) {
        OfertaHotel oferta = proveedorHoteles.consultarTarifa(hotelClave, s.ida(), s.vuelta(),
                        ProveedorHoteles.HABITACIONES_POR_DEFECTO, ProveedorHoteles.ADULTOS_POR_DEFECTO)
                .orElseThrow(() -> new IllegalStateException("El hotel elegido ya no tiene tarifa para esas fechas."));
        double nocheCop = aCop(oferta.mejorTarifa().precioPorNoche(), oferta.moneda())
                .orElseThrow(() -> new IllegalStateException("No se puede convertir la moneda " + oferta.moneda() + "."));

        Optional<HotelResumen> resumen = proveedorHoteles.buscarHoteles(s.claveHoteles().trim(), LIMITE_HOTELES).stream()
                .filter(h -> hotelClave.equals(h.clave()))
                .findFirst();
        String nombre = resumen.map(HotelResumen::nombre).filter(n -> !vacio(n)).orElse("Hotel " + hotelClave);
        String enlace = resumen.map(HotelResumen::enlace).orElse(null);

        return new Hotel(UUID.randomUUID().toString(), nocheCop, true, nombre,
                destino.getCiudad() + ", " + destino.getPais(), TIPO_HABITACION, oferta.noches(),
                "XOTELO", hotelClave, LocalDateTime.ofInstant(oferta.fechaConsulta(), ZONA), enlace);
    }

    /** Descripción automática cuando el administrador no escribe una. */
    private String resumen(List<ServicioReservable> servicios) {
        List<String> partes = new ArrayList<>();
        for (ServicioReservable s : servicios) {
            if (s instanceof Vuelo v) {
                partes.add("Vuelo " + v.getAerolinea() + " " + v.getOrigen() + " → " + v.getDestino()
                        + " el " + v.getFechaSalida().format(FECHA));
            } else if (s instanceof Hotel h) {
                partes.add(h.getNoches() + " noche(s) en " + h.getNombre());
            }
        }
        return recortar(String.join(" + ", partes) + ". Precio de referencia consultado el "
                + LocalDate.now(ZONA).format(FECHA) + ".");
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
