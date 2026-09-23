package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.ActualizarPublicacionRequest;
import co.edu.unbosque.casitago.dto.BloquearPublicacionRequest;
import co.edu.unbosque.casitago.dto.CrearPublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ListingService {

    private final PublicacionRepository publicacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditService auditService;
    private final ImagenStorageService imagenStorageService;

    public ListingService(
            PublicacionRepository publicacionRepository,
            UsuarioRepository usuarioRepository,
            AuditService auditService, ImagenStorageService imagenStorageService
    ) {
        this.publicacionRepository = publicacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditService = auditService;
        this.imagenStorageService = imagenStorageService;
    }

    // ---------- RF-08: crear publicación ----------
    @Transactional
    public PublicacionResponse crearPublicacion(UUID usuarioId, CrearPublicacionRequest request) {
        Usuario anfitrion = buscarUsuario(usuarioId);

        if (anfitrion.getRol() != RolUsuario.ANFITRION) {
            throw new RuntimeException("Solo un usuario con rol ANFITRIÓN puede crear publicaciones.");
        }

        Publicacion publicacion = new Publicacion();
        publicacion.setAnfitrion(anfitrion);
        aplicarDatos(publicacion, request.getTitulo(), request.getDescripcion(), request.getUbicacionTextual(),
                request.getTipo(), request.getCapacidad(), request.getPrecioNoche());
        reemplazarServicios(publicacion, request.getServicios());
        reemplazarReglas(publicacion, request.getReglas());

        publicacion = publicacionRepository.save(publicacion);

        auditService.registrar(anfitrion.getId(), "publicaciones", "CREAR_PUBLICACION", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString()));

        return PublicacionResponse.desde(publicacion);
    }
    // ---------- RF-08 (parte de imágenes): agregar imagen a una publicación ----------
    @Transactional
    public PublicacionResponse agregarImagen(UUID usuarioId, UUID publicacionId, org.springframework.web.multipart.MultipartFile archivo) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        verificarPropietario(usuario, publicacion);

        String url = imagenStorageService.subirImagen(publicacionId, archivo);

        ImagenPublicacion imagen = new ImagenPublicacion();
        imagen.setPublicacion(publicacion);
        imagen.setUrl(url);
        imagen.setOrden(publicacion.getImagenes().size());
        publicacion.getImagenes().add(imagen);

        auditService.registrar(usuario.getId(), "publicaciones", "AGREGAR_IMAGEN", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString()));

        return PublicacionResponse.desde(publicacion);
    }

    // ---------- RF-09: editar publicación (propietario o ADMINISTRADOR) ----------
    @Transactional
    public PublicacionResponse editarPublicacion(UUID usuarioId, UUID publicacionId, ActualizarPublicacionRequest request) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        verificarPropietarioOAdministrador(usuario, publicacion);

        aplicarDatos(publicacion, request.getTitulo(), request.getDescripcion(), request.getUbicacionTextual(),
                request.getTipo(), request.getCapacidad(), request.getPrecioNoche());
        reemplazarServicios(publicacion, request.getServicios());
        reemplazarReglas(publicacion, request.getReglas());

        auditService.registrar(usuario.getId(), "publicaciones", "EDITAR_PUBLICACION", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString()));

        return PublicacionResponse.desde(publicacion);
    }

    // ---------- RF-10: activar publicación ----------
    @Transactional
    public PublicacionResponse activarPublicacion(UUID usuarioId, UUID publicacionId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        verificarPropietario(usuario, publicacion);

        if (publicacion.getImagenes().isEmpty()) {
            throw new RuntimeException("La publicación necesita al menos una imagen antes de activarse.");
        }

        publicacion.setEstado(EstadoPublicacion.ACTIVA);

        auditService.registrar(usuario.getId(), "publicaciones", "ACTIVAR_PUBLICACION", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString()));

        return PublicacionResponse.desde(publicacion);
    }

    // ---------- RF-11: pausar publicación ----------
    @Transactional
    public PublicacionResponse pausarPublicacion(UUID usuarioId, UUID publicacionId) {
        Usuario usuario = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        verificarPropietario(usuario, publicacion);

        publicacion.setEstado(EstadoPublicacion.PAUSADA);

        auditService.registrar(usuario.getId(), "publicaciones", "PAUSAR_PUBLICACION", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString()));

        return PublicacionResponse.desde(publicacion);
    }

    // ---------- RF-12: consultar publicaciones de un ANFITRIÓN ----------
    public List<PublicacionResponse> consultarPublicacionesDeAnfitrion(UUID usuarioId) {
        Usuario anfitrion = buscarUsuario(usuarioId);

        return publicacionRepository.findByAnfitrion(anfitrion).stream()
                .map(PublicacionResponse::desde)
                .collect(Collectors.toList());
    }

    // ---------- RF-24: ADMINISTRADOR bloquea publicación ----------
    @Transactional
    public PublicacionResponse bloquearPublicacion(UUID usuarioId, UUID publicacionId, BloquearPublicacionRequest request) {
        Usuario administrador = buscarUsuario(usuarioId);
        Publicacion publicacion = buscarPublicacion(publicacionId);

        if (administrador.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("Solo un ADMINISTRADOR puede bloquear una publicación.");
        }

        // Las reservas futuras asociadas a esta publicación NO se tocan aquí:
        // solo cambia el estado de la publicación, por lo que quedan intactas
        // para auditoría, tal como exige el RF-24.
        publicacion.setEstado(EstadoPublicacion.BLOQUEADA);

        auditService.registrar(administrador.getId(), "publicaciones", "BLOQUEAR_PUBLICACION", "EXITOSO",
                Map.of("publicacionId", publicacion.getId().toString(), "motivo", request.getMotivo()));

        return PublicacionResponse.desde(publicacion);
    }

    // ---------- helpers privados ----------

    private void aplicarDatos(Publicacion publicacion, String titulo, String descripcion, String ubicacionTextual,
                              String tipo, Integer capacidad, java.math.BigDecimal precioNoche) {
        TipoAlojamiento tipoAlojamiento;
        try {
            tipoAlojamiento = TipoAlojamiento.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("El tipo de alojamiento indicado no es válido.");
        }

        publicacion.setTitulo(titulo);
        publicacion.setDescripcion(descripcion);
        publicacion.setUbicacionTextual(ubicacionTextual);
        publicacion.setTipo(tipoAlojamiento);
        publicacion.setCapacidad(capacidad);
        publicacion.setPrecioNoche(precioNoche);
        publicacion.setActualizadoEn(java.time.LocalDateTime.now());
    }

    private void reemplazarServicios(Publicacion publicacion, List<String> nombres) {
        publicacion.getServicios().clear();
        if (nombres == null) {
            return;
        }
        for (String nombre : nombres) {
            ServicioAlojamiento servicio = new ServicioAlojamiento();
            servicio.setPublicacion(publicacion);
            servicio.setNombre(nombre);
            publicacion.getServicios().add(servicio);
        }
    }

    private void reemplazarReglas(Publicacion publicacion, List<String> descripciones) {
        publicacion.getReglas().clear();
        if (descripciones == null) {
            return;
        }
        for (String descripcion : descripciones) {
            ReglaAlojamiento regla = new ReglaAlojamiento();
            regla.setPublicacion(publicacion);
            regla.setDescripcion(descripcion);
            publicacion.getReglas().add(regla);
        }
    }

    private void verificarPropietario(Usuario usuario, Publicacion publicacion) {
        if (!publicacion.getAnfitrion().getId().equals(usuario.getId())) {
            throw new RuntimeException("Solo el ANFITRIÓN propietario puede realizar esta acción.");
        }
    }

    private void verificarPropietarioOAdministrador(Usuario usuario, Publicacion publicacion) {
        boolean esPropietario = publicacion.getAnfitrion().getId().equals(usuario.getId());
        boolean esAdministrador = usuario.getRol() == RolUsuario.ADMINISTRADOR;

        if (!esPropietario && !esAdministrador) {
            throw new RuntimeException("No tienes permiso para editar esta publicación.");
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