package com.travelgo.recordatorio;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.model.Factura;
import com.travelgo.model.Pago;
import com.travelgo.model.Reserva;
import com.travelgo.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Revisa las reservas, decide a quién avisar y registra cada envío para no repetirlo.
 * <ul>
 *   <li>Qué aviso toca: {@link ReglasRecordatorio}.</li>
 *   <li>Qué dice el aviso: {@link RedactorAvisos}.</li>
 *   <li>Por dónde sale: la lista de {@link CanalAviso} que entrega Spring (correo, SMS...). El servicio no sabe
 *       qué canales existen: agregar uno no lo modifica (inversión de dependencias, abierto/cerrado).</li>
 *   <li>Si ya se envió: {@link EnvioRecordatorioDAO} (tabla {@code envio_recordatorio}).</li>
 * </ul>
 * Solo se avisa a quien aceptó los avisos de ese canal. Si un canal falla, se registra FALLIDO y se reintenta en la
 * próxima revisión; un error en una reserva no detiene la revisión de las demás.
 */
@Service
public class RecordatorioService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordatorioService.class);

    private final ReservaDAO reservaDAO;
    private final PagoDAO pagoDAO;
    private final FacturaDAO facturaDAO;
    private final EnvioRecordatorioDAO envioDAO;
    private final List<CanalAviso> canales;
    private final ReglasRecordatorio reglas;
    private final RedactorAvisos redactor;

    public RecordatorioService(ReservaDAO reservaDAO, PagoDAO pagoDAO, FacturaDAO facturaDAO,
                               EnvioRecordatorioDAO envioDAO, List<CanalAviso> canales,
                               ReglasRecordatorio reglas, RedactorAvisos redactor) {
        this.reservaDAO = reservaDAO;
        this.pagoDAO = pagoDAO;
        this.facturaDAO = facturaDAO;
        this.envioDAO = envioDAO;
        this.canales = List.copyOf(canales);
        this.reglas = reglas;
        this.redactor = redactor;
    }

    /**
     * Revisa todas las reservas y envía los avisos que correspondan en este momento.
     * Es {@code synchronized} para que la revisión programada y la del botón del administrador no se crucen.
     *
     * @param ahora momento de la revisión
     * @return cuántos avisos salieron y cuántos fallaron
     */
    public synchronized ResumenRevision revisar(LocalDateTime ahora) {
        List<CanalAviso> activos = canales.stream().filter(CanalAviso::configurado).toList();
        if (activos.isEmpty()) {
            return new ResumenRevision(0, 0, 0, false);
        }

        int revisadas = 0;
        int enviados = 0;
        int fallidos = 0;
        for (Reserva reserva : reservaDAO.listarTodas()) {
            revisadas++;
            try {
                Optional<Pago> pago = pagoDAO.buscarPorReservaId(reserva.getId());
                Optional<LocalDate> fechaPago = pago.map(Pago::getFechaPago);
                for (TipoAviso tipo : reglas.avisosQueCorresponden(reserva, fechaPago, ahora)) {
                    Aviso aviso = null;
                    for (CanalAviso canal : activos) {
                        Usuario usuario = reserva.getUsuario();
                        if (!canal.aceptaAvisos(usuario)
                                || envioDAO.yaEnviado(reserva.getId(), tipo, canal.canal())) {
                            continue;
                        }
                        if (aviso == null) {
                            Optional<Factura> factura = facturaDAO.buscarPorReservaId(reserva.getId());
                            aviso = redactor.redactar(tipo, reserva, factura, pago);
                        }
                        if (enviar(canal, usuario, reserva, aviso, ahora)) {
                            enviados++;
                        } else {
                            fallidos++;
                        }
                    }
                }
            } catch (RuntimeException e) {
                LOG.warn("No se pudo revisar la reserva {}: {}", reserva.getId(), e.getMessage());
            }
        }
        if (enviados > 0 || fallidos > 0) {
            LOG.info("Recordatorios: {} reservas revisadas, {} avisos enviados, {} fallidos.",
                    revisadas, enviados, fallidos);
        }
        return new ResumenRevision(revisadas, enviados, fallidos, true);
    }

    /** Últimos envíos registrados (para el panel del administrador). */
    public List<EnvioRecordatorio> ultimosEnvios(int limite) {
        return envioDAO.listarRecientes(limite);
    }

    /** Canales y si tienen credenciales (para el panel del administrador). */
    public List<EstadoCanal> estadoCanales() {
        return canales.stream().map(c -> new EstadoCanal(c.canal(), c.configurado())).toList();
    }

    private boolean enviar(CanalAviso canal, Usuario usuario, Reserva reserva, Aviso aviso,
                           LocalDateTime ahora) {
        EstadoEnvio estado;
        try {
            canal.enviar(usuario, aviso);
            estado = EstadoEnvio.ENVIADO;
        } catch (RuntimeException e) {
            LOG.warn("Aviso {} por {} de la reserva {} falló: {}", aviso.tipo(), canal.canal(),
                    reserva.getId(), e.getMessage());
            estado = EstadoEnvio.FALLIDO;
        }
        envioDAO.registrar(new EnvioRecordatorio(UUID.randomUUID().toString(), usuario.getId(),
                reserva.getId(), aviso.tipo(), canal.canal(), estado, ahora));
        return estado == EstadoEnvio.ENVIADO;
    }

    /**
     * Resultado de una revisión.
     *
     * @param hayCanales false si ningún canal tiene credenciales (no se revisó nada)
     */
    public record ResumenRevision(int revisadas, int enviados, int fallidos, boolean hayCanales) {
    }

    /** Un canal y si está configurado. */
    public record EstadoCanal(CanalEnvio canal, boolean configurado) {
    }
}
