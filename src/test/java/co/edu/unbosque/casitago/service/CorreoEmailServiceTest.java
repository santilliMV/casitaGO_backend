package co.edu.unbosque.casitago.service;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorreoEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private CorreoEmailService correoEmailService;

    @BeforeEach
    void preparar() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        correoEmailService = new CorreoEmailService(mailSender, "casitago@gmail.com");
    }

    private MimeMessage capturarMensajeEnviado() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private String contenidoCompleto(Part parte) throws Exception {
        Object contenido = parte.getContent();
        if (contenido instanceof String texto) {
            return texto;
        }
        if (contenido instanceof Multipart multiparte) {
            StringBuilder acumulado = new StringBuilder();
            for (int i = 0; i < multiparte.getCount(); i++) {
                acumulado.append(contenidoCompleto(multiparte.getBodyPart(i)));
            }
            return acumulado.toString();
        }
        return "";
    }

    @Test
    void enviarCodigoRecuperacion_enviaCorreoConElCodigo() throws Exception {
        correoEmailService.enviarCodigoRecuperacion("ana@example.com", "Ana", "123456");

        MimeMessage mensaje = capturarMensajeEnviado();

        assertEquals("casitago@gmail.com", ((InternetAddress) mensaje.getFrom()[0]).getAddress());
        assertEquals("ana@example.com",
                ((InternetAddress) mensaje.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertTrue(mensaje.getSubject().contains("recuperación"));
        assertTrue(contenidoCompleto(mensaje).contains("123456"));
    }

    @Test
    void enviarCodigoMfa_enviaCorreoConElCodigoEnHtmlYEnTexto() throws Exception {
        correoEmailService.enviarCodigoMfa("ana@example.com", "Ana", "654321");

        MimeMessage mensaje = capturarMensajeEnviado();
        String contenido = contenidoCompleto(mensaje);

        assertEquals("ana@example.com",
                ((InternetAddress) mensaje.getRecipients(Message.RecipientType.TO)[0]).getAddress());
        assertTrue(mensaje.getSubject().contains("verificación"));
        assertTrue(contenido.contains("654321"));
        assertTrue(contenido.contains("<html"));
        assertTrue(contenido.contains("Tu código: 654321"));
    }

    @Test
    void enviarCodigoRecuperacion_siFallaElEnvio_lanzaRuntimeException() {
        doThrow(new MailSendException("fallo SMTP")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> correoEmailService.enviarCodigoRecuperacion("ana@example.com", "Ana", "123456"));

        assertEquals("No se pudo enviar el correo. Intenta de nuevo más tarde.", error.getMessage());
    }
}