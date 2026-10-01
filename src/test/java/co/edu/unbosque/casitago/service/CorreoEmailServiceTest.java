package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CorreoEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoEmailService correoEmailService;

    @BeforeEach
    void preparar() {
        correoEmailService = new CorreoEmailService(mailSender, "casitago@gmail.com");
    }

    @Test
    void enviarCodigoRecuperacion_enviaCorreoConElCodigo() {
        correoEmailService.enviarCodigoRecuperacion("ana@example.com", "Ana", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();

        assertEquals("casitago@gmail.com", mensaje.getFrom());
        assertEquals("ana@example.com", mensaje.getTo()[0]);
        assertTrue(mensaje.getSubject().contains("recuperación"));
        assertTrue(mensaje.getText().contains("123456"));
    }

    @Test
    void enviarCodigoMfa_enviaCorreoConElCodigo() {
        correoEmailService.enviarCodigoMfa("ana@example.com", "Ana", "654321");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();

        assertEquals("ana@example.com", mensaje.getTo()[0]);
        assertTrue(mensaje.getSubject().contains("verificación"));
        assertTrue(mensaje.getText().contains("654321"));
    }

    @Test
    void enviarCodigoRecuperacion_siFallaElEnvio_lanzaRuntimeException() {
        doThrow(new MailSendException("fallo SMTP")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> correoEmailService.enviarCodigoRecuperacion("ana@example.com", "Ana", "123456"));

        assertEquals("No se pudo enviar el correo. Intenta de nuevo más tarde.", error.getMessage());
    }
}