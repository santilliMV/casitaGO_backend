package co.edu.unbosque.casitago.config;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private JwtAuthenticationFilter crearFiltro() {
        return new JwtAuthenticationFilter(jwtService, usuarioRepository);
    }

    private Usuario usuarioActivo() {
        Usuario usuario = new Usuario("Ana", "ana@example.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }

    @Test
    void sinHeaderAuthorization_noAutenticaYContinua() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
    }

    @Test
    void headerSinBearer_noAutenticaYContinua() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic algo");
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenValidoYUsuarioActivo_autenticaCorrectamente() throws Exception {
        Usuario usuario = usuarioActivo();
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.validarYObtenerCorreo("token-valido")).thenReturn("ana@example.com");
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(usuario);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void tokenInvalido_noAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalido");
        when(jwtService.validarYObtenerCorreo("token-invalido")).thenReturn(null);
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(usuarioRepository, never()).findByCorreo(anyString());
    }

    @Test
    void usuarioDelTokenYaNoExiste_noAutentica() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.validarYObtenerCorreo("token-valido")).thenReturn("fantasma@example.com");
        when(usuarioRepository.findByCorreo("fantasma@example.com")).thenReturn(Optional.empty());
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void usuarioDesactivado_noAutentica() throws Exception {
        Usuario usuario = usuarioActivo();
        usuario.setActivo(false);
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valido");
        when(jwtService.validarYObtenerCorreo("token-valido")).thenReturn("ana@example.com");
        when(usuarioRepository.findByCorreo("ana@example.com")).thenReturn(Optional.of(usuario));
        filter = crearFiltro();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}