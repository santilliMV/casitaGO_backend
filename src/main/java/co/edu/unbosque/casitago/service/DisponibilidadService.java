package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.BloqueoRequest;
import co.edu.unbosque.casitago.dto.DisponibilidadResponse;
import co.edu.unbosque.casitago.dto.PeriodoResponse;
import co.edu.unbosque.casitago.entity.EstadoPeriodo;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.PeriodoDisponibilidad;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DisponibilidadService {

    private static final String ENTIDAD_PERIODOS = "periodos_disponibilidad";
    private static final String RESULTADO_EXITOSO = "EXITOSO";
    private static final String PUBLICACION_ID = "publicacionId";

    private final PeriodoDisponibilidadRepository periodoRepository;
    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;

    public DisponibilidadService(
            PeriodoDisponibilidadRepository periodoRepository,
            PublicacionRepository publicacionRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService
    ) {
        this.periodoRepository = periodoRepository;
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
    }

    // ---------- RF-13: bloquear un rango de fechas ----------
    @Transactional
    public PeriodoResponse crearBloqueo(UUID usuarioId, UUID publicacionId, BloqueoRequest request) {
        validarRango(request.getFechaInicio(), request.getFechaFin());

        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);
        verificarPropietario(usuario, publicacion);

        List<PeriodoDisponibilidad> solapados = periodoRepository.buscarSolapados(
                publicacionId, request.getFechaInicio(), request.getFechaFin());
        if (!solapados.isEmpty()) {
            throw new RuntimeException("El rango se cruza con otro bloqueo o con una reserva existente.");
        }

        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setPublicacion(publicacion);
        periodo.setFechaInicio(request.getFechaInicio());
        periodo.setFechaFin(request.getFechaFin());
        periodo.setEstado(EstadoPeriodo.BLOQUEADO);
        periodo = periodoRepository.save(periodo);

        auditService.registrar(usuario.getId(), ENTIDAD_PERIODOS, "CREAR_BLOQUEO", RESULTADO_EXITOSO,
                Map.of(PUBLICACION_ID, publicacionId.toString()));

        return PeriodoResponse.desde(periodo);
    }

    // ---------- RF-13: consultar los periodos de una publicación ----------
    public List<PeriodoResponse> listarPeriodos(UUID usuarioId, UUID publicacionId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);
        verificarPropietario(usuario, publicacion);

        List<PeriodoResponse> respuesta = new ArrayList<>();
        for (PeriodoDisponibilidad periodo : periodoRepository.findByPublicacionIdOrderByFechaInicioAsc(publicacionId)) {
            respuesta.add(PeriodoResponse.desde(periodo));
        }
        return respuesta;
    }

    // ---------- RF-13: eliminar un bloqueo ----------
    @Transactional
    public void eliminarBloqueo(UUID usuarioId, UUID publicacionId, UUID periodoId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);
        verificarPropietario(usuario, publicacion);

        PeriodoDisponibilidad periodo = periodoRepository.findById(periodoId)
                .orElseThrow(() -> new RuntimeException("Periodo no encontrado."));

        if (!periodo.getPublicacion().getId().equals(publicacionId)) {
            throw new RuntimeException("El periodo no pertenece a esta publicación.");
        }
        if (periodo.getEstado() != EstadoPeriodo.BLOQUEADO) {
            throw new RuntimeException("Solo se pueden eliminar periodos en estado BLOQUEADO.");
        }

        periodoRepository.delete(periodo);

        auditService.registrar(usuario.getId(), ENTIDAD_PERIODOS, "ELIMINAR_BLOQUEO", RESULTADO_EXITOSO,
                Map.of(PUBLICACION_ID, publicacionId.toString()));
    }

    // ---------- RF-14: consultar disponibilidad para un rango de fechas ----------
    public DisponibilidadResponse consultarDisponibilidad(UUID publicacionId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Publicacion publicacion = buscarPublicacion(publicacionId);

        DisponibilidadResponse respuesta = new DisponibilidadResponse();
        respuesta.setPublicacionId(publicacionId);
        respuesta.setDesde(desde);
        respuesta.setHasta(hasta);

        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            respuesta.setDisponible(false);
            respuesta.setMotivo("La publicación no está activa.");
            return respuesta;
        }

        List<PeriodoDisponibilidad> solapados = periodoRepository.buscarSolapados(publicacionId, desde, hasta);
        if (!solapados.isEmpty()) {
            respuesta.setDisponible(false);
            respuesta.setMotivo("El alojamiento no está disponible en esas fechas.");
            return respuesta;
        }

        respuesta.setDisponible(true);
        return respuesta;
    }

    // ---------- helpers privados ----------

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio == null || fin == null) {
            throw new RuntimeException("Debes indicar la fecha de inicio y la fecha de fin.");
        }
        if (!fin.isAfter(inicio)) {
            throw new RuntimeException("La fecha de fin debe ser posterior a la fecha de inicio.");
        }
    }

    private void verificarPropietario(Usuario usuario, Publicacion publicacion) {
        if (!publicacion.getAnfitrion().getId().equals(usuario.getId())) {
            throw new RuntimeException("Solo el ANFITRIÓN propietario puede realizar esta acción.");
        }
    }

    private Usuario buscarUsuario(UUID usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));
    }

    private Publicacion buscarPublicacion(UUID publicacionId) {
        return publicacionRepository.findById(publicacionId)
                .orElseThrow(() -> new RuntimeException("Publicación no encontrada."));
    }
}