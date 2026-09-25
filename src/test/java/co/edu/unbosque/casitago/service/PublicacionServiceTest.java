package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.BloquearPublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicacionServiceTest {

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
    private Usuario otroAnfitrion;
    private Usuario administrador;
    private Usuario huesped;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        otroAnfitrion = new Usuario("Otro Anfitrion", "otro@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(otroAnfitrion, "id", UUID.randomUUID());

        administrador = new Usuario("Admin", "admin@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(administrador, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private PublicacionRequest requestValido() {
        PublicacionRequest request = new PublicacionRequest();
        request.setTitulo("Apartamento con vista al mar");
        request.setDescripcion("Cómodo y luminoso");
        request.setUbicacionTextual("Cartagena, Bolívar");
        request.setCiudad("Cartagena");
        request.setTipo("APARTAMENTO");
        request.setCapacidad(4);
        request.setPrecioNoche(new BigDecimal("250000"));
        request.setServicios(List.of("WiFi", "Piscina"));
        request.setReglas(List.of("No fiestas"));
        return request;
    }

    // ---------- RF-08: crear publicación ----------

    @Test
    void crearPublicacion_conAnfitrionValido_deberiaCrearEnBorrador() {
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> {
            Publicacion publicacion = inv.getArgument(0);
            if (publicacion.getId() == null) {
                publicacion.setId(UUID.randomUUID());
            }
            return publicacion;
        });

        PublicacionResponse response = publicacionService.crearPublicacion(anfitrion.getId(), requestValido());

        assertEquals("BORRADOR", response.getEstado());
        assertEquals("Apartamento con vista al mar", response.getTitulo());
        assertEquals(2, response.getServicios().size());
        verify(auditService).registrar(eq(anfitrion.getId()), eq("publicaciones"), eq("CREAR_PUBLICACION"), eq("EXITOSO"), any());
    }

    @Test
    void crearPublicacion_conHuesped_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publicacionService.crearPublicacion(huesped.getId(), requestValido()));

        assertEquals("Solo un usuario con rol ANFITRIÓN puede crear publicaciones.", ex.getMessage());
        verify(publicacionRepository, never()).save(any());
    }

    @Test
    void crearPublicacion_conTipoInvalido_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        PublicacionRequest request = requestValido();
        request.setTipo("CASTILLO");

        assertThrows(RuntimeException.class, () -> publicacionService.crearPublicacion(anfitrion.getId(), request));
    }

    @Test
    void crearPublicacion_sinServiciosNiReglas_deberiaCrearIgual() {
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> {
            Publicacion publicacion = inv.getArgument(0);
            if (publicacion.getId() == null) {
                publicacion.setId(UUID.randomUUID());
            }
            return publicacion;
        });

        PublicacionRequest request = requestValido();
        request.setServicios(null);
        request.setReglas(null);

        PublicacionResponse response = publicacionService.crearPublicacion(anfitrion.getId(), request);

        assertEquals(0, response.getServicios().size());
        assertEquals(0, response.getReglas().size());
    }

    // ---------- RF-09: editar publicación ----------

    @Test
    void editarPublicacion_comoPropietario_deberiaActualizar() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionRequest request = requestValido();
        request.setTitulo("Título actualizado");
        request.setTipo("CASA");
        request.setCapacidad(6);
        request.setPrecioNoche(new BigDecimal("300000"));

        PublicacionResponse response = publicacionService.editarPublicacion(anfitrion.getId(), publicacion.getId(), request);

        assertEquals("Título actualizado", response.getTitulo());
        assertEquals("CASA", response.getTipo());
    }

    @Test
    void editarPublicacion_comoAdministrador_deberiaActualizar() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionRequest request = requestValido();
        request.setTitulo("Editado por admin");

        PublicacionResponse response = publicacionService.editarPublicacion(administrador.getId(), publicacion.getId(), request);

        assertEquals("Editado por admin", response.getTitulo());
    }

    @Test
    void editarPublicacion_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        assertThrows(RuntimeException.class,
                () -> publicacionService.editarPublicacion(otroAnfitrion.getId(), publicacion.getId(), requestValido()));
    }

    // ---------- RF-10: activar publicación ----------

    @Test
    void activarPublicacion_sinImagenes_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> publicacionService.activarPublicacion(anfitrion.getId(), publicacion.getId()));

        assertEquals("La publicación necesita al menos una imagen antes de activarse.", ex.getMessage());
    }

    @Test
    void activarPublicacion_conImagenes_deberiaActivar() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        ImagenPublicacion imagen = new ImagenPublicacion();
        imagen.setUrl("https://ejemplo.com/foto.jpg");
        publicacion.getImagenes().add(imagen);

        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = publicacionService.activarPublicacion(anfitrion.getId(), publicacion.getId());

        assertEquals("ACTIVA", response.getEstado());
    }

    // ---------- RF-11: pausar publicación ----------

    @Test
    void pausarPublicacion_comoPropietario_deberiaPausar() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        PublicacionResponse response = publicacionService.pausarPublicacion(anfitrion.getId(), publicacion.getId());

        assertEquals("PAUSADA", response.getEstado());
    }

    @Test
    void pausarPublicacion_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        assertThrows(RuntimeException.class,
                () -> publicacionService.pausarPublicacion(otroAnfitrion.getId(), publicacion.getId()));
    }

    // ---------- RF-12: consultar publicaciones de un ANFITRIÓN ----------

    @Test
    void consultarPublicacionesDeAnfitrion_deberiaRetornarLista() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findByAnfitrion(anfitrion)).thenReturn(List.of(publicacion));

        List<PublicacionResponse> resultado = publicacionService.consultarPublicacionesDeAnfitrion(anfitrion.getId());

        assertEquals(1, resultado.size());
    }

    // ---------- RF-24: bloquear publicación ----------

    @Test
    void bloquearPublicacion_comoAdministrador_deberiaBloquear() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(administrador.getId())).thenReturn(Optional.of(administrador));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        BloquearPublicacionRequest request = new BloquearPublicacionRequest();
        request.setMotivo("Información engañosa sobre la ubicación");

        PublicacionResponse response = publicacionService.bloquearPublicacion(administrador.getId(), publicacion.getId(), request);

        assertEquals("BLOQUEADA", response.getEstado());
        verify(auditService).registrar(eq(administrador.getId()), eq("publicaciones"), eq("BLOQUEAR_PUBLICACION"), eq("EXITOSO"), any());
    }

    @Test
    void bloquearPublicacion_comoAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        BloquearPublicacionRequest request = new BloquearPublicacionRequest();
        request.setMotivo("Cualquier motivo");

        assertThrows(RuntimeException.class,
                () -> publicacionService.bloquearPublicacion(anfitrion.getId(), publicacion.getId(), request));
    }

    // ---------- RF-08 (imágenes) ----------

    @Test
    void agregarImagen_propietario_deberiaAgregarla() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(imagenStorageService.subirImagen(any(), any())).thenReturn("https://ejemplo.com/foto.jpg");

        MultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "x".getBytes());

        PublicacionResponse response = publicacionService.agregarImagen(anfitrion.getId(), publicacion.getId(), archivo);

        assertEquals(1, response.getImagenes().size());
    }

    @Test
    void agregarImagen_noPropietario_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        MultipartFile archivo = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "x".getBytes());

        assertThrows(RuntimeException.class,
                () -> publicacionService.agregarImagen(otroAnfitrion.getId(), publicacion.getId(), archivo));
    }

    // ---------- helper ----------

    private Publicacion publicacionExistente(Usuario propietario) {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(propietario);
        publicacion.setTitulo("Publicación existente");
        publicacion.setDescripcion("Descripción existente");
        publicacion.setUbicacionTextual("Ubicación existente");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.BORRADOR);
        return publicacion;
    }
}