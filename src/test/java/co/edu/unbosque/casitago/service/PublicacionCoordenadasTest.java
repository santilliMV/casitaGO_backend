package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.PublicacionRequest;
import co.edu.unbosque.casitago.dto.PublicacionResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicacionCoordenadasTest {

    private static final String DIRECCION = "Cartagena, Bolívar";
    private static final String MENSAJE_JUNTAS = "Debes indicar la latitud y la longitud juntas.";
    private static final String MENSAJE_LATITUD = "La latitud debe estar entre -90 y 90.";
    private static final String MENSAJE_LONGITUD = "La longitud debe estar entre -180 y 180.";

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
    private Publicacion existente;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));

        existente = new Publicacion();
        existente.setId(UUID.randomUUID());
        existente.setAnfitrion(anfitrion);
        existente.setTipo(TipoAlojamiento.APARTAMENTO);
        existente.setEstado(EstadoPublicacion.ACTIVA);
        existente.setUbicacionTextual(DIRECCION);
        existente.setLatitud(10.4);
        existente.setLongitud(-75.5);
    }

    private PublicacionRequest request(Double latitud, Double longitud, String ubicacion) {
        PublicacionRequest request = new PublicacionRequest();
        request.setTitulo("Apartamento con vista al mar");
        request.setDescripcion("Cómodo y luminoso");
        request.setUbicacionTextual(ubicacion);
        request.setCiudad("Cartagena");
        request.setTipo("APARTAMENTO");
        request.setCapacidad(4);
        request.setPrecioNoche(new BigDecimal("250000"));
        request.setLatitud(latitud);
        request.setLongitud(longitud);
        return request;
    }

    private void guardarDevolviendoLoRecibido() {
        when(publicacionRepository.save(any(Publicacion.class))).thenAnswer(inv -> {
            Publicacion publicacion = inv.getArgument(0);
            publicacion.setId(UUID.randomUUID());
            return publicacion;
        });
    }

    private void publicacionExistente() {
        when(publicacionRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
    }

    // ---------- Crear ----------

    @Test
    void crearPublicacion_conCoordenadasValidas_deberiaGuardarlasYDevolverlas() {
        guardarDevolviendoLoRecibido();

        PublicacionResponse response = publicacionService.crearPublicacion(anfitrion.getId(), request(10.4, -75.5, DIRECCION));

        assertEquals(10.4, response.getLatitud());
        assertEquals(-75.5, response.getLongitud());
    }

    @Test
    void crearPublicacion_sinCoordenadas_deberiaDejarlasVacias() {
        guardarDevolviendoLoRecibido();

        PublicacionResponse response = publicacionService.crearPublicacion(anfitrion.getId(), request(null, null, DIRECCION));

        assertNull(response.getLatitud());
        assertNull(response.getLongitud());
    }

    @Test
    void crearPublicacion_enLosLimitesExactos_deberiaAceptarlas() {
        guardarDevolviendoLoRecibido();

        PublicacionResponse response = publicacionService.crearPublicacion(anfitrion.getId(), request(90.0, -180.0, DIRECCION));

        assertEquals(90.0, response.getLatitud());
        assertEquals(-180.0, response.getLongitud());
    }

    @Test
    void crearPublicacion_soloConLatitud_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(10.4, null, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_JUNTAS, ex.getMessage());
        verify(publicacionRepository, never()).save(any(Publicacion.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void crearPublicacion_soloConLongitud_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(null, -75.5, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_JUNTAS, ex.getMessage());
    }

    @Test
    void crearPublicacion_conLatitudFueraDeRango_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(90.1, -75.5, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_LATITUD, ex.getMessage());
    }

    @Test
    void crearPublicacion_conLatitudMuyBaja_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(-90.1, -75.5, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_LATITUD, ex.getMessage());
    }

    @Test
    void crearPublicacion_conLongitudFueraDeRango_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(10.4, 180.1, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_LONGITUD, ex.getMessage());
    }

    @Test
    void crearPublicacion_conLongitudMuyBaja_deberiaLanzarExcepcion() {
        UUID usuarioId = anfitrion.getId();
        PublicacionRequest request = request(10.4, -180.1, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.crearPublicacion(usuarioId, request));

        assertEquals(MENSAJE_LONGITUD, ex.getMessage());
    }

    // ---------- Editar ----------

    @Test
    void editarPublicacion_conCoordenadasNuevas_deberiaReemplazarlas() {
        publicacionExistente();

        publicacionService.editarPublicacion(anfitrion.getId(), existente.getId(), request(6.2, -75.6, DIRECCION));

        assertEquals(6.2, existente.getLatitud());
        assertEquals(-75.6, existente.getLongitud());
    }

    @Test
    void editarPublicacion_sinCoordenadasYMismaDireccion_deberiaConservarLasActuales() {
        publicacionExistente();

        publicacionService.editarPublicacion(anfitrion.getId(), existente.getId(), request(null, null, DIRECCION));

        assertEquals(10.4, existente.getLatitud());
        assertEquals(-75.5, existente.getLongitud());
    }

    @Test
    void editarPublicacion_sinCoordenadasYConOtraDireccion_deberiaBorrarlasParaRecalcular() {
        publicacionExistente();

        publicacionService.editarPublicacion(anfitrion.getId(), existente.getId(), request(null, null, "Medellín, Antioquia"));

        assertNull(existente.getLatitud());
        assertNull(existente.getLongitud());
        assertEquals("Medellín, Antioquia", existente.getUbicacionTextual());
    }

    @Test
    void editarPublicacion_conCoordenadasYOtraDireccion_deberiaUsarLasNuevas() {
        publicacionExistente();

        publicacionService.editarPublicacion(anfitrion.getId(), existente.getId(), request(6.2, -75.6, "Medellín, Antioquia"));

        assertEquals(6.2, existente.getLatitud());
        assertEquals(-75.6, existente.getLongitud());
    }

    @Test
    void editarPublicacion_soloConLatitud_deberiaLanzarExcepcionYConservarLasActuales() {
        publicacionExistente();
        UUID usuarioId = anfitrion.getId();
        UUID publicacionId = existente.getId();
        PublicacionRequest request = request(6.2, null, DIRECCION);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> publicacionService.editarPublicacion(usuarioId, publicacionId, request));

        assertEquals(MENSAJE_JUNTAS, ex.getMessage());
        assertEquals(10.4, existente.getLatitud());
        assertEquals(-75.5, existente.getLongitud());
        verifyNoInteractions(auditService);
    }
}