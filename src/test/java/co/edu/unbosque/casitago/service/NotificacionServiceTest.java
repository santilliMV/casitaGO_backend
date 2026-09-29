package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.NotificacionResponse;
import co.edu.unbosque.casitago.entity.Notificacion;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionService notificacionService;

    @Test
    void crear_deberiaGuardarNotificacionNoLeida() {
        Usuario usuario = mock(Usuario.class);

        notificacionService.crear(usuario, "RESERVA_CONFIRMADA", "Tu reserva fue confirmada");

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        Notificacion guardada = captor.getValue();
        assertEquals(usuario, guardada.getUsuario());
        assertEquals("RESERVA_CONFIRMADA", guardada.getTipoEvento());
        assertEquals("Tu reserva fue confirmada", guardada.getMensaje());
        assertFalse(guardada.isLeida());
    }

    @Test
    void listarMisNotificaciones_deberiaDevolverLasDelUsuario() {
        UUID usuarioId = UUID.randomUUID();

        Notificacion primera = new Notificacion();
        primera.setTipoEvento("RESERVA_CANCELADA");
        primera.setMensaje("Cancelaste tu reserva");

        Notificacion segunda = new Notificacion();
        segunda.setTipoEvento("RESERVA_CONFIRMADA");
        segunda.setMensaje("Tu reserva fue confirmada");

        when(notificacionRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId))
                .thenReturn(List.of(primera, segunda));

        List<NotificacionResponse> resultado = notificacionService.listarMisNotificaciones(usuarioId);

        assertEquals(2, resultado.size());
        assertEquals("RESERVA_CANCELADA", resultado.get(0).getTipoEvento());
        assertEquals("Tu reserva fue confirmada", resultado.get(1).getMensaje());
    }

    @Test
    void marcarComoLeida_notificacionPropia_deberiaMarcarLeida() {
        UUID notificacionId = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(UUID.randomUUID());

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);

        when(notificacionRepository.findById(notificacionId)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificacionResponse respuesta = notificacionService.marcarComoLeida(notificacionId, usuario);

        assertTrue(respuesta.isLeida());
        assertTrue(notificacion.isLeida());
    }

    @Test
    void marcarComoLeida_notificacionAjena_deberiaFallar() {
        UUID notificacionId = UUID.randomUUID();

        Usuario dueno = mock(Usuario.class);
        when(dueno.getId()).thenReturn(UUID.randomUUID());
        Usuario otro = mock(Usuario.class);
        when(otro.getId()).thenReturn(UUID.randomUUID());

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(dueno);

        when(notificacionRepository.findById(notificacionId)).thenReturn(Optional.of(notificacion));

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> notificacionService.marcarComoLeida(notificacionId, otro));

        assertEquals("No tienes permiso para modificar esta notificación", e.getMessage());
        assertFalse(notificacion.isLeida());
        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void marcarComoLeida_notificacionInexistente_deberiaFallar() {
        UUID notificacionId = UUID.randomUUID();
        Usuario usuario = mock(Usuario.class);

        when(notificacionRepository.findById(notificacionId)).thenReturn(Optional.empty());

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> notificacionService.marcarComoLeida(notificacionId, usuario));

        assertEquals("La notificación no existe", e.getMessage());
    }
}