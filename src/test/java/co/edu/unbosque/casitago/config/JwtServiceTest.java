package co.edu.unbosque.casitago.config;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // 32+ bytes, requerido por HS256
    private static final String SECRETO_VALIDO = "unbosque-casitago-secreto-de-pruebas-1234567890";

    @Test
    void generarToken_y_validarYObtenerCorreo_devuelvenElMismoCorreo() {
        JwtService jwtService = new JwtService(SECRETO_VALIDO, 60_000);
        UUID usuarioId = UUID.randomUUID();

        String token = jwtService.generarToken(usuarioId, "ana@example.com", "HUESPED");
        String correoExtraido = jwtService.validarYObtenerCorreo(token);

        assertThat(token).isNotBlank();
        assertThat(correoExtraido).isEqualTo("ana@example.com");
    }

    @Test
    void validarYObtenerCorreo_conTokenInvalido_devuelveNull() {
        JwtService jwtService = new JwtService(SECRETO_VALIDO, 60_000);

        assertThat(jwtService.validarYObtenerCorreo("esto-no-es-un-jwt")).isNull();
    }

    @Test
    void validarYObtenerCorreo_conTokenExpirado_devuelveNull() throws InterruptedException {
        // expiración casi inmediata (1 ms) para forzar el vencimiento
        JwtService jwtService = new JwtService(SECRETO_VALIDO, 1);
        String token = jwtService.generarToken(UUID.randomUUID(), "ana@example.com", "HUESPED");

        Thread.sleep(15);

        assertThat(jwtService.validarYObtenerCorreo(token)).isNull();
    }

    @Test
    void validarYObtenerCorreo_conTokenFirmadoConOtraClave_devuelveNull() {
        JwtService emisor = new JwtService(SECRETO_VALIDO, 60_000);
        JwtService validador = new JwtService("otro-secreto-totalmente-distinto-1234567890ab", 60_000);

        String token = emisor.generarToken(UUID.randomUUID(), "ana@example.com", "HUESPED");

        assertThat(validador.validarYObtenerCorreo(token)).isNull();
    }

    @Test
    void constructor_conSecretoCorto_lanzaExcepcion() {
        assertThatThrownBy(() -> new JwtService("muy-corto", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }
}
