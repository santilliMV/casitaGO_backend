package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CodigoRecuperacionTest {

    private final Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);

    @Test
    void esValido_conCodigoCorrectoYNoExpirado_devuelveTrue() {
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(10));

        assertThat(codigo.esValido("123456")).isTrue();
    }

    @Test
    void esValido_conCodigoIncorrecto_devuelveFalse() {
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(10));

        assertThat(codigo.esValido("000000")).isFalse();
    }

    @Test
    void esValido_conCodigoExpirado_devuelveFalse() {
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().minusMinutes(1));

        assertThat(codigo.esValido("123456")).isFalse();
    }

    @Test
    void esValido_conCodigoYaUsado_devuelveFalse() {
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "123456", OffsetDateTime.now().plusMinutes(10));
        codigo.marcarUsado();

        assertThat(codigo.esValido("123456")).isFalse();
        assertThat(codigo.isUsado()).isTrue();
    }

    @Test
    void getters_devuelvenLosValoresDelConstructor() {
        OffsetDateTime expiracion = OffsetDateTime.now().plusMinutes(15);
        CodigoRecuperacion codigo = new CodigoRecuperacion(usuario, "654321", expiracion);

        assertThat(codigo.getUsuario()).isEqualTo(usuario);
        assertThat(codigo.getCodigo()).isEqualTo("654321");
        assertThat(codigo.getExpiraEn()).isEqualTo(expiracion);
        assertThat(codigo.isUsado()).isFalse();
    }
}