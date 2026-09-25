package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.BloqueoRequest;
import co.edu.unbosque.casitago.dto.DisponibilidadResponse;
import co.edu.unbosque.casitago.dto.PeriodoResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisponibilidadServiceTest {

    @Mock
    private PeriodoDisponibilidadRepository periodoRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private Usuario anfitrion;
    private Usuario otroAnfitrion;

    private final LocalDate inicio = LocalDate.of(2026, 12, 1);
    private final LocalDate fin = LocalDate.of(2026, 12, 5);

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        otroAnfitrion = new Usuario("Otro Anfitrion", "otro@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(otroAnfitrion, "id", UUID.randomUUID());
    }

    private BloqueoRequest bloqueoValido() {
        BloqueoRequest request = new BloqueoRequest();
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        return request;
    }

    // ---------- RF-13: crear bloqueo ----------

    @Test
    void crearBloqueo_comoPropietario_deberiaCrearloEnEstadoBloqueado() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), inicio, fin)).thenReturn(List.of());
        when(periodoRepository.save(any(PeriodoDisponibilidad.class))).thenAnswer(inv -> {
            PeriodoDisponibilidad periodo = inv.getArgument(0);
            periodo.setId(UUID.randomUUID());
            return periodo;
        });

        PeriodoResponse response = disponibilidadService.crearBloqueo(anfitrion.getId(), publicacion.getId(), bloqueoValido());

        assertEquals("BLOQUEADO", response.getEstado());
        assertEquals(inicio, response.getFechaInicio());
        assertEquals(fin, response.getFechaFin());
        verify(auditService).registrar(eq(anfitrion.getId()), eq("periodos_disponibilidad"), eq("CREAR_BLOQUEO"), eq("EXITOSO"), any());
    }

    @Test
    void crearBloqueo_conFechaFinAnteriorAInicio_deberiaLanzarExcepcion() {
        BloqueoRequest request = new BloqueoRequest();
        request.setFechaInicio(fin);
        request.setFechaFin(inicio);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrion.getId(), UUID.randomUUID(), request));

        assertEquals("La fecha de fin debe ser posterior a la fecha de inicio.", ex.getMessage());
        verify(periodoRepository, never()).save(any());
    }

    @Test
    void crearBloqueo_conFechaFinIgualAInicio_deberiaLanzarExcepcion() {
        BloqueoRequest request = new BloqueoRequest();
        request.setFechaInicio(inicio);
        request.setFechaFin(inicio);

        assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrion.getId(), UUID.randomUUID(), request));
    }

    @Test
    void crearBloqueo_sinFechas_deberiaLanzarExcepcion() {
        BloqueoRequest request = new BloqueoRequest();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrion.getId(), UUID.randomUUID(), request));

        assertEquals("Debes indicar la fecha de inicio y la fecha de fin.", ex.getMessage());
    }

    @Test
    void crearBloqueo_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(otroAnfitrion.getId(), publicacion.getId(), bloqueoValido()));

        assertEquals("Solo el ANFITRIÓN propietario puede realizar esta acción.", ex.getMessage());
        verify(periodoRepository, never()).save(any());
    }

    @Test
    void crearBloqueo_conRangoSolapado_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), inicio, fin))
                .thenReturn(List.of(periodoExistente(publicacion, EstadoPeriodo.BLOQUEADO)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrion.getId(), publicacion.getId(), bloqueoValido()));

        assertEquals("El rango se cruza con otro bloqueo o con una reserva existente.", ex.getMessage());
        verify(periodoRepository, never()).save(any());
    }

    @Test
    void crearBloqueo_conUsuarioInexistente_deberiaLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(usuarioId, UUID.randomUUID(), bloqueoValido()));

        assertEquals("Usuario no encontrado.", ex.getMessage());
    }

    @Test
    void crearBloqueo_conPublicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrion.getId(), publicacionId, bloqueoValido()));

        assertEquals("Publicación no encontrada.", ex.getMessage());
    }

    // ---------- RF-13: listar periodos ----------

    @Test
    void listarPeriodos_comoPropietario_deberiaRetornarLista() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.findByPublicacionIdOrderByFechaInicioAsc(publicacion.getId()))
                .thenReturn(List.of(periodoExistente(publicacion, EstadoPeriodo.BLOQUEADO)));

        List<PeriodoResponse> resultado = disponibilidadService.listarPeriodos(anfitrion.getId(), publicacion.getId());

        assertEquals(1, resultado.size());
        assertEquals("BLOQUEADO", resultado.get(0).getEstado());
    }

    @Test
    void listarPeriodos_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        assertThrows(RuntimeException.class,
                () -> disponibilidadService.listarPeriodos(otroAnfitrion.getId(), publicacion.getId()));
    }

    // ---------- RF-13: eliminar bloqueo ----------

    @Test
    void eliminarBloqueo_bloqueadoDelPropietario_deberiaEliminarlo() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        PeriodoDisponibilidad periodo = periodoExistente(publicacion, EstadoPeriodo.BLOQUEADO);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.findById(periodo.getId())).thenReturn(Optional.of(periodo));

        disponibilidadService.eliminarBloqueo(anfitrion.getId(), publicacion.getId(), periodo.getId());

        verify(periodoRepository).delete(periodo);
        verify(auditService).registrar(eq(anfitrion.getId()), eq("periodos_disponibilidad"), eq("ELIMINAR_BLOQUEO"), eq("EXITOSO"), any());
    }

    @Test
    void eliminarBloqueo_periodoInexistente_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        UUID periodoId = UUID.randomUUID();
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.findById(periodoId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.eliminarBloqueo(anfitrion.getId(), publicacion.getId(), periodoId));

        assertEquals("Periodo no encontrado.", ex.getMessage());
    }

    @Test
    void eliminarBloqueo_periodoDeOtraPublicacion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        Publicacion otraPublicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        PeriodoDisponibilidad periodo = periodoExistente(otraPublicacion, EstadoPeriodo.BLOQUEADO);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.findById(periodo.getId())).thenReturn(Optional.of(periodo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.eliminarBloqueo(anfitrion.getId(), publicacion.getId(), periodo.getId()));

        assertEquals("El periodo no pertenece a esta publicación.", ex.getMessage());
        verify(periodoRepository, never()).delete(any());
    }

    @Test
    void eliminarBloqueo_periodoReservado_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        PeriodoDisponibilidad periodo = periodoExistente(publicacion, EstadoPeriodo.RESERVADO);
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.findById(periodo.getId())).thenReturn(Optional.of(periodo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.eliminarBloqueo(anfitrion.getId(), publicacion.getId(), periodo.getId()));

        assertEquals("Solo se pueden eliminar periodos en estado BLOQUEADO.", ex.getMessage());
        verify(periodoRepository, never()).delete(any());
    }

    @Test
    void eliminarBloqueo_comoOtroAnfitrion_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(usuarioRepository.findById(otroAnfitrion.getId())).thenReturn(Optional.of(otroAnfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        assertThrows(RuntimeException.class,
                () -> disponibilidadService.eliminarBloqueo(otroAnfitrion.getId(), publicacion.getId(), UUID.randomUUID()));
    }

    // ---------- RF-14: consultar disponibilidad ----------

    @Test
    void consultarDisponibilidad_publicacionActivaSinCruces_deberiaEstarDisponible() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), inicio, fin)).thenReturn(List.of());

        DisponibilidadResponse response = disponibilidadService.consultarDisponibilidad(publicacion.getId(), inicio, fin);

        assertTrue(response.isDisponible());
        assertNull(response.getMotivo());
        assertEquals(inicio, response.getDesde());
        assertEquals(fin, response.getHasta());
    }

    @Test
    void consultarDisponibilidad_publicacionNoActiva_deberiaNoEstarDisponible() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.PAUSADA);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        DisponibilidadResponse response = disponibilidadService.consultarDisponibilidad(publicacion.getId(), inicio, fin);

        assertFalse(response.isDisponible());
        assertEquals("La publicación no está activa.", response.getMotivo());
        verify(periodoRepository, never()).buscarSolapados(any(), any(), any());
    }

    @Test
    void consultarDisponibilidad_conPeriodoSolapado_deberiaNoEstarDisponible() {
        Publicacion publicacion = publicacionExistente(anfitrion, EstadoPublicacion.ACTIVA);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), inicio, fin))
                .thenReturn(List.of(periodoExistente(publicacion, EstadoPeriodo.RESERVADO)));

        DisponibilidadResponse response = disponibilidadService.consultarDisponibilidad(publicacion.getId(), inicio, fin);

        assertFalse(response.isDisponible());
        assertEquals("El alojamiento no está disponible en esas fechas.", response.getMotivo());
    }

    @Test
    void consultarDisponibilidad_conRangoInvalido_deberiaLanzarExcepcion() {
        assertThrows(RuntimeException.class,
                () -> disponibilidadService.consultarDisponibilidad(UUID.randomUUID(), fin, inicio));
    }

    @Test
    void consultarDisponibilidad_conPublicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> disponibilidadService.consultarDisponibilidad(publicacionId, inicio, fin));
    }

    // ---------- helpers ----------

    private Publicacion publicacionExistente(Usuario propietario, EstadoPublicacion estado) {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(propietario);
        publicacion.setTitulo("Publicación existente");
        publicacion.setDescripcion("Descripción existente");
        publicacion.setUbicacionTextual("Ubicación existente");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(estado);
        return publicacion;
    }

    private PeriodoDisponibilidad periodoExistente(Publicacion publicacion, EstadoPeriodo estado) {
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setId(UUID.randomUUID());
        periodo.setPublicacion(publicacion);
        periodo.setFechaInicio(inicio);
        periodo.setFechaFin(fin);
        periodo.setEstado(estado);
        return periodo;
    }
}