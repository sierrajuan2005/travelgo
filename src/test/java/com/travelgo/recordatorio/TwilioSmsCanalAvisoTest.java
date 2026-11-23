package com.travelgo.recordatorio;

import com.travelgo.model.RolUsuario;
import com.travelgo.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas del canal de SMS sin llamar a Twilio. */
class TwilioSmsCanalAvisoTest {

    @Test
    @DisplayName("Convierte el celular al formato internacional que pide Twilio")
    void formatoInternacional() {
        assertThat(TwilioSmsCanalAviso.formatoInternacional("300 123 4567", "+57")).isEqualTo("+573001234567");
        assertThat(TwilioSmsCanalAviso.formatoInternacional("+1 (415) 555-0100", "+57")).isEqualTo("+14155550100");
        assertThatThrownBy(() -> TwilioSmsCanalAviso.formatoInternacional("  ", "+57"))
                .isInstanceOf(CanalAvisoException.class);
    }

    @Test
    @DisplayName("Corta el texto a un solo SMS de 160 caracteres")
    void recortar() {
        assertThat(TwilioSmsCanalAviso.recortar("corto")).isEqualTo("corto");
        assertThat(TwilioSmsCanalAviso.recortar("x".repeat(200))).hasSize(160).endsWith("...");
    }

    @Test
    @DisplayName("Sin credenciales queda no configurado y la aplicación sigue funcionando")
    void sinCredenciales() {
        TwilioSmsCanalAviso canal = new TwilioSmsCanalAviso(RestClient.builder(), "", "", "", "", "+57");

        assertThat(canal.configurado()).isFalse();
        assertThatThrownBy(() -> canal.enviar(
                new Usuario("u", "Ana", "a@b.co", "x", RolUsuario.CLIENTE, "3001234567", false, true),
                new Aviso(TipoAviso.PAGO_CONFIRMADO, "a", "b", "c")))
                .isInstanceOf(CanalAvisoException.class);
    }

    @Test
    @DisplayName("Solo acepta avisos si el usuario activó el SMS y tiene celular")
    void aceptaAvisos() {
        TwilioSmsCanalAviso canal = new TwilioSmsCanalAviso(RestClient.builder(), "AC1", "t", "+1555", "", "+57");

        assertThat(canal.aceptaAvisos(
                new Usuario("u", "Ana", "a@b.co", "x", RolUsuario.CLIENTE, "3001234567", false, true))).isTrue();
        assertThat(canal.aceptaAvisos(
                new Usuario("u", "Ana", "a@b.co", "x", RolUsuario.CLIENTE, "3001234567", true, false))).isFalse();
    }
}
