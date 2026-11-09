package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseMfaTest {

    @Test
    void codigoMfaRequerido_marcaElIndicadorYNoTraeSesion() {
        LoginResponse respuesta = LoginResponse.codigoMfaRequerido();

        assertThat(respuesta.isMfaRequerido()).isTrue();
        assertThat(respuesta.getToken()).isNull();
        assertThat(respuesta.getUsuarioId()).isNull();
        assertThat(respuesta.getNombre()).isNull();
        assertThat(respuesta.getRol()).isNull();
        assertThat(respuesta.getExpiraEnSegundos()).isZero();
    }

    @Test
    void respuestaNormal_noPideCodigoMfa() {
        LoginResponse respuesta = new LoginResponse("jwt", 1800L, UUID.randomUUID(), "Ana", RolUsuario.HUESPED);

        assertThat(respuesta.isMfaRequerido()).isFalse();
        assertThat(respuesta.getToken()).isEqualTo("jwt");
    }

    @Test
    void setMfaRequerido_cambiaElIndicador() {
        LoginResponse respuesta = new LoginResponse();

        respuesta.setMfaRequerido(true);
        assertThat(respuesta.isMfaRequerido()).isTrue();

        respuesta.setMfaRequerido(false);
        assertThat(respuesta.isMfaRequerido()).isFalse();
    }
}