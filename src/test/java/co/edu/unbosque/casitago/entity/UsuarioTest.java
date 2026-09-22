package co.edu.unbosque.casitago.entity;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioTest {

    @Test
    void getUsername_devuelveElCorreo() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);

        assertThat(usuario.getUsername()).isEqualTo("ana@example.com");
    }

    @Test
    void getPassword_devuelveElHash() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash-seguro", RolUsuario.HUESPED);

        assertThat(usuario.getPassword()).isEqualTo("hash-seguro");
    }

    @Test
    void getAuthorities_incluyePrefijoRoleConElRol() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.ANFITRION);

        assertThat(usuario.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ANFITRION");
    }

    @Test
    void isEnabled_reflejaElCampoActivo() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);

        assertThat(usuario.isEnabled()).isTrue();

        usuario.setActivo(false);
        assertThat(usuario.isEnabled()).isFalse();
    }

    @Test
    void banderasDeCuenta_siempreVerdaderasSalvoEnabled() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);

        assertThat(usuario.isAccountNonExpired()).isTrue();
        assertThat(usuario.isAccountNonLocked()).isTrue();
        assertThat(usuario.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void prePersist_asignaCreadoEnSiEstaVacio() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);
        assertThat(usuario.getCreadoEn()).isNull();

        usuario.prePersist();

        assertThat(usuario.getCreadoEn()).isNotNull();
        assertThat(usuario.getCreadoEn()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void prePersist_noSobrescribeCreadoEnExistente() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);
        OffsetDateTime fechaOriginal = OffsetDateTime.now().minusDays(1);
        ReflectionTestUtils.setField(usuario, "creadoEn", fechaOriginal);

        usuario.prePersist();

        assertThat(usuario.getCreadoEn()).isEqualTo(fechaOriginal);
    }

    @Test
    void setNombreYSetCorreo_actualizanLosCampos() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);

        usuario.setNombre("Ana Ríos");
        usuario.setCorreo("ana.rios@example.com");

        assertThat(usuario.getNombre()).isEqualTo("Ana Ríos");
        assertThat(usuario.getCorreo()).isEqualTo("ana.rios@example.com");
    }
}