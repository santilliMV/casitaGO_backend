package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.MensajeRequest;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Mensaje;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.MensajeRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MensajeServiceEventosTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private MensajeRepository mensajeRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CanalEventosService canalEventosService;

    @InjectMocks
    private MensajeService mensajeService;

    private Usuario anfitrion;
    private Usuario huesped;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Publicacion de prueba");
        publicacion.setDescripcion("Descripcion");
        publicacion.setUbicacionTextual("Ubicacion");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);

        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(mensajeRepository.save(any(Mensaje.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private MensajeRequest solicitud(UUID huespedId) {
        MensajeRequest request = new MensajeRequest();
        request.setContenido("Hola");
        request.setHuespedId(huespedId);
        return request;
    }

    @Test
    void enviarMensaje_comoHuesped_avisaAlAnfitrion() {
        mensajeService.enviarMensaje(publicacion.getId(), huesped, solicitud(null));

        verify(canalEventosService).publicar(anfitrion.getId(), "mensaje", Map.of(
                "publicacionId", publicacion.getId().toString(),
                "huespedId", huesped.getId().toString()));
    }

    @Test
    void enviarMensaje_comoAnfitrion_avisaAlHuesped() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));

        mensajeService.enviarMensaje(publicacion.getId(), anfitrion, solicitud(huesped.getId()));

        verify(canalEventosService).publicar(huesped.getId(), "mensaje", Map.of(
                "publicacionId", publicacion.getId().toString(),
                "huespedId", huesped.getId().toString()));
    }
}