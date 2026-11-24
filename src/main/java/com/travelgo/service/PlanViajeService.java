package com.travelgo.service;

import com.travelgo.dto.MapaRespuestaDTO;
import com.travelgo.model.Actividad;
import com.travelgo.model.Destino;
import com.travelgo.model.Gusto;
import com.travelgo.model.PaqueteTuristico;
import com.travelgo.model.Reserva;
import com.travelgo.model.Viajero;
import com.travelgo.proveedor.lugar.CategoriaMapa;
import com.travelgo.proveedor.lugar.LugarMapa;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Arma un plan día por día para cada viajero según sus gustos.
 * <ol>
 *   <li>Primero pone las actividades pagas que eligió (la fiesta de noche; lo demás de mañana o de tarde).</li>
 *   <li>Los huecos se llenan con lugares reales del destino (Geoapify, por medio de {@link MapaService})
 *       que sean de SUS gustos: a quien no eligió "Iglesias" nunca se le propone una iglesia.</li>
 *   <li>Si dos viajeros coinciden en el mismo plan a la misma hora, el plan dice "con Ana".</li>
 * </ol>
 * Si la fuente de lugares falla, el plan sale solo con las actividades y un aviso (no rompe la página).
 */
@Service
public class PlanViajeService {

    /** Días máximos que se dibujan (los paquetes largos repetirían lugares). */
    static final int MAXIMO_DIAS = 10;
    private static final int RADIO_METROS = 7000;
    private static final int LIMITE_POR_CATEGORIA = 15;

    /** Qué categorías del mapa corresponden a cada gusto. */
    static final Map<Gusto, Set<CategoriaMapa>> CATEGORIAS = new EnumMap<>(Gusto.class);

    static {
        CATEGORIAS.put(Gusto.FIESTA, EnumSet.of(CategoriaMapa.BAR));
        CATEGORIAS.put(Gusto.CULTURA, EnumSet.of(CategoriaMapa.MUSEO, CategoriaMapa.TEATRO));
        CATEGORIAS.put(Gusto.HISTORIA, EnumSet.of(CategoriaMapa.HISTORICO, CategoriaMapa.MONUMENTO,
                CategoriaMapa.SITIO_TURISTICO));
        CATEGORIAS.put(Gusto.RELIGION, EnumSet.of(CategoriaMapa.IGLESIA));
        CATEGORIAS.put(Gusto.NATURALEZA, EnumSet.of(CategoriaMapa.PARQUE, CategoriaMapa.PLAYA, CategoriaMapa.MIRADOR));
        CATEGORIAS.put(Gusto.GASTRONOMIA, EnumSet.of(CategoriaMapa.RESTAURANTE, CategoriaMapa.CAFE));
        CATEGORIAS.put(Gusto.COMPRAS, EnumSet.of(CategoriaMapa.CENTRO_COMERCIAL, CategoriaMapa.MERCADO));
        CATEGORIAS.put(Gusto.FAMILIA, EnumSet.of(CategoriaMapa.PARQUE_TEMATICO, CategoriaMapa.HELADERIA,
                CategoriaMapa.CINE));
    }

    private final MapaService mapaService;

    public PlanViajeService(MapaService mapaService) {
        this.mapaService = mapaService;
    }

    // ---------- Lo que recibe la vista ----------

    public enum Franja {
        MANANA("Mañana", "bi-sunrise"), TARDE("Tarde", "bi-sun"), NOCHE("Noche", "bi-moon-stars");

        private final String etiqueta;
        private final String icono;

        Franja(String etiqueta, String icono) {
            this.etiqueta = etiqueta;
            this.icono = icono;
        }

        public String getEtiqueta() { return etiqueta; }

        public String getIcono() { return icono; }
    }

    public enum TipoMomento { LLEGADA, REGRESO, ACTIVIDAD, LUGAR, LIBRE }

    /**
     * Un bloque del día.
     *
     * @param precio      precio si es una actividad paga; null si no
     * @param companeros  otros viajeros que hacen lo mismo a la misma hora
     */
    public record Momento(Franja franja, TipoMomento tipo, String titulo, String detalle, Gusto gusto,
                          Double precio, String enlace, List<String> companeros) {

        Momento conCompaneros(List<String> nombres) {
            return new Momento(franja, tipo, titulo, detalle, gusto, precio, enlace, nombres);
        }
    }

    public record DiaPlan(int numero, List<Momento> momentos) {
    }

    public record PlanViajero(Viajero viajero, List<DiaPlan> dias) {
    }

    public record PlanReserva(String ciudad, int dias, List<PlanViajero> viajeros, List<String> avisos) {
    }

    // ---------- Armado del plan ----------

    public PlanReserva planDe(Reserva reserva) {
        PaqueteTuristico paquete = reserva.getPaqueteTuristico();
        Destino destino = paquete == null || paquete.getDestinos() == null || paquete.getDestinos().isEmpty()
                ? null : paquete.getDestinos().get(0);
        int dias = paquete == null ? 3 : Math.min(MAXIMO_DIAS, Math.max(1, paquete.getDuracionDias()));
        List<Viajero> viajeros = reserva.getViajeros();

        List<String> avisos = new ArrayList<>();
        Map<Gusto, List<LugarMapa>> lugares = lugaresPorGusto(destino, gustosDe(viajeros), avisos);
        return armar(destino == null ? "tu destino" : destino.getCiudad(), dias, viajeros, lugares, avisos);
    }

    /** Arma el plan con los lugares ya cargados. Separado para poder probarlo sin internet. */
    static PlanReserva armar(String ciudad, int dias, List<Viajero> viajeros,
                             Map<Gusto, List<LugarMapa>> lugares, List<String> avisos) {
        List<PlanViajero> planes = new ArrayList<>();
        for (Viajero viajero : viajeros) {
            planes.add(new PlanViajero(viajero, diasDe(viajero, dias, ciudad, lugares)));
        }
        return new PlanReserva(ciudad, dias, marcarCompaneros(planes), avisos);
    }

    private static List<DiaPlan> diasDe(Viajero viajero, int dias, String ciudad, Map<Gusto, List<LugarMapa>> lugares) {
        // casillas[dia][franja]; null = libre todavía
        Momento[][] casillas = new Momento[dias][Franja.values().length];
        casillas[0][Franja.MANANA.ordinal()] = new Momento(Franja.MANANA, TipoMomento.LLEGADA,
                "Llegada a " + ciudad, "Vuelo, traslado y check-in en el hotel.", null, null, null, List.of());
        if (dias > 1) {
            casillas[dias - 1][Franja.TARDE.ordinal()] = new Momento(Franja.TARDE, TipoMomento.REGRESO,
                    "Regreso a casa", "Check-out y traslado al aeropuerto.", null, null, null, List.of());
            casillas[dias - 1][Franja.NOCHE.ordinal()] = new Momento(Franja.NOCHE, TipoMomento.REGRESO,
                    "De vuelta en casa", "Fin del viaje.", null, null, null, List.of());
        }

        // 1) Actividades pagas
        for (Actividad actividad : viajero.getActividades()) {
            Momento momento = new Momento(null, TipoMomento.ACTIVIDAD, actividad.nombre(), actividad.descripcion(),
                    actividad.gusto(), actividad.precio(), null, List.of());
            List<Franja> preferidas = actividad.gusto().isDeNoche()
                    ? List.of(Franja.NOCHE, Franja.TARDE, Franja.MANANA)
                    : List.of(Franja.MANANA, Franja.TARDE, Franja.NOCHE);
            ubicar(casillas, momento, preferidas);
        }

        // 2) Lugares reales de sus gustos en los huecos (sin repetir lugares)
        List<Gusto> deDia = viajero.getGustos().stream().filter(g -> !g.isDeNoche()).toList();
        List<Gusto> deNoche = viajero.getGustos().stream()
                .filter(g -> g.isDeNoche() || g == Gusto.GASTRONOMIA).toList();
        Set<String> usados = new HashSet<>();
        int turnoDia = 0;
        int turnoNoche = 0;
        for (int d = 0; d < dias; d++) {
            for (Franja franja : Franja.values()) {
                if (casillas[d][franja.ordinal()] != null) {
                    continue;
                }
                List<Gusto> opciones = franja == Franja.NOCHE ? deNoche : deDia;
                int turno = franja == Franja.NOCHE ? turnoNoche++ : turnoDia++;
                Momento lugar = siguienteLugar(opciones, turno, lugares, usados, franja);
                casillas[d][franja.ordinal()] = lugar != null ? lugar : libre(franja, viajero.getGustos().isEmpty());
            }
        }

        List<DiaPlan> resultado = new ArrayList<>();
        for (int d = 0; d < dias; d++) {
            List<Momento> momentos = new ArrayList<>();
            for (Franja franja : Franja.values()) {
                Momento m = casillas[d][franja.ordinal()];
                momentos.add(new Momento(franja, m.tipo(), m.titulo(), m.detalle(), m.gusto(), m.precio(),
                        m.enlace(), m.companeros()));
            }
            resultado.add(new DiaPlan(d + 1, momentos));
        }
        return resultado;
    }

    /** Pone la actividad en la primera casilla libre, probando primero las franjas preferidas. */
    private static void ubicar(Momento[][] casillas, Momento momento, List<Franja> preferidas) {
        for (Franja franja : preferidas) {
            for (Momento[] dia : casillas) {
                if (dia[franja.ordinal()] == null) {
                    dia[franja.ordinal()] = momento;
                    return;
                }
            }
        }
        // Más actividades que casillas: no se dibuja, pero sigue cobrándose y aparece en el resumen
    }

    /** Toma un lugar no usado, rotando entre los gustos para que el plan sea variado. */
    private static Momento siguienteLugar(List<Gusto> opciones, int turno, Map<Gusto, List<LugarMapa>> lugares,
                                          Set<String> usados, Franja franja) {
        for (int i = 0; i < opciones.size(); i++) {
            Gusto gusto = opciones.get((turno + i) % opciones.size());
            for (LugarMapa lugar : lugares.getOrDefault(gusto, List.of())) {
                if (usados.add(lugar.fuente() + "|" + lugar.id())) {
                    String prefijo = franja == Franja.NOCHE && gusto == Gusto.GASTRONOMIA ? "Cena en " : "";
                    String detalle = lugar.categoria().etiqueta()
                            + (lugar.direccion() == null || lugar.direccion().isBlank() ? "" : " · " + lugar.direccion());
                    return new Momento(franja, TipoMomento.LUGAR, prefijo + lugar.nombre(), detalle, gusto, null,
                            lugar.enlace(), List.of());
                }
            }
        }
        return null;
    }

    private static Momento libre(Franja franja, boolean sinGustos) {
        String detalle = sinGustos ? "Elige tus gustos y te proponemos qué hacer."
                : franja == Franja.NOCHE ? "Descansa o explora por tu cuenta." : "Tiempo libre para caminar la ciudad.";
        return new Momento(franja, TipoMomento.LIBRE, "Libre", detalle, null, null, null, List.of());
    }

    /** Si dos personas hacen lo mismo el mismo día y a la misma hora, cada una ve "con ...". */
    private static List<PlanViajero> marcarCompaneros(List<PlanViajero> planes) {
        Map<String, List<String>> quienes = new LinkedHashMap<>();
        for (PlanViajero plan : planes) {
            recorrer(plan, (clave, m) -> quienes.computeIfAbsent(clave, k -> new ArrayList<>())
                    .add(plan.viajero().getNombre()));
        }
        List<PlanViajero> resultado = new ArrayList<>();
        for (PlanViajero plan : planes) {
            List<DiaPlan> dias = new ArrayList<>();
            for (DiaPlan dia : plan.dias()) {
                List<Momento> momentos = new ArrayList<>();
                for (Momento m : dia.momentos()) {
                    String clave = clave(dia.numero(), m);
                    List<String> otros = clave == null ? List.of() : quienes.getOrDefault(clave, List.of()).stream()
                            .filter(n -> !n.equals(plan.viajero().getNombre())).toList();
                    momentos.add(m.conCompaneros(otros));
                }
                dias.add(new DiaPlan(dia.numero(), momentos));
            }
            resultado.add(new PlanViajero(plan.viajero(), dias));
        }
        return resultado;
    }

    private static void recorrer(PlanViajero plan, java.util.function.BiConsumer<String, Momento> accion) {
        for (DiaPlan dia : plan.dias()) {
            for (Momento m : dia.momentos()) {
                String clave = clave(dia.numero(), m);
                if (clave != null) {
                    accion.accept(clave, m);
                }
            }
        }
    }

    /** Solo se comparten actividades y lugares (la llegada, el regreso y lo libre no cuentan). */
    private static String clave(int dia, Momento m) {
        if (m.tipo() != TipoMomento.ACTIVIDAD && m.tipo() != TipoMomento.LUGAR) {
            return null;
        }
        return dia + "|" + m.franja() + "|" + m.titulo();
    }

    // ---------- Lugares reales ----------

    private static Set<Gusto> gustosDe(List<Viajero> viajeros) {
        Set<Gusto> gustos = EnumSet.noneOf(Gusto.class);
        viajeros.forEach(v -> gustos.addAll(v.getGustos()));
        return gustos;
    }

    /**
     * Una sola consulta para todo el grupo (las categorías de todos los gustos juntas) y luego se reparte
     * por gusto. Un lugar solo entra en un gusto si su categoría REAL es de ese gusto: MapaService puede
     * reclasificar un "sitio turístico" como iglesia, y ese no se le debe proponer a quien busca historia.
     */
    private Map<Gusto, List<LugarMapa>> lugaresPorGusto(Destino destino, Set<Gusto> gustos, List<String> avisos) {
        Map<Gusto, List<LugarMapa>> porGusto = new EnumMap<>(Gusto.class);
        if (destino == null || destino.getId() == null || gustos.isEmpty()) {
            return porGusto;
        }
        Set<CategoriaMapa> categorias = EnumSet.noneOf(CategoriaMapa.class);
        gustos.forEach(g -> categorias.addAll(CATEGORIAS.get(g)));
        try {
            MapaRespuestaDTO respuesta = mapaService.lugares("destino-" + destino.getId(), categorias,
                    RADIO_METROS, LIMITE_POR_CATEGORIA);
            avisos.addAll(respuesta.avisos());
            return repartir(respuesta.lugares(), gustos);
        } catch (RuntimeException e) {
            avisos.add("No pudimos cargar lugares reales de " + destino.getCiudad()
                    + " ahora mismo; el plan muestra tus actividades y tiempo libre.");
            return porGusto;
        }
    }

    static Map<Gusto, List<LugarMapa>> repartir(List<LugarMapa> lugares, Set<Gusto> gustos) {
        Map<Gusto, List<LugarMapa>> porGusto = new EnumMap<>(Gusto.class);
        for (Gusto gusto : gustos) {
            Set<CategoriaMapa> suyas = CATEGORIAS.get(gusto);
            porGusto.put(gusto, lugares.stream()
                    .filter(l -> suyas.contains(l.categoria()))
                    .filter(l -> l.nombre() != null && !l.nombre().isBlank())
                    .toList());
        }
        return porGusto;
    }
}
