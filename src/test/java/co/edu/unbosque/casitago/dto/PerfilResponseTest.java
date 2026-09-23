package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PerfilResponseTest {

    @Test
    void constructorVacio_ySetters_asignanLosValores() {
        UUID id = UUID.randomUUID();
        OffsetDateTime ahora = OffsetDateTime.now();
        PerfilResponse perfil = new PerfilResponse();

        perfil.setId(id);
        perfil.setNombre("Ana");
        perfil.setCorreo("ana@example.com");
        perfil.setRol(RolUsuario.HUESPED);
        perfil.setActivo(true);
        perfil.setCreadoEn(ahora);

        assertThat(perfil.getId()).isEqualTo(id);
        assertThat(perfil.getNombre()).isEqualTo("Ana");
        assertThat(perfil.getCorreo()).isEqualTo("ana@example.com");
        assertThat(perfil.getRol()).isEqualTo(RolUsuario.HUESPED);
        assertThat(perfil.isActivo()).isTrue();
        assertThat(perfil.getCreadoEn()).isEqualTo(ahora);
    }

    @Test
    void desde_construyeAPartirDeUnUsuario() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());

        PerfilResponse perfil = PerfilResponse.desde(usuario);

        assertThat(perfil.getNombre()).isEqualTo("Ana");
        assertThat(perfil.getCorreo()).isEqualTo("ana@example.com");
        assertThat(perfil.getRol()).isEqualTo(RolUsuario.ANFITRION);
        assertThat(perfil.isActivo()).isTrue();
    }
}