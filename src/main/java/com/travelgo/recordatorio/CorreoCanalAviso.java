package com.travelgo.recordatorio;

import com.travelgo.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Canal de correo (SMTP) con el {@link JavaMailSender} de Spring.
 * <p>
 * Se configura con variables de entorno (ver docs/recordatorios.md): {@code MAIL_HOST}, {@code MAIL_PORT},
 * {@code MAIL_USERNAME}, {@code MAIL_PASSWORD} y {@code TRAVELGO_CORREO_REMITENTE}. Sin ellas el canal queda
 * "no configurado": la aplicación arranca igual y no se envía nada.
 * <p>
 * Para probar sin escribirle a usuarios reales: {@code TRAVELGO_CORREO_REDIRIGIR_A} manda todos los correos a esa
 * dirección (el asunto dice para quién era).
 */
@Component
public class CorreoCanalAviso implements CanalAviso {

    private static final Logger LOG = LoggerFactory.getLogger(CorreoCanalAviso.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String remitente;
    private final String redirigirA;

    public CorreoCanalAviso(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${spring.mail.host:}") String host,
                            @Value("${travelgo.recordatorios.correo.remitente:}") String remitente,
                            @Value("${travelgo.recordatorios.correo.redirigir-a:}") String redirigirA) {
        this.mailSender = mailSender;
        this.host = limpiar(host);
        this.remitente = limpiar(remitente);
        this.redirigirA = limpiar(redirigirA);
        if (!configurado()) {
            LOG.info("Recordatorios por correo desactivados: falta MAIL_HOST o TRAVELGO_CORREO_REMITENTE "
                    + "(ver docs/recordatorios.md).");
        } else if (!this.redirigirA.isEmpty()) {
            LOG.info("Recordatorios por correo en modo prueba: todos van a {}.", this.redirigirA);
        }
    }

    @Override
    public CanalEnvio canal() {
        return CanalEnvio.CORREO;
    }

    @Override
    public boolean configurado() {
        return !host.isEmpty() && !remitente.isEmpty() && mailSender.getIfAvailable() != null;
    }

    @Override
    public boolean aceptaAvisos(Usuario usuario) {
        return usuario.isNotificacionesCorreo()
                && usuario.getCorreo() != null && !usuario.getCorreo().isBlank();
    }

    @Override
    public void enviar(Usuario usuario, Aviso aviso) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || !configurado()) {
            throw new CanalAvisoException("El correo no está configurado.");
        }
        SimpleMailMessage correo = new SimpleMailMessage();
        correo.setFrom(remitente);
        if (redirigirA.isEmpty()) {
            correo.setTo(usuario.getCorreo());
            correo.setSubject(aviso.asunto());
        } else {
            correo.setTo(redirigirA);
            correo.setSubject("[Prueba, para " + usuario.getCorreo() + "] " + aviso.asunto());
        }
        correo.setText(aviso.texto());
        try {
            sender.send(correo);
        } catch (MailException e) {
            throw new CanalAvisoException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private static String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
