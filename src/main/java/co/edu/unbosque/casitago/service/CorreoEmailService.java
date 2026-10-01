package co.edu.unbosque.casitago.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementación real de EmailService (RF-03, RF-33): envía el código por
 * correo usando SMTP (Brevo) a través de JavaMailSender.
 */
@Service
public class CorreoEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(CorreoEmailService.class);

    private final JavaMailSender mailSender;
    private final String remitente;

    public CorreoEmailService(JavaMailSender mailSender,
                              @Value("${correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreDestino, String codigo) {
        String asunto = "CasitaGO - Código de recuperación de contraseña";
        String texto = "Hola " + nombreDestino + ",\n\n"
                + "Tu código para recuperar la contraseña es: " + codigo + "\n\n"
                + "Si no fuiste tú quien lo solicitó, ignora este mensaje.";
        enviar(correoDestino, asunto, texto);
    }

    @Override
    public void enviarCodigoMfa(String correoDestino, String nombreDestino, String codigo) {
        String asunto = "CasitaGO - Código de verificación";
        String texto = "Hola " + nombreDestino + ",\n\n"
                + "Tu código de verificación para iniciar sesión es: " + codigo + "\n\n"
                + "Si no fuiste tú quien intentó iniciar sesión, cambia tu contraseña.";
        enviar(correoDestino, asunto, texto);
    }

    private void enviar(String correoDestino, String asunto, String texto) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(correoDestino);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        try {
            mailSender.send(mensaje);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", correoDestino, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo más tarde.");
        }
    }
}