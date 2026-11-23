package com.travelgo.expo;

import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.destinos.InfoDestino;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Envía al visitante su viaje por WhatsApp: foto real del destino, clima de hoy, paquetes de TravelGo con
 * precio y enlace, y el enlace a la página de su destino.
 *
 * Reglas:
 * - Solo si dejó su celular con autorización (LanzamientoService.registrarContacto).
 * - No se repite: si ya va en camino o llegó, se devuelve el estado. Solo se reintenta si falló o si la
 *   persona no había activado WhatsApp, y con al menos {@value #SEGUNDOS_ENTRE_INTENTOS} s entre intentos.
 * - El estado real (entregado, leído...) se le pregunta al proveedor con {@link #consultar(long)}.
 */
@Service
public class WhatsAppExpoService {

    static final int SEGUNDOS_ENTRE_INTENTOS = 8;

    private final CanalWhatsApp canal;
    private final LanzamientoService lanzamientos;
    private final LanzamientoDAO dao;
    private final DestinoExpoService destinoExpo;
    private final Clock reloj;

    @Autowired
    public WhatsAppExpoService(CanalWhatsApp canal, LanzamientoService lanzamientos, LanzamientoDAO dao,
                               DestinoExpoService destinoExpo) {
        this(canal, lanzamientos, dao, destinoExpo, Clock.systemDefaultZone());
    }

    WhatsAppExpoService(CanalWhatsApp canal, LanzamientoService lanzamientos, LanzamientoDAO dao,
                        DestinoExpoService destinoExpo, Clock reloj) {
        this.canal = canal;
        this.lanzamientos = lanzamientos;
        this.dao = dao;
        this.destinoExpo = destinoExpo;
        this.reloj = reloj;
    }

    public boolean disponible() {
        return canal.configurado();
    }

    /** Enlace que abre WhatsApp con el mensaje de activación ya escrito. */
    public String enlaceActivacion() {
        return canal.enlaceActivacion();
    }

    /**
     * Envía el viaje por WhatsApp.
     *
     * @param urlBase dirección pública de la app (la del túnel), para los enlaces del mensaje
     * @throws IllegalStateException    si WhatsApp no está configurado o se intentó hace muy poco
     * @throws IllegalArgumentException si el visitante no dejó su celular
     */
    public EstadoWhatsApp enviar(long id, String urlBase) {
        if (!canal.configurado()) {
            throw new IllegalStateException("WhatsApp no está activo en el stand en este momento.");
        }
        LanzamientoService.ViajeLanzado viaje = lanzamientos.viaje(id);
        String telefono = viaje.lanzamiento().telefono();
        if (telefono == null) {
            throw new IllegalArgumentException("Primero deja tu celular y autoriza los mensajes.");
        }

        Optional<LanzamientoDAO.EnvioWhatsApp> anterior = dao.envioWhatsApp(id);
        if (anterior.isPresent()) {
            EstadoWhatsApp estado = new EstadoWhatsApp(anterior.get().estado(), null);
            if (!estado.sePuedeReintentar()) {
                return estado; // ya va en camino o llegó: no se manda dos veces
            }
            LocalDateTime ultimo = anterior.get().fecha();
            if (ultimo != null && ultimo.plusSeconds(SEGUNDOS_ENTRE_INTENTOS).isAfter(LocalDateTime.now(reloj))) {
                throw new IllegalStateException("Espera unos segundos e intenta otra vez.");
            }
        }

        String base = sinBarraFinal(urlBase);
        DestinoSonado destino = viaje.destino();
        InfoDestino info = destinoExpo.info(destino);
        Double temperatura = info.clima() == null ? null : info.clima().datos().temperaturaActual();
        String clima = info.clima() == null ? null : info.clima().datos().descripcion();
        String foto = info.foto() == null ? null : info.foto().datos().url();

        String texto = RedactorWhatsApp.mensaje(viaje.lanzamiento().nombre(), destino, temperatura, clima,
                paquetesParaMensaje(destinoExpo.paquetes(destino), base, id),
                paquetesParaMensaje(destinoExpo.masBaratos(RedactorWhatsApp.MAXIMO_PAQUETES), base, id),
                lanzamientos.sonadoresDe(destino.id()),
                base + "/expo/viaje/" + id);

        String idMensaje = canal.enviar(telefono, texto, foto);
        dao.guardarWhatsApp(id, idMensaje, EstadoWhatsApp.ENVIANDO, LocalDateTime.now(reloj));
        return new EstadoWhatsApp(EstadoWhatsApp.ENVIANDO, null);
    }

    /** Último estado guardado (sin preguntarle al proveedor), para pintar la página rápido. */
    public Optional<String> estadoGuardado(long id) {
        return dao.envioWhatsApp(id).map(LanzamientoDAO.EnvioWhatsApp::estado);
    }

    /**
     * Estado del último WhatsApp de ese viaje (le pregunta al proveedor si todavía va en camino).
     *
     * @return vacío si nunca se envió
     */
    public Optional<EstadoWhatsApp> consultar(long id) {
        Optional<LanzamientoDAO.EnvioWhatsApp> envio = dao.envioWhatsApp(id);
        if (envio.isEmpty()) {
            return Optional.empty();
        }
        EstadoWhatsApp guardado = new EstadoWhatsApp(envio.get().estado(), null);
        if (guardado.terminado() && !EstadoWhatsApp.ENTREGADO.equals(guardado.estado())) {
            return Optional.of(guardado); // LEIDO, SIN_ACTIVAR y FALLIDO ya no cambian
        }
        EstadoWhatsApp actual = canal.estado(envio.get().idMensaje());
        if (!actual.estado().equals(guardado.estado())) {
            dao.actualizarEstadoWhatsApp(id, actual.estado());
        }
        return Optional.of(actual);
    }

    // Los enlaces pasan por /expo/viaje/{id}/paquete/{p}: así el clic cuenta en el embudo del stand
    private static List<RedactorWhatsApp.PaqueteMensaje> paquetesParaMensaje(List<PaqueteTuristico> paquetes,
                                                                            String base, long viajeId) {
        return paquetes.stream().map(p -> new RedactorWhatsApp.PaqueteMensaje(p.getNombre(), p.getDuracionDias(),
                        p.getPrecio() == null ? 0 : p.getPrecio().doubleValue(),
                        base + "/expo/viaje/" + viajeId + "/paquete/" + p.getId()))
                .toList();
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
