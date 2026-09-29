package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.NotificacionResponse;
import co.edu.unbosque.casitago.entity.Notificacion;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository notificacionRepository;

    public void crear(Usuario usuario, String tipoEvento, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipoEvento(tipoEvento);
        notificacion.setMensaje(mensaje);
        notificacionRepository.save(notificacion);
    }

    public List<NotificacionResponse> listarMisNotificaciones(UUID usuarioId) {
        return notificacionRepository.findByUsuarioIdOrderByCreadoEnDesc(usuarioId)
                .stream()
                .map(NotificacionResponse::desde)
                .collect(Collectors.toList());
    }

    public NotificacionResponse marcarComoLeida(UUID notificacionId, Usuario usuario) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RuntimeException("La notificación no existe"));

        if (!notificacion.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta notificación");
        }

        notificacion.setLeida(true);
        notificacion = notificacionRepository.save(notificacion);
        return NotificacionResponse.desde(notificacion);
    }
}