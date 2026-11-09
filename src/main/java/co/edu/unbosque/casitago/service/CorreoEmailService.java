package co.edu.unbosque.casitago.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementación real de EmailService (RF-03, RF-33): envía el código por
 * correo usando SMTP (Brevo) a través de JavaMailSender. El correo lleva una
 * versión HTML y una versión en texto plano (ver PlantillaCorreo).
 */
@Service
public class CorreoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(CorreoEmailService.class);
    private static final String CODIFICACION = "UTF-8";

    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoEmailService(JavaMailSender mailSender,
                              @Value("${correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreDestino, String codigo) {
        enviar(correoDestino,
                "CasitaGO - Código de recuperación de contraseña",
                PlantillaCorreo.textoRecuperacion(nombreDestino, codigo),
                PlantillaCorreo.htmlRecuperacion(nombreDestino, codigo));
    }

    @Override
    public void enviarCodigoMfa(String correoDestino, String nombreDestino, String codigo) {
        enviar(correoDestino,
                "CasitaGO - Código de verificación",
                PlantillaCorreo.textoVerificacion(nombreDestino, codigo),
                PlantillaCorreo.htmlVerificacion(nombreDestino, codigo));
    }

    private void enviar(String correoDestino, String asunto, String texto, String html) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, CODIFICACION);
            ayudante.setFrom(remitente);
            ayudante.setTo(correoDestino);
            ayudante.setSubject(asunto);
            ayudante.setText(texto, html);
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", correoDestino, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo más tarde.");
        }
    }
}