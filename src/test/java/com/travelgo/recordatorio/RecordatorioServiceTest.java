package com.travelgo.recordatorio;

import com.travelgo.dao.FacturaDAO;
import com.travelgo.dao.PagoDAO;
import com.travelgo.dao.ReservaDAO;
import com.travelgo.model.EstadoReserva;
import com.travelgo.model.Reserva;
import com.travelgo.model.RolUsuario;
import com.travelgo.model.ServicioReservable;
import com.travelgo.model.Usuario;
import com.travelgo.model.Vuelo;
import com.travelgo.web.FormatoWeb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas del servicio con canales y DAO falsos: nunca se envía un correo ni un SMS de verdad.
 */
class RecordatorioServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 1, 10, 0);

    private ReservaDAO reservaDAO;
    private PagoDAO pagoDAO;
    private FacturaDAO facturaDAO;
    private EnvioDAOEnMemoria envioDAO;
    private CanalFalso correo;
    private CanalFalso sms;

    @BeforeEach
    void preparar() {
        reservaDAO = mock(ReservaDAO.class);
        pagoDAO = mock(PagoDAO.class);
        facturaDAO = mock(FacturaDAO.class);
        when(pagoDAO.buscarPorReservaId(anyString())).thenReturn(Optional.empty());
        when(facturaDAO.buscarPorReservaId(anyString())).thenReturn(Optional.empty());
        envioDAO = new EnvioDAOEnMemoria();
        correo = new CanalFalso(CanalEnvio.CORREO, true);
        sms = new CanalFalso(CanalEnvio.SMS, true);
    }

    private RecordatorioService servicio(CanalAviso... canales) {
        return new RecordatorioService(reservaDAO, pagoDAO, facturaDAO, envioDAO, List.of(canales),
                new ReglasRecordatorio(6, 2), new RedactorAvisos(new FormatoWeb()));
    }

    private Usuario usuario(boolean aceptaCorreo, boolean aceptaSms) {
        return new Usuario("u-1", "Ana", "ana@ejemplo.com", "x", RolUsuario.CLIENTE,
                aceptaSms ? "3001234567" : null, aceptaCorreo, aceptaSms);
    }

    private Reserva reservaQueSaleEn(Usuario usuario, int dias) {
        ServicioReservable vuelo = new Vuelo("v-1", 500_000, true, "Avianca", "Bogotá", "Cartagena",
                AHORA.plusDays(dias), AHORA.plusDays(dias).plusHours(1));
        return new Reserva("r-1", usuario, null, List.of(vuelo), AHORA.toLocalDate(),
                EstadoReserva.CONFIRMADA, 500_000);
    }

    @Test
    @DisplayName("Envía el aviso por cada canal aceptado y lo registra")
    void enviaPorLosCanalesAceptados() {
        when(reservaDAO.listarTodas()).thenReturn(List.of(reservaQueSaleEn(usuario(true, true), 5)));

        RecordatorioService.ResumenRevision resumen = servicio(correo, sms).revisar(AHORA);

        assertThat(resumen.enviados()).isEqualTo(2);
        assertThat(correo.enviados).extracting(Aviso::tipo).containsExactly(TipoAviso.VIAJE_7_DIAS);
        assertThat(sms.enviados).hasSize(1);
        assertThat(envioDAO.envios).allMatch(e -> e.estado() == EstadoEnvio.ENVIADO);
    }

    @Test
    @DisplayName("No repite un aviso que ya se envió")
    void noRepite() {
        when(reservaDAO.listarTodas()).thenReturn(List.of(reservaQueSaleEn(usuario(true, false), 5)));
        RecordatorioService servicio = servicio(correo);

        servicio.revisar(AHORA);
        servicio.revisar(AHORA.plusMinutes(5));

        assertThat(correo.enviados).hasSize(1);
    }

    @Test
    @DisplayName("Solo avisa por los canales que el usuario aceptó")
    void respetaLasPreferencias() {
        when(reservaDAO.listarTodas()).thenReturn(List.of(reservaQueSaleEn(usuario(false, false), 5)));

        servicio(correo, sms).revisar(AHORA);

        assertThat(correo.enviados).isEmpty();
        assertThat(sms.enviados).isEmpty();
        assertThat(envioDAO.envios).isEmpty();
    }

    @Test
    @DisplayName("Un canal sin credenciales no se usa")
    void canalSinCredenciales() {
        when(reservaDAO.listarTodas()).thenReturn(List.of(reservaQueSaleEn(usuario(true, true), 5)));
        CanalFalso smsSinLlave = new CanalFalso(CanalEnvio.SMS, false);

        servicio(correo, smsSinLlave).revisar(AHORA);

        assertThat(correo.enviados).hasSize(1);
        assertThat(smsSinLlave.enviados).isEmpty();
    }

    @Test
    @DisplayName("Sin ningún canal configurado no revisa nada")
    void sinCanales() {
        RecordatorioService.ResumenRevision resumen =
                servicio(new CanalFalso(CanalEnvio.CORREO, false)).revisar(AHORA);

        assertThat(resumen.hayCanales()).isFalse();
    }

    @Test
    @DisplayName("Si el canal falla se registra FALLIDO y se reintenta en la próxima revisión")
    void fallaYReintenta() {
        when(reservaDAO.listarTodas()).thenReturn(List.of(reservaQueSaleEn(usuario(true, false), 5)));
        correo.falla = true;
        RecordatorioService servicio = servicio(correo);

        RecordatorioService.ResumenRevision primera = servicio.revisar(AHORA);
        assertThat(primera.fallidos()).isEqualTo(1);
        assertThat(envioDAO.envios).singleElement().extracting(EnvioRecordatorio::estado).isEqualTo(EstadoEnvio.FALLIDO);

        correo.falla = false;
        servicio.revisar(AHORA.plusMinutes(5));

        assertThat(correo.enviados).hasSize(1);
        assertThat(envioDAO.envios).singleElement().extracting(EnvioRecordatorio::estado).isEqualTo(EstadoEnvio.ENVIADO);
    }

    // ---------- Dobles de prueba ----------

    /** Canal que guarda los avisos en una lista en vez de enviarlos. */
    private static final class CanalFalso implements CanalAviso {
        private final CanalEnvio canal;
        private final boolean configurado;
        private final List<Aviso> enviados = new ArrayList<>();
        private boolean falla;

        CanalFalso(CanalEnvio canal, boolean configurado) {
            this.canal = canal;
            this.configurado = configurado;
        }

        @Override
        public CanalEnvio canal() {
            return canal;
        }

        @Override
        public boolean configurado() {
            return configurado;
        }

        @Override
        public boolean aceptaAvisos(Usuario usuario) {
            return canal == CanalEnvio.CORREO ? usuario.isNotificacionesCorreo() : usuario.isNotificacionesSms();
        }

        @Override
        public void enviar(Usuario usuario, Aviso aviso) {
            if (falla) {
                throw new CanalAvisoException("servidor caído");
            }
            enviados.add(aviso);
        }
    }

    /** Misma lógica que la tabla: una fila por reserva, tipo y canal (UNIQUE) que se actualiza. */
    private static final class EnvioDAOEnMemoria implements EnvioRecordatorioDAO {
        private final List<EnvioRecordatorio> envios = new ArrayList<>();

        @Override
        public boolean yaEnviado(String reservaId, TipoAviso tipo, CanalEnvio canal) {
            return envios.stream().anyMatch(e -> e.reservaId().equals(reservaId) && e.tipo() == tipo
                    && e.canal() == canal && e.estado() == EstadoEnvio.ENVIADO);
        }

        @Override
        public void registrar(EnvioRecordatorio envio) {
            envios.removeIf(e -> e.reservaId().equals(envio.reservaId()) && e.tipo() == envio.tipo()
                    && e.canal() == envio.canal());
            envios.add(envio);
        }

        @Override
        public List<EnvioRecordatorio> listarRecientes(int limite) {
            return envios.stream().limit(limite).toList();
        }
    }
}
