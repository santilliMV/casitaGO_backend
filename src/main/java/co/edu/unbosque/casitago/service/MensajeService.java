package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.ConversacionResponse;
import co.edu.unbosque.casitago.dto.MensajeRequest;
import co.edu.unbosque.casitago.dto.MensajeResponse;
import co.edu.unbosque.casitago.entity.Mensaje;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.MensajeRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MensajeService {

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public MensajeResponse enviarMensaje(UUID publicacionId, Usuario usuario, MensajeRequest request) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("La publicación no existe"));

        Usuario huesped;
        if (usuario.getRol() == RolUsuario.HUESPED) {
            huesped = usuario;
        } else if (usuario.getRol() == RolUsuario.ANFITRION) {
            if (!publicacion.getAnfitrion().getId().equals(usuario.getId())) {
                throw new RuntimeException("Solo el ANFITRIÓN propietario puede responder en esta publicación");
            }
            if (request.getHuespedId() == null) {
                throw new RuntimeException("Debes indicar a qué huésped le estás respondiendo");
            }
            huesped = buscarHuesped(request.getHuespedId());
        } else {
            throw new RuntimeException("Solo un HUÉSPED o un ANFITRIÓN pueden enviar mensajes");
        }

        Mensaje mensaje = new Mensaje();
        mensaje.setPublicacion(publicacion);
        mensaje.setHuesped(huesped);
        mensaje.setEmisor(usuario);
        mensaje.setContenido(request.getContenido());

        mensaje = mensajeRepository.save(mensaje);
        return MensajeResponse.desde(mensaje);
    }

    public List<MensajeResponse> listarConversacion(UUID publicacionId, UUID huespedId, Usuario usuario) {
        Publicacion publicacion = publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("La publicación no existe"));

        boolean esHuesped = usuario.getId().equals(huespedId);
        boolean esAnfitrion = publicacion.getAnfitrion().getId().equals(usuario.getId());
        if (!esHuesped && !esAnfitrion) {
            throw new RuntimeException("No tienes permiso para ver esta conversación");
        }

        List<Mensaje> mensajes = mensajeRepository
                .findByPublicacionIdAndHuespedIdOrderByCreadoEnAsc(publicacionId, huespedId);

        for (Mensaje mensaje : mensajes) {
            if (!mensaje.getEmisor().getId().equals(usuario.getId()) && !mensaje.isLeido()) {
                mensaje.setLeido(true);
                mensajeRepository.save(mensaje);
            }
        }

        return mensajes.stream().map(MensajeResponse::desde).collect(Collectors.toList());
    }

    public List<ConversacionResponse> listarMisConversaciones(UUID usuarioId) {
        List<Mensaje> mensajes = mensajeRepository.buscarConversacionesDeUsuario(usuarioId);

        Map<String, Mensaje> ultimoPorConversacion = new LinkedHashMap<>();
        for (Mensaje mensaje : mensajes) {
            String clave = mensaje.getPublicacion().getId() + "-" + mensaje.getHuesped().getId();
            ultimoPorConversacion.putIfAbsent(clave, mensaje);
        }

        List<ConversacionResponse> respuesta = new ArrayList<>();
        for (Mensaje ultimoMensaje : ultimoPorConversacion.values()) {
            respuesta.add(ConversacionResponse.desde(ultimoMensaje));
        }
        return respuesta;
    }

    private Usuario buscarHuesped(UUID huespedId) {
        Usuario huesped = usuarioRepository.findById(huespedId)
                .orElseThrow(() -> new RuntimeException("El huésped indicado no existe"));
        if (huesped.getRol() != RolUsuario.HUESPED) {
            throw new RuntimeException("El usuario indicado no es un HUÉSPED");
        }
        return huesped;
    }
}