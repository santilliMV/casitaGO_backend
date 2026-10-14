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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionBloqueadaTest {

    private static final String MENSAJE_BLOQUEADA =
            "Una publicación bloqueada por un administrador no puede cambiar de estado.";

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

    private Usuario anfitrion;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);

        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
    }

    private void agregarImagen() {
        ImagenPublicacion imagen = new ImagenPublicacion();
        imagen.setPublicacion(publicacion);
        imagen.setUrl("https://ejemplo.com/foto.jpg");
        imagen.setOrden(0);
        publicacion.getImagenes().add(imagen);
    }

    @Test
    void activarPublicacion_bloqueada_deberiaLanzarExcepcionYNoCambiarElEstado() {
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        agregarImagen();
        UUID usuarioId = anfitrion.getId();
        UUID publicacionId = publicacion.getId();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publicacionService.activarPublicacion(usuarioId, publicacionId));

        assertEquals(MENSAJE_BLOQUEADA, ex.getMessage());
        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
        verifyNoInteractions(auditService);
    }

    @Test
    void pausarPublicacion_bloqueada_deberiaLanzarExcepcionYNoCambiarElEstado() {
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);
        UUID usuarioId = anfitrion.getId();
        UUID publicacionId = publicacion.getId();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publicacionService.pausarPublicacion(usuarioId, publicacionId));

        assertEquals(MENSAJE_BLOQUEADA, ex.getMessage());
        assertEquals(EstadoPublicacion.BLOQUEADA, publicacion.getEstado());
        verifyNoInteractions(auditService);
    }

    @Test
    void activarPublicacion_pausadaConImagen_deberiaActivarse() {
        publicacion.setEstado(EstadoPublicacion.PAUSADA);
        agregarImagen();

        PublicacionResponse response = publicacionService.activarPublicacion(anfitrion.getId(), publicacion.getId());

        assertEquals("ACTIVA", response.getEstado());
        assertEquals(EstadoPublicacion.ACTIVA, publicacion.getEstado());
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void pausarPublicacion_activa_deberiaPausarse() {
        publicacion.setEstado(EstadoPublicacion.ACTIVA);

        PublicacionResponse response = publicacionService.pausarPublicacion(anfitrion.getId(), publicacion.getId());

        assertEquals("PAUSADA", response.getEstado());
        assertEquals(EstadoPublicacion.PAUSADA, publicacion.getEstado());
        verify(auditService).registrar(any(), any(), any(), any(), any());
    }
}