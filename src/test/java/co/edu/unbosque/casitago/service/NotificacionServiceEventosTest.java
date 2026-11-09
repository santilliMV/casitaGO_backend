package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceEventosTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private CanalEventosService canalEventosService;

    @InjectMocks
    private NotificacionService notificacionService;

    @Test
    void crear_avisaAlUsuarioPorElCanalDeEventos() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = new Usuario("Ana", "ana@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(usuario, "id", usuarioId);

        notificacionService.crear(usuario, "RESERVA_CONFIRMADA", "Tu reserva fue confirmada");

        verify(canalEventosService).publicar(usuarioId, "notificacion",
                Map.of("tipoEvento", "RESERVA_CONFIRMADA", "mensaje", "Tu reserva fue confirmada"));
    }
}