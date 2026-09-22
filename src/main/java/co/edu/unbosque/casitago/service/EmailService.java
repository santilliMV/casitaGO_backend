package co.edu.unbosque.casitago.service;

/**
 * Abstracción del envío de correo. RF-03 exige enviar un código de
 * verificación al correo del usuario; la implementación real (SMTP, SES,
 * Resend, etc.) puede intercambiarse sin tocar AuthService.
 */
public interface EmailService {

    void enviarCodigoRecuperacion(String correoDestino, String nombreDestino, String codigo);
}
