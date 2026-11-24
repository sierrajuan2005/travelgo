package com.travelgo.expo;

import com.travelgo.model.PaqueteTuristico;
import com.travelgo.proveedor.destinos.Clima;
import com.travelgo.proveedor.destinos.Consulta;
import com.travelgo.proveedor.destinos.FotoDestino;
import com.travelgo.proveedor.destinos.InfoDestino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas del WhatsApp de la expo con un canal falso: nunca se escribe a un número real.
 * También prueba el texto del mensaje y la traducción de los estados de Twilio.
 */
class WhatsAppExpoServiceTest {

    private static final String BASE = "https://algo.trycloudflare.com";

    private LanzamientoServiceTest.DAOEnMemoria dao;
    private LanzamientoServiceTest.RelojManual reloj;
    private LanzamientoService lanzamientos;
    private CanalFalso canal;
    private WhatsAppExpoService service;

    @BeforeEach
    void preparar() {
        dao = new LanzamientoServiceTest.DAOEnMemoria();
        reloj = new LanzamientoServiceTest.RelojManual(Instant.parse("2026-12-01T15:00:00Z"));
        lanzamientos = new LanzamientoService(dao, new CatalogoDestinosSonados(), reloj);
        canal = new CanalFalso();

        DestinoExpoService destinoExpo = mock(DestinoExpoService.class);
        Clima clima = new Clima(29.4, "Despejado", "bi-sun", List.of());
        InfoDestino info = new InfoDestino(null, "Cartagena", "Cartagena", "Colombia", null,
                new Consulta<>(new FotoDestino("https://upload.wikimedia.org/cartagena.jpg", "t", "autor", null,
                        null, null), "Wikipedia", "", "", LocalDateTime.now()),
                new Consulta<>(clima, "Open-Meteo", "", "", LocalDateTime.now()), null, List.of());
        PaqueteTuristico colonial = new PaqueteTuristico("Cartagena Colonial", "", new BigDecimal("1200000"), 4);
        colonial.setId(7L);
        when(destinoExpo.info(any())).thenReturn(info);
        when(destinoExpo.paquetes(any())).thenReturn(List.of(colonial));
        when(destinoExpo.masBaratos(3)).thenReturn(List.of(colonial));

        service = new WhatsAppExpoService(canal, lanzamientos, dao, destinoExpo, reloj);
    }

    private long viajeConCelular() {
        long id = lanzamientos.lanzar("Ana", "cartagena", "cel-1").lanzamiento().id();
        lanzamientos.registrarContacto(id, "300 123 4567", true);
        return id;
    }

    @Test
    @DisplayName("Envía al celular del visitante su viaje con foto, clima, paquete y enlaces públicos")
    void enviaElViaje() {
        long id = viajeConCelular();

        EstadoWhatsApp estado = service.enviar(id, BASE + "/");

        assertThat(estado.estado()).isEqualTo(EstadoWhatsApp.ENVIANDO);
        assertThat(canal.enviados).hasSize(1);
        CanalFalso.Envio envio = canal.enviados.get(0);
        assertThat(envio.telefono()).isEqualTo("+573001234567");
        assertThat(envio.imagen()).isEqualTo("https://upload.wikimedia.org/cartagena.jpg");
        assertThat(envio.texto())
                .contains("Hola, Ana", "VUP → CTG", "29 °C", "Cartagena Colonial", "$ 1.200.000")
                .contains(BASE + "/expo/viaje/" + id + "/paquete/7")
                .contains(BASE + "/expo/viaje/" + id + "\n");
    }

    @Test
    @DisplayName("Sin celular guardado no se envía nada")
    void sinCelular() {
        long id = lanzamientos.lanzar("Ana", "cartagena", "cel-1").lanzamiento().id();

        assertThatThrownBy(() -> service.enviar(id, BASE)).isInstanceOf(IllegalArgumentException.class);
        assertThat(canal.enviados).isEmpty();
    }

    @Test
    @DisplayName("Sin credenciales de WhatsApp avisa y no envía")
    void sinConfigurar() {
        long id = viajeConCelular();
        canal.configurado = false;

        assertThatThrownBy(() -> service.enviar(id, BASE)).isInstanceOf(IllegalStateException.class);
        assertThat(canal.enviados).isEmpty();
    }

    @Test
    @DisplayName("No se envía dos veces: si ya va en camino o llegó, devuelve el estado")
    void noRepite() {
        long id = viajeConCelular();
        service.enviar(id, BASE);
        canal.estadoActual = new EstadoWhatsApp(EstadoWhatsApp.ENTREGADO, null);
        assertThat(service.consultar(id)).get().extracting(EstadoWhatsApp::estado).isEqualTo(EstadoWhatsApp.ENTREGADO);

        assertThat(service.enviar(id, BASE).estado()).isEqualTo(EstadoWhatsApp.ENTREGADO);
        assertThat(canal.enviados).hasSize(1);
    }

    @Test
    @DisplayName("Si no había activado WhatsApp se puede reintentar, pero no antes de unos segundos")
    void reintentaSiNoHabiaActivado() {
        long id = viajeConCelular();
        service.enviar(id, BASE);
        canal.estadoActual = EstadoWhatsApp.deTwilio("failed", 63015);
        assertThat(service.consultar(id)).get().extracting(EstadoWhatsApp::estado)
                .isEqualTo(EstadoWhatsApp.SIN_ACTIVAR);

        assertThatThrownBy(() -> service.enviar(id, BASE)).isInstanceOf(IllegalStateException.class);

        reloj.avanzar(Duration.ofSeconds(WhatsAppExpoService.SEGUNDOS_ENTRE_INTENTOS));
        service.enviar(id, BASE);
        assertThat(canal.enviados).hasSize(2);
    }

    @Test
    @DisplayName("El embudo cuenta como 'recibieron WhatsApp' solo los entregados o leídos")
    void embudoCuentaEntregados() {
        long id = viajeConCelular();
        service.enviar(id, BASE);
        assertThat(lanzamientos.embudo().recibieronWhatsApp()).isZero();

        canal.estadoActual = new EstadoWhatsApp(EstadoWhatsApp.LEIDO, null);
        service.consultar(id);
        assertThat(lanzamientos.embudo().recibieronWhatsApp()).isEqualTo(1);
    }

    @Test
    @DisplayName("Traduce los estados de Twilio a los de la página")
    void estadosDeTwilio() {
        assertThat(EstadoWhatsApp.deTwilio("queued", null).estado()).isEqualTo(EstadoWhatsApp.ENVIANDO);
        assertThat(EstadoWhatsApp.deTwilio("sent", null).estado()).isEqualTo(EstadoWhatsApp.ENVIANDO);
        assertThat(EstadoWhatsApp.deTwilio("delivered", null).estado()).isEqualTo(EstadoWhatsApp.ENTREGADO);
        assertThat(EstadoWhatsApp.deTwilio("read", null).estado()).isEqualTo(EstadoWhatsApp.LEIDO);
        assertThat(EstadoWhatsApp.deTwilio("failed", 63016).estado()).isEqualTo(EstadoWhatsApp.SIN_ACTIVAR);
        assertThat(EstadoWhatsApp.deTwilio("undelivered", 63024).estado()).isEqualTo(EstadoWhatsApp.FALLIDO);
    }

    @Test
    @DisplayName("Sin paquetes al destino, el mensaje cuenta cuántos sueñan con él y sugiere otros viajes")
    void mensajeSinPaquetes() {
        DestinoSonado tokio = new CatalogoDestinosSonados().buscar("tokio").orElseThrow();
        RedactorWhatsApp.PaqueteMensaje barato = new RedactorWhatsApp.PaqueteMensaje("Bogotá Cultural", 3,
                850_000, BASE + "/expo/viaje/1/paquete/2");

        String texto = RedactorWhatsApp.mensaje("Luis", tokio, null, null, List.of(), List.of(barato), 12,
                BASE + "/expo/viaje/1");

        assertThat(texto).contains("Todavía no vendemos Tokio", "ya son 12 personas", "Bogotá Cultural",
                "$ 850.000", "VUP → HND").doesNotContain("°C");
        assertThat(texto.length()).isLessThanOrEqualTo(RedactorWhatsApp.MAXIMO_CARACTERES);
    }

    @Test
    @DisplayName("Twilio: el enlace de activación abre WhatsApp con el código del sandbox ya escrito")
    void enlaceDeActivacionDeTwilio() {
        TwilioWhatsApp twilio = new TwilioWhatsApp(RestClient.builder(), "AC123", "token", "+14155238886",
                "join palabra-otra");
        assertThat(twilio.configurado()).isTrue();
        assertThat(twilio.enlaceActivacion()).isEqualTo("https://wa.me/14155238886?text=join%20palabra-otra");

        TwilioWhatsApp sinCodigo = new TwilioWhatsApp(RestClient.builder(), "AC123", "token", "+14155238886", "");
        assertThat(sinCodigo.configurado()).isFalse();
        assertThat(sinCodigo.enlaceActivacion()).isEmpty();
    }

    @Test
    @DisplayName("Twilio: agrega 'join' si falta y descarta el código de ejemplo de la documentación")
    void codigoDelSandbox() {
        assertThat(TwilioWhatsApp.codigoValido("happy-tiger")).isEqualTo("join happy-tiger");
        assertThat(TwilioWhatsApp.codigoValido(" join happy-tiger ")).isEqualTo("join happy-tiger");
        assertThat(TwilioWhatsApp.codigoValido("join palabra-palabra")).isEmpty();
        assertThat(TwilioWhatsApp.codigoValido(null)).isEmpty();
    }

    /** Canal que guarda lo que "envía" en una lista. */
    private static final class CanalFalso implements CanalWhatsApp {
        record Envio(String telefono, String texto, String imagen) {
        }

        final List<Envio> enviados = new ArrayList<>();
        boolean configurado = true;
        EstadoWhatsApp estadoActual = new EstadoWhatsApp(EstadoWhatsApp.ENVIANDO, null);

        @Override
        public boolean configurado() {
            return configurado;
        }

        @Override
        public String enlaceActivacion() {
            return "https://wa.me/14155238886?text=join%20prueba";
        }

        @Override
        public String enviar(String telefono, String texto, String urlImagen) {
            enviados.add(new Envio(telefono, texto, urlImagen));
            return "SM" + enviados.size();
        }

        @Override
        public EstadoWhatsApp estado(String idMensaje) {
            return estadoActual;
        }
    }
}
