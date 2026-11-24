package com.travelgo.expo;

import com.travelgo.exception.RecursoNoEncontradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reglas de "Lanza tu viaje" (ExpoSoftware): el visitante escribe su nombre en el celular, elige un destino
 * y su avión sale de Valledupar en la pantalla grande del stand.
 *
 * - El nombre se limpia y se valida con {@link NombreViajero}.
 * - Cada celular (sesión) espera unos segundos entre lanzamientos, para que nadie llene la pantalla solo.
 * - La pantalla pregunta cada pocos segundos por los lanzamientos nuevos ({@link #pantalla(long)}).
 */
@Service
public class LanzamientoService {

    static final Duration ESPERA = Duration.ofSeconds(15);
    static final int MAXIMO_EN_PANTALLA = 500;
    static final int MAXIMO_POR_CONSULTA = 50;

    private final LanzamientoDAO dao;
    private final CatalogoDestinosSonados catalogo;
    private final Clock reloj;
    private final Map<String, Instant> ultimoPorSesion = new ConcurrentHashMap<>();

    @Autowired
    public LanzamientoService(LanzamientoDAO dao, CatalogoDestinosSonados catalogo) {
        this(dao, catalogo, Clock.systemDefaultZone());
    }

    LanzamientoService(LanzamientoDAO dao, CatalogoDestinosSonados catalogo, Clock reloj) {
        this.dao = dao;
        this.catalogo = catalogo;
        this.reloj = reloj;
    }

    public List<DestinoSonado> destinos() {
        return catalogo.todos();
    }

    /**
     * Registra el viaje de un visitante.
     *
     * @param sesion identifica al celular (id de la sesión HTTP)
     * @throws IllegalArgumentException si el nombre no es válido
     * @throws RecursoNoEncontradoException si el destino no existe
     * @throws IllegalStateException si ese celular lanzó otro viaje hace muy poco
     */
    public ViajeLanzado lanzar(String nombre, String destinoId, String sesion) {
        String limpio = NombreViajero.limpiar(nombre);
        DestinoSonado destino = catalogo.buscar(destinoId).orElseThrow(
                () -> new RecursoNoEncontradoException("Ese destino no está en la lista: " + destinoId));

        Instant ahora = reloj.instant();
        Instant anterior = ultimoPorSesion.get(sesion);
        if (anterior != null && anterior.plus(ESPERA).isAfter(ahora)) {
            long faltan = (Duration.between(ahora, anterior.plus(ESPERA)).toMillis() + 999) / 1000; // redondea hacia arriba
            throw new IllegalStateException("Tu avión acaba de despegar. Espera " + faltan
                    + " segundos para lanzar otro viaje.");
        }
        ultimoPorSesion.put(sesion, ahora);
        olvidarSesionesViejas(ahora);

        LocalDateTime fecha = LocalDateTime.now(reloj);
        long id = dao.guardar(limpio, destino.id(), fecha);
        return new ViajeLanzado(new Lanzamiento(id, limpio, destino.id(), fecha, false), destino);
    }

    /**
     * Lo que pinta la pantalla del stand.
     *
     * @param desdeId 0 la primera vez (trae los últimos {@value #MAXIMO_EN_PANTALLA}); después, el último id
     *                que ya tiene la pantalla (trae solo los nuevos)
     */
    public Pantalla pantalla(long desdeId) {
        List<Lanzamiento> filas = desdeId <= 0
                ? dao.recientesVisibles(MAXIMO_EN_PANTALLA)
                : dao.visiblesDesde(desdeId, MAXIMO_POR_CONSULTA);
        List<ViajeLanzado> viajes = filas.stream().map(this::conDestino).flatMap(Optional::stream).toList();
        return new Pantalla(dao.contarVisibles(), viajes);
    }

    /**
     * Un viaje con su destino, para la página que ve el visitante al aterrizar.
     *
     * @throws RecursoNoEncontradoException si no existe, está oculto o su destino ya no está en la lista
     */
    public ViajeLanzado viaje(long id) {
        return dao.buscar(id).filter(l -> !l.oculto()).flatMap(this::conDestino)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el viaje " + id));
    }

    /** Cuántas personas soñaron con ese destino en la expo (prueba social en la página del destino). */
    public long sonadoresDe(String destinoId) {
        return dao.contarVisiblesDe(destinoId);
    }

    /**
     * Guarda el celular del visitante para su postal y ofertas.
     *
     * @param autoriza el visitante marcó la autorización de tratamiento de datos (Ley 1581 de 2012)
     * @return el número normalizado
     * @throws IllegalArgumentException si no autorizó o el número no es válido
     */
    public String registrarContacto(long id, String telefono, boolean autoriza) {
        if (!autoriza) {
            throw new IllegalArgumentException("Para guardar tu celular necesitamos tu autorización.");
        }
        String normalizado = TelefonoContacto.normalizar(telefono);
        if (!dao.guardarContacto(id, normalizado, LocalDateTime.now(reloj))) {
            throw new RecursoNoEncontradoException("No existe el viaje " + id);
        }
        return normalizado;
    }

    /** Borra el celular (el visitante o el administrador lo pidieron). */
    public void borrarContacto(long id) {
        if (!dao.guardarContacto(id, null, null)) {
            throw new RecursoNoEncontradoException("No existe el viaje " + id);
        }
    }

    public void marcarVioDestino(long id) {
        dao.marcarVioDestino(id);
    }

    public void marcarAbrioPaquete(long id) {
        dao.marcarAbrioPaquete(id);
    }

    public LanzamientoDAO.Embudo embudo() {
        return dao.embudo();
    }

    /** Viajes visibles en toda la expo. */
    public long total() {
        return dao.contarVisibles();
    }

    /** Panel del administrador: los últimos lanzamientos, también los ocultos. */
    public List<ViajeLanzado> ultimosParaModerar(int limite) {
        return dao.ultimos(limite).stream().map(this::conDestino).flatMap(Optional::stream).toList();
    }

    public void cambiarOculto(long id, boolean oculto) {
        if (!dao.cambiarOculto(id, oculto)) {
            throw new RecursoNoEncontradoException("No existe el viaje lanzado " + id);
        }
    }

    // Si un destino se quita de la lista, sus lanzamientos viejos simplemente no se pintan
    private Optional<ViajeLanzado> conDestino(Lanzamiento l) {
        return catalogo.buscar(l.destinoId()).map(d -> new ViajeLanzado(l, d));
    }

    private void olvidarSesionesViejas(Instant ahora) {
        if (ultimoPorSesion.size() > 1000) {
            ultimoPorSesion.values().removeIf(i -> i.plus(ESPERA).isBefore(ahora));
        }
    }

    /** Un lanzamiento con los datos de su destino. */
    public record ViajeLanzado(Lanzamiento lanzamiento, DestinoSonado destino) {
    }

    /** total = viajes visibles en toda la expo (la lista puede traer menos). */
    public record Pantalla(long total, List<ViajeLanzado> viajes) {
    }
}
