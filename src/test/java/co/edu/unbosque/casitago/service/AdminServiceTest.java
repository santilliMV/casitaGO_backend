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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RestriccionUsuarioRepository restriccionUsuarioRepository;

    @Mock
    private EventoAuditoriaRepository eventoAuditoriaRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminService adminService;

    private Usuario admin;
    private Usuario huesped;

    @BeforeEach
    void setUp() {
        admin = new Usuario("Admin", "admin@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private RestriccionRequest requestDePrueba() {
        RestriccionRequest request = new RestriccionRequest();
        request.setMotivo("Incumple las políticas de la plataforma");
        return request;
    }

    // ---------- RF-25: restringir usuario ----------

    @Test
    void restringirUsuario_datosValidos_deberiaDesactivarYRegistrar() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(restriccionUsuarioRepository.existsByUsuarioIdAndActivaTrue(huesped.getId())).thenReturn(false);
        when(restriccionUsuarioRepository.save(any(RestriccionUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RestriccionResponse response = adminService.restringirUsuario(huesped.getId(), admin, requestDePrueba());

        assertTrue(response.isActiva());
        assertEquals(huesped.getId(), response.getUsuarioId());
        assertEquals(admin.getId(), response.getCreadoPorId());
        assertEquals("Incumple las políticas de la plataforma", response.getMotivo());
        assertFalse(huesped.isEnabled());
        verify(auditService).registrar(eq(admin.getId()), eq("usuarios"), eq("RESTRINGIR_USUARIO"), eq("EXITOSO"), any());
    }

    @Test
    void restringirUsuario_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.restringirUsuario(UUID.randomUUID(), huesped, requestDePrueba()));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verify(restriccionUsuarioRepository, never()).save(any());
    }

    @Test
    void restringirUsuario_asiMismo_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.restringirUsuario(admin.getId(), admin, requestDePrueba()));

        assertEquals("No puedes restringir tu propia cuenta", ex.getMessage());
    }

    @Test
    void restringirUsuario_usuarioInexistente_deberiaLanzarExcepcion() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.restringirUsuario(usuarioId, admin, requestDePrueba()));

        assertEquals("El usuario no existe", ex.getMessage());
    }

    @Test
    void restringirUsuario_otroAdministrador_deberiaLanzarExcepcion() {
        Usuario otroAdmin = new Usuario("Otro Admin", "otro@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(otroAdmin, "id", UUID.randomUUID());
        when(usuarioRepository.findById(otroAdmin.getId())).thenReturn(Optional.of(otroAdmin));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.restringirUsuario(otroAdmin.getId(), admin, requestDePrueba()));

        assertEquals("No se puede restringir a otro administrador", ex.getMessage());
        assertTrue(otroAdmin.isEnabled());
    }

    @Test
    void restringirUsuario_yaRestringido_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(huesped.getId())).thenReturn(Optional.of(huesped));
        when(restriccionUsuarioRepository.existsByUsuarioIdAndActivaTrue(huesped.getId())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.restringirUsuario(huesped.getId(), admin, requestDePrueba()));

        assertEquals("El usuario ya tiene una restricción activa", ex.getMessage());
        verify(restriccionUsuarioRepository, never()).save(any());
    }

    // ---------- RF-25: levantar restricción ----------

    @Test
    void levantarRestriccion_restriccionActiva_deberiaReactivarUsuario() {
        huesped.setActivo(false);
        RestriccionUsuario restriccion = new RestriccionUsuario();
        restriccion.setUsuario(huesped);
        restriccion.setCreadoPor(admin);
        restriccion.setMotivo("Motivo");
        when(restriccionUsuarioRepository.findByUsuarioIdAndActivaTrue(huesped.getId()))
                .thenReturn(Optional.of(restriccion));
        when(restriccionUsuarioRepository.save(any(RestriccionUsuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RestriccionResponse response = adminService.levantarRestriccion(huesped.getId(), admin);

        assertFalse(response.isActiva());
        assertTrue(huesped.isEnabled());
        verify(auditService).registrar(eq(admin.getId()), eq("usuarios"), eq("LEVANTAR_RESTRICCION"), eq("EXITOSO"), any());
    }

    @Test
    void levantarRestriccion_sinRestriccionActiva_deberiaLanzarExcepcion() {
        when(restriccionUsuarioRepository.findByUsuarioIdAndActivaTrue(huesped.getId()))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.levantarRestriccion(huesped.getId(), admin));

        assertEquals("El usuario no tiene una restricción activa", ex.getMessage());
    }

    @Test
    void levantarRestriccion_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.levantarRestriccion(huesped.getId(), huesped));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
    }

    // ---------- RF-26: consultar auditoría ----------

    @Test
    void consultarAuditoria_sinFiltros_deberiaUsarValoresNeutros() {
        EventoAuditoria evento = new EventoAuditoria(admin.getId(), "usuarios", "LOGIN", "EXITOSO", Map.of());
        when(eventoAuditoriaRepository.buscar(eq(""), eq(""), eq(false), eq(new UUID(0L, 0L)), any(Pageable.class)))
                .thenReturn(List.of(evento));

        List<EventoAuditoriaResponse> resultado = adminService.consultarAuditoria(admin, null, null, null);

        assertEquals(1, resultado.size());
        assertEquals("LOGIN", resultado.get(0).getAccion());
        assertEquals("usuarios", resultado.get(0).getEntidad());
    }

    @Test
    void consultarAuditoria_conFiltros_deberiaPasarlosAlRepository() {
        UUID usuarioId = UUID.randomUUID();
        when(eventoAuditoriaRepository.buscar(eq("usuarios"), eq("LOGIN"), eq(true), eq(usuarioId), any(Pageable.class)))
                .thenReturn(List.of());

        List<EventoAuditoriaResponse> resultado = adminService.consultarAuditoria(admin, "usuarios", "LOGIN", usuarioId);

        assertTrue(resultado.isEmpty());
        verify(eventoAuditoriaRepository).buscar(eq("usuarios"), eq("LOGIN"), eq(true), eq(usuarioId), any(Pageable.class));
    }

    @Test
    void consultarAuditoria_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.consultarAuditoria(huesped, null, null, null));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verify(eventoAuditoriaRepository, never()).buscar(any(), any(), anyBoolean(), any(), any());
    }
}