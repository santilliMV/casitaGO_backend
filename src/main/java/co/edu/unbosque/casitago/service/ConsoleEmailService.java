package co.edu.unbosque.casitago.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Implementación provisional de EmailService: NO envía correo real, solo
 * loguea el código para desarrollo/pruebas locales.
 *
 * REEMPLAZAR antes de la entrega: implementar un EmailService real (ej. con
 * JavaMailSender + SMTP, o un proveedor como Resend/SendGrid) y quitar el
 * @Service de esta clase (o darle @Profile("dev")) para que no quede activa
 * en el ambiente que se sustente. RF-03 dice explícitamente que el envío
 * debe funcionar de verdad, esto es solo el andamiaje mientras se define
 * el proveedor.
 */
@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void enviarCodigoRecuperacion(String correoDestino, String nombreDestino, String codigo) {
        log.warn("[EMAIL SIMULADO] Para: {} <{}> — Código de recuperación: {}", nombreDestino, correoDestino, codigo);
    }
}
