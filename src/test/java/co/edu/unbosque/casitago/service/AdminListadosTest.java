package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.PublicacionAdminResponse;
import co.edu.unbosque.casitago.dto.UsuarioAdminResponse;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RestriccionUsuario;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.RestriccionUsuarioRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminListadosTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RestriccionUsuarioRepository restriccionUsuarioRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @InjectMocks
    private AdminService adminService;

    private Usuario admin;
    private Usuario huesped;
    private Usuario anfitrion;

    @BeforeEach
    void setUp() {
        admin = new Usuario("Admin", "admin@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());

        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());
    }

    private Publicacion publicacionDePrueba(String titulo) {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo(titulo);
        publicacion.setCiudad("Bogotá");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setPrecioNoche(new BigDecimal("150000"));
        return publicacion;
    }

    // ---------- Listado de usuarios ----------

    @Test
    void listarUsuarios_comoAdmin_deberiaMarcarLosRestringidos() {
        huesped.setActivo(false);
        RestriccionUsuario restriccion = new RestriccionUsuario();
        restriccion.setUsuario(huesped);

        when(restriccionUsuarioRepository.findByActivaTrue()).thenReturn(List.of(restriccion));
        when(usuarioRepository.findAllByOrderByCreadoEnDesc()).thenReturn(List.of(huesped, anfitrion));

        List<UsuarioAdminResponse> respuestas = adminService.listarUsuarios(admin);

        assertEquals(2, respuestas.size());
        assertEquals("hugo@correo.com", respuestas.get(0).getCorreo());
        assertTrue(respuestas.get(0).isRestringido());
        assertFalse(respuestas.get(0).isActivo());
        assertEquals(RolUsuario.ANFITRION, respuestas.get(1).getRol());
        assertFalse(respuestas.get(1).isRestringido());
        assertTrue(respuestas.get(1).isActivo());
    }

    @Test
    void listarUsuarios_sinUsuarios_deberiaDevolverListaVacia() {
        when(restriccionUsuarioRepository.findByActivaTrue()).thenReturn(List.of());
        when(usuarioRepository.findAllByOrderByCreadoEnDesc()).thenReturn(List.of());

        assertTrue(adminService.listarUsuarios(admin).isEmpty());
    }

    @Test
    void listarUsuarios_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.listarUsuarios(huesped));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verifyNoInteractions(usuarioRepository, restriccionUsuarioRepository);
    }

    // ---------- Listado de publicaciones ----------

    @Test
    void listarPublicaciones_comoAdmin_deberiaDevolverTodasConDatosDelAnfitrion() {
        when(publicacionRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(publicacionDePrueba("Apto en Chapinero"), publicacionDePrueba("Casa en Suba")));

        List<PublicacionAdminResponse> respuestas = adminService.listarPublicaciones(admin);

        assertEquals(2, respuestas.size());
        assertEquals("Apto en Chapinero", respuestas.get(0).getTitulo());
        assertEquals("BORRADOR", respuestas.get(0).getEstado());
        assertEquals("APARTAMENTO", respuestas.get(0).getTipo());
        assertEquals(anfitrion.getId(), respuestas.get(0).getAnfitrionId());
        assertEquals("Ana Anfitriona", respuestas.get(0).getAnfitrionNombre());
        assertEquals("ana@correo.com", respuestas.get(0).getAnfitrionCorreo());
    }

    @Test
    void listarPublicaciones_sinPublicaciones_deberiaDevolverListaVacia() {
        when(publicacionRepository.findAll(any(Sort.class))).thenReturn(List.of());

        assertTrue(adminService.listarPublicaciones(admin).isEmpty());
    }

    @Test
    void listarPublicaciones_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.listarPublicaciones(anfitrion));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verifyNoInteractions(publicacionRepository);
    }
}