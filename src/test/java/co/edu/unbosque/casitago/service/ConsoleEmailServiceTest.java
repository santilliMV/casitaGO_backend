package co.edu.unbosque.casitago.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ConsoleEmailServiceTest {

    @Test
    void enviarCodigoRecuperacion_noLanzaExcepcion() {
        ConsoleEmailService emailService = new ConsoleEmailService();

        assertThatCode(() -> emailService.enviarCodigoRecuperacion("ana@example.com", "Ana", "123456"))
                .doesNotThrowAnyException();
    }
}