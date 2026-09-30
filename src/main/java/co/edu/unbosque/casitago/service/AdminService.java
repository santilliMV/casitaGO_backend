package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.EventoAuditoriaResponse;
import co.edu.unbosque.casitago.dto.RestriccionRequest;
import co.edu.unbosque.casitago.dto.RestriccionResponse;
import co.edu.unbosque.casitago.entity.EventoAuditoria;
import co.edu.unbosque.casitago.entity.RestriccionUsuario;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.EventoAuditoriaRepository;
import co.edu.unbosque.casitago.repository.RestriccionUsuarioRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private static final String ENTIDAD_USUARIOS = "usuarios";
    private static final int MAXIMO_EVENTOS = 100;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RestriccionUsuarioRepository restriccionUsuarioRepository;

    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Autowired
    private AuditService auditService;

    // ---------- RF-25: restringir usuario ----------
    @Transactional
    public RestriccionResponse restringirUsuario(UUID usuarioId, Usuario admin, RestriccionRequest request) {
        verificarAdministrador(admin);

        if (usuarioId.equals(admin.getId())) {
            throw new RuntimeException("No puedes restringir tu propia cuenta");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("El usuario no existe"));

        if (usuario.getRol() == RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("No se puede restringir a otro administrador");
        }

        if (restriccionUsuarioRepository.existsByUsuarioIdAndActivaTrue(usuarioId)) {
            throw new RuntimeException("El usuario ya tiene una restricción activa");
        }

        RestriccionUsuario restriccion = new RestriccionUsuario();
        restriccion.setUsuario(usuario);
        restriccion.setMotivo(request.getMotivo());
        restriccion.setCreadoPor(admin);
        restriccion = restriccionUsuarioRepository.save(restriccion);

        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        auditService.registrar(admin.getId(), ENTIDAD_USUARIOS, "RESTRINGIR_USUARIO", "EXITOSO",
                Map.of("usuarioRestringido", usuarioId.toString(), "motivo", request.getMotivo()));

        return RestriccionResponse.desde(restriccion);
    }

    @Transactional
    public RestriccionResponse levantarRestriccion(UUID usuarioId, Usuario admin) {
        verificarAdministrador(admin);

        RestriccionUsuario restriccion = restriccionUsuarioRepository.findByUsuarioIdAndActivaTrue(usuarioId)
                .orElseThrow(() -> new RuntimeException("El usuario no tiene una restricción activa"));

        restriccion.setActiva(false);
        restriccionUsuarioRepository.save(restriccion);

        Usuario usuario = restriccion.getUsuario();
        usuario.setActivo(true);
        usuarioRepository.save(usuario);

        auditService.registrar(admin.getId(), ENTIDAD_USUARIOS, "LEVANTAR_RESTRICCION", "EXITOSO",
                Map.of("usuarioLiberado", usuarioId.toString()));

        return RestriccionResponse.desde(restriccion);
    }

    // ---------- RF-26: consultar eventos de auditoría ----------
    public List<EventoAuditoriaResponse> consultarAuditoria(Usuario admin, String entidad, String accion, UUID usuarioId) {
        verificarAdministrador(admin);

        String entidadFiltro = entidad == null ? "" : entidad.trim();
        String accionFiltro = accion == null ? "" : accion.trim();
        boolean filtrarUsuario = usuarioId != null;
        UUID usuarioFiltro = filtrarUsuario ? usuarioId : new UUID(0L, 0L);

        List<EventoAuditoria> eventos = eventoAuditoriaRepository.buscar(
                entidadFiltro, accionFiltro, filtrarUsuario, usuarioFiltro, PageRequest.of(0, MAXIMO_EVENTOS));

        List<EventoAuditoriaResponse> respuestas = new ArrayList<>();
        for (EventoAuditoria evento : eventos) {
            respuestas.add(EventoAuditoriaResponse.desde(evento));
        }
        return respuestas;
    }

    private void verificarAdministrador(Usuario usuario) {
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción");
        }
    }
}