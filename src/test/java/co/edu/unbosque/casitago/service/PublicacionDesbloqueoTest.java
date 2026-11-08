package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.ImagenPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionDesbloqueoTest {

    private static final String MENSAJE_NO_ADMIN = "Solo un ADMINISTRADOR puede desbloquear una publicación.";
    private static final String MENSAJE_NO_BLOQUEADA = "La publicación no está bloqueada.";

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ImagenStorageService imagenStorageService;

    @InjectMocks
    private PublicacionService publicacionService;

    private Usuario administrador;
    private Usuario anfitrion;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        administrador = new Usuario("Ada Admin", "ada@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(administrador, "id", UUID.randomUUID());

        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);

        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
    }

    @Test
    void desbloquearPublicacion_administradorConPublicacionBloqueada_quedaPausadaYSeAudita() {
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        when(usuarioRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));

        PublicacionResponse response = publicacionService.desbloquearPublicacion(administrador.getId(), publicacion.getId());

        assertEquals("PAUSADA", response.getEstado());
        assertEquals(EstadoPublicacion.PAUSADA, publicacion.getEstado());
        verify(auditService).registrar(eq(administrador.getId()), eq("publicaciones"),
                eq("DESBLOQUEAR_PUBLICACION"), eq("EXITOSO"), any());
    }

    @Test
    void desbloquearPublicacion_comoAnfitrion_deberiaLanzarExcepcionYNoCambiarElEstado() {
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        UUID usuarioId = anfitrion.getId();
        UUID publicacionId = publicacion.getId();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publicacionService.desbloquearPublicacion(usuarioId, publicacionId));

        assertEquals(MENSAJE_NO_ADMIN, ex.getMessage());
        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
        verifyNoInteractions(auditService);
    }

    @Test
    void desbloquearPublicacion_publicacionNoBloqueada_deberiaLanzarExcepcionYNoCambiarElEstado() {
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));
        UUID usuarioId = administrador.getId();
        UUID publicacionId = publicacion.getId();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publicacionService.desbloquearPublicacion(usuarioId, publicacionId));

        assertEquals(MENSAJE_NO_BLOQUEADA, ex.getMessage());
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
        verifyNoInteractions(auditService);
    }

    @Test
    void desbloquearPublicacion_luegoElAnfitrionPuedeActivarla() {
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        ImagenPublicacion imagen = new ImagenPublicacion();
        imagen.setPublicacion(publicacion);
        imagen.setUrl("https://ejemplo.com/foto.jpg");
        imagen.setOrden(0);
        publicacion.getImagenes().add(imagen);
        when(usuarioRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));

        publicacionService.desbloquearPublicacion(administrador.getId(), publicacion.getId());
        PublicacionResponse activada = publicacionService.activarPublicacion(anfitrion.getId(), publicacion.getId());

        assertEquals("ACTIVA", activada.getEstado());
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
    }
}
