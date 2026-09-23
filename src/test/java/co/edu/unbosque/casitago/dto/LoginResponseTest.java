package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseTest {

    @Test
    void constructorVacio_ySetters_asignanLosValores() {
        UUID id = UUID.randomUUID();
        LoginResponse respuesta = new LoginResponse();

        respuesta.setToken("jwt-token");
        respuesta.setExpiraEnSegundos(1800L);
        respuesta.setUsuarioId(id);
        respuesta.setNombre("Ana");
        respuesta.setRol(RolUsuario.HUESPED);

        assertThat(respuesta.getToken()).isEqualTo("jwt-token");
        assertThat(respuesta.getExpiraEnSegundos()).isEqualTo(1800L);
        assertThat(respuesta.getUsuarioId()).isEqualTo(id);
        assertThat(respuesta.getNombre()).isEqualTo("Ana");
        assertThat(respuesta.getRol()).isEqualTo(RolUsuario.HUESPED);
    }

    @Test
    void constructorConTodosLosArgumentos_asignaLosValores() {
        UUID id = UUID.randomUUID();
        LoginResponse respuesta = new LoginResponse("jwt-token", 1800L, id, "Ana", RolUsuario.ANFITRION);

        assertThat(respuesta.getToken()).isEqualTo("jwt-token");
        assertThat(respuesta.getRol()).isEqualTo(RolUsuario.ANFITRION);
    }
}