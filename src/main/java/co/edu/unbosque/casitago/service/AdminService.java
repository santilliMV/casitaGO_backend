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
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import co.edu.unbosque.casitago.dto.UsuarioAdminResponse;
import java.util.HashSet;
import java.util.Set;
import co.edu.unbosque.casitago.dto.PublicacionAdminResponse;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminService {

    private static final String ENTIDAD_USUARIOS = "usuarios";
    private static final int MAXIMO_EVENTOS = 100;
    private static final int MAXIMO_EXPORTACION = 5000;
    private static final String SEPARADOR = ";";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RestriccionUsuarioRepository restriccionUsuarioRepository;

    @Autowired
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PublicacionRepository publicacionRepository;

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

    // ---------- Listado de usuarios para el administrador ----------
    public List<UsuarioAdminResponse> listarUsuarios(Usuario admin) {
        verificarAdministrador(admin);

        Set<UUID> restringidos = new HashSet<>();
        for (RestriccionUsuario restriccion : restriccionUsuarioRepository.findByActivaTrue()) {
            restringidos.add(restriccion.getUsuario().getId());
        }

        List<UsuarioAdminResponse> respuestas = new ArrayList<>();
        for (Usuario usuario : usuarioRepository.findAllByOrderByCreadoEnDesc()) {
            respuestas.add(UsuarioAdminResponse.desde(usuario, restringidos.contains(usuario.getId())));
        }
        return respuestas;
    }

    // ---------- Listado de publicaciones para moderación ----------
    public List<PublicacionAdminResponse> listarPublicaciones(Usuario admin) {
        verificarAdministrador(admin);

        List<PublicacionAdminResponse> respuestas = new ArrayList<>();
        for (Publicacion publicacion : publicacionRepository.findAll(Sort.by(Sort.Direction.DESC, "creadoEn"))) {
            respuestas.add(PublicacionAdminResponse.desde(publicacion));
        }
        return respuestas;
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
    // ---------- Exportación de la auditoría en CSV (RF-26, RNF-07) ----------
    public byte[] exportarAuditoriaCsv(Usuario admin, String entidad, String accion, UUID usuarioId) {
        verificarAdministrador(admin);

        String entidadFiltro = entidad == null ? "" : entidad.trim();
        String accionFiltro = accion == null ? "" : accion.trim();
        boolean filtrarUsuario = usuarioId != null;
        UUID usuarioFiltro = filtrarUsuario ? usuarioId : new UUID(0L, 0L);

        List<EventoAuditoria> eventos = eventoAuditoriaRepository.buscar(
                entidadFiltro, accionFiltro, filtrarUsuario, usuarioFiltro, PageRequest.of(0, MAXIMO_EXPORTACION));

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append(armarFila("Fecha", "Actor", "Acción", "Entidad", "Resultado", "Detalle"));
        for (EventoAuditoria evento : eventos) {
            csv.append(armarFila(
                    evento.getCreadoEn() == null ? "" : evento.getCreadoEn().atZoneSameInstant(ZONA_COLOMBIA).format(FORMATO_FECHA),
                    evento.getUsuarioId() == null ? "Sistema" : evento.getUsuarioId().toString(),
                    evento.getAccion(),
                    evento.getEntidad(),
                    evento.getResultado(),
                    describirDetalle(evento.getDetalle())));
        }

        auditService.registrar(admin.getId(), "auditoria", "EXPORTAR_AUDITORIA", "EXITOSO",
                Map.of("filas", String.valueOf(eventos.size())));

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String armarFila(String... celdas) {
        StringBuilder fila = new StringBuilder();
        for (int i = 0; i < celdas.length; i++) {
            if (i > 0) {
                fila.append(SEPARADOR);
            }
            fila.append(armarCelda(celdas[i]));
        }
        return fila.append("\r\n").toString();
    }

    private String armarCelda(String valor) {
        String texto = valor == null ? "" : valor;
        // Evita que Excel interprete un texto como si fuera una fórmula
        if (!texto.isEmpty() && "=+-@".indexOf(texto.charAt(0)) >= 0) {
            texto = "'" + texto;
        }
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private String describirDetalle(Map<String, Object> detalle) {
        if (detalle == null) {
            return "";
        }
        List<String> partes = new ArrayList<>();
        for (Map.Entry<String, Object> entrada : detalle.entrySet()) {
            partes.add(entrada.getKey() + ": " + entrada.getValue());
        }
        return String.join(" · ", partes);
    }

    private void verificarAdministrador(Usuario usuario) {
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción");
        }
    }
}