package com.travelgo.expo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.travelgo.proveedor.destinos.ProveedoresDestinoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * WhatsApp con la API de mensajes de Twilio (adaptador). Para la ExpoSoftware se usa el <b>sandbox</b> de
 * WhatsApp de Twilio: es gratis y funciona con la cuenta de prueba, pero cada persona primero envía
 * "join palabra-palabra" al número del sandbox (el enlace de {@link #enlaceActivacion()} lo deja escrito).
 *
 * Usa las mismas credenciales del SMS ({@code TWILIO_ACCOUNT_SID}, {@code TWILIO_AUTH_TOKEN}) y además:
 * - {@code TRAVELGO_WHATSAPP_NUMERO}: número de WhatsApp de Twilio (sandbox: +14155238886).
 * - {@code TRAVELGO_WHATSAPP_CODIGO}: código del sandbox tal como sale en la consola ("join palabra-palabra").
 */
@Component
public class TwilioWhatsApp implements CanalWhatsApp {

    private static final Logger LOG = LoggerFactory.getLogger(TwilioWhatsApp.class);
    private static final String URL_BASE = "https://api.twilio.com/2010-04-01";

    private final RestClient http;
    private final String accountSid;
    private final String authToken;
    private final String numero;
    private final String codigoSandbox;

    public TwilioWhatsApp(@Qualifier(ProveedoresDestinoConfig.HTTP_PROVEEDORES) RestClient.Builder http,
                          @Value("${travelgo.recordatorios.sms.twilio.account-sid:}") String accountSid,
                          @Value("${travelgo.recordatorios.sms.twilio.auth-token:}") String authToken,
                          @Value("${travelgo.expo.whatsapp.numero:${TRAVELGO_WHATSAPP_NUMERO:+14155238886}}") String numero,
                          @Value("${travelgo.expo.whatsapp.codigo:${TRAVELGO_WHATSAPP_CODIGO:}}") String codigoSandbox) {
        this.http = http.clone().baseUrl(URL_BASE).build();
        this.accountSid = limpiar(accountSid);
        this.authToken = limpiar(authToken);
        this.numero = limpiar(numero);
        this.codigoSandbox = limpiar(codigoSandbox);
        if (!configurado()) {
            LOG.info("WhatsApp de la expo desactivado: faltan TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN o "
                    + "TRAVELGO_WHATSAPP_CODIGO (ver docs/expo_lanza_tu_viaje.md).");
        }
    }

    @Override
    public boolean configurado() {
        return !accountSid.isEmpty() && !authToken.isEmpty() && !numero.isEmpty() && !codigoSandbox.isEmpty();
    }

    @Override
    public String enlaceActivacion() {
        if (codigoSandbox.isEmpty()) {
            return "";
        }
        String digitos = numero.replaceAll("[^0-9]", "");
        return "https://wa.me/" + digitos + "?text=" + URLEncoder.encode(codigoSandbox, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    @Override
    public String enviar(String telefono, String texto, String urlImagen) {
        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("To", "whatsapp:" + telefono);
        formulario.add("From", "whatsapp:" + numero);
        formulario.add("Body", texto);
        if (urlImagen != null && !urlImagen.isBlank()) {
            formulario.add("MediaUrl", urlImagen);
        }
        try {
            MensajeTwilio respuesta = http.post()
                    .uri("/Accounts/{sid}/Messages.json", accountSid)
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .body(MensajeTwilio.class);
            if (respuesta == null || respuesta.sid() == null) {
                throw new WhatsAppException("Twilio no devolvió el id del mensaje.", null);
            }
            return respuesta.sid();
        } catch (RestClientException e) {
            throw new WhatsAppException("Twilio no aceptó el WhatsApp: " + e.getMessage(), e);
        }
    }

    @Override
    public EstadoWhatsApp estado(String idMensaje) {
        try {
            MensajeTwilio m = http.get()
                    .uri("/Accounts/{sid}/Messages/{mensaje}.json", accountSid, idMensaje)
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .retrieve()
                    .body(MensajeTwilio.class);
            return m == null ? new EstadoWhatsApp(EstadoWhatsApp.ENVIANDO, null)
                    : EstadoWhatsApp.deTwilio(m.status(), m.errorCode());
        } catch (RestClientException e) {
            throw new WhatsAppException("No se pudo consultar el WhatsApp en Twilio: " + e.getMessage(), e);
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    /** Lo que se usa de la respuesta de Twilio (trae muchos campos más). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record MensajeTwilio(String sid, String status, @JsonProperty("error_code") Integer errorCode) {
    }
}
