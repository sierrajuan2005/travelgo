package com.travelgo.recordatorio;

import com.travelgo.model.Usuario;
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

/**
 * Canal de SMS con la API REST de Twilio (adaptador: traduce un {@link Aviso} a la petición de Twilio).
 * Si se cambia de proveedor, solo cambia esta clase (variaciones protegidas).
 * <p>
 * Credenciales en variables de entorno: {@code TWILIO_ACCOUNT_SID}, {@code TWILIO_AUTH_TOKEN} y
 * {@code TWILIO_NUMERO_ORIGEN}. Sin ellas el canal queda "no configurado" y el correo cubre los avisos.
 * Con la cuenta de prueba de Twilio solo se puede escribir a números verificados en la consola de Twilio.
 * <p>
 * Para probar sin escribirle a usuarios reales: {@code TRAVELGO_SMS_REDIRIGIR_A} manda todos los SMS a ese número.
 */
@Component
public class TwilioSmsCanalAviso implements CanalAviso {

    private static final Logger LOG = LoggerFactory.getLogger(TwilioSmsCanalAviso.class);
    private static final String URL_BASE = "https://api.twilio.com/2010-04-01";
    /** Un SMS tiene 160 caracteres; los más largos se parten y se cobra cada parte. */
    static final int MAXIMO_CARACTERES = 160;

    private final RestClient http;
    private final String accountSid;
    private final String authToken;
    private final String numeroOrigen;
    private final String redirigirA;
    private final String codigoPais;

    public TwilioSmsCanalAviso(@Qualifier(ProveedoresDestinoConfig.HTTP_PROVEEDORES) RestClient.Builder http,
                               @Value("${travelgo.recordatorios.sms.twilio.account-sid:}") String accountSid,
                               @Value("${travelgo.recordatorios.sms.twilio.auth-token:}") String authToken,
                               @Value("${travelgo.recordatorios.sms.twilio.numero-origen:}") String numeroOrigen,
                               @Value("${travelgo.recordatorios.sms.redirigir-a:}") String redirigirA,
                               @Value("${travelgo.recordatorios.sms.codigo-pais:+57}") String codigoPais) {
        this.http = http.clone().baseUrl(URL_BASE).build();
        this.accountSid = limpiar(accountSid);
        this.authToken = limpiar(authToken);
        this.numeroOrigen = limpiar(numeroOrigen);
        this.redirigirA = limpiar(redirigirA);
        this.codigoPais = limpiar(codigoPais);
        if (!configurado()) {
            LOG.info("Recordatorios por SMS desactivados: faltan TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN o "
                    + "TWILIO_NUMERO_ORIGEN (ver docs/recordatorios.md).");
        }
    }

    @Override
    public CanalEnvio canal() {
        return CanalEnvio.SMS;
    }

    @Override
    public boolean configurado() {
        return !accountSid.isEmpty() && !authToken.isEmpty() && !numeroOrigen.isEmpty();
    }

    @Override
    public boolean aceptaAvisos(Usuario usuario) {
        return usuario.isNotificacionesSms() && usuario.getTelefono() != null && !usuario.getTelefono().isBlank();
    }

    @Override
    public void enviar(Usuario usuario, Aviso aviso) {
        if (!configurado()) {
            throw new CanalAvisoException("El SMS no está configurado.");
        }
        String destino = redirigirA.isEmpty() ? usuario.getTelefono() : redirigirA;

        MultiValueMap<String, String> formulario = new LinkedMultiValueMap<>();
        formulario.add("To", formatoInternacional(destino, codigoPais));
        formulario.add("From", numeroOrigen);
        formulario.add("Body", recortar(aviso.textoCorto()));
        try {
            http.post()
                    .uri("/Accounts/{sid}/Messages.json", accountSid)
                    .headers(h -> h.setBasicAuth(accountSid, authToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formulario)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new CanalAvisoException("Twilio no aceptó el SMS: " + e.getMessage(), e);
        }
    }

    /**
     * Número en formato E.164, el que pide Twilio: {@code "300 123 4567" -> "+573001234567"}.
     * Si ya trae el "+", se respeta.
     */
    static String formatoInternacional(String telefono, String codigoPais) {
        if (telefono == null || telefono.isBlank()) {
            throw new CanalAvisoException("El usuario no tiene teléfono.");
        }
        String limpio = telefono.trim();
        String digitos = limpio.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            throw new CanalAvisoException("El teléfono no tiene números: " + telefono);
        }
        return limpio.startsWith("+") ? "+" + digitos : codigoPais + digitos;
    }

    /** Corta el texto a un solo SMS. */
    static String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= MAXIMO_CARACTERES ? texto : texto.substring(0, MAXIMO_CARACTERES - 3) + "...";
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
