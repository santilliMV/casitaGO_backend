package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.entity.EventoAuditoria;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminExportarAuditoriaTest {

    private static final String ENCABEZADO =
            "\uFEFF\"Fecha\";\"Actor\";\"Acción\";\"Entidad\";\"Resultado\";\"Detalle\"";

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

        huesped = new Usuario("Luis Huesped", "luis@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private EventoAuditoria eventoDe(OffsetDateTime fecha, UUID usuarioId, String accion, Map<String, Object> detalle) {
        EventoAuditoria evento = mock(EventoAuditoria.class);
        when(evento.getCreadoEn()).thenReturn(fecha);
        when(evento.getUsuarioId()).thenReturn(usuarioId);
        when(evento.getAccion()).thenReturn(accion);
        when(evento.getEntidad()).thenReturn("usuarios");
        when(evento.getResultado()).thenReturn("EXITOSO");
        when(evento.getDetalle()).thenReturn(detalle);
        return evento;
    }

    @Test
    void exportarAuditoriaCsv_administrador_deberiaArmarEncabezadoYFilas() {
        UUID actorId = UUID.randomUUID();
        EventoAuditoria completo = eventoDe(OffsetDateTime.parse("2026-10-11T14:42:00-05:00"),
                actorId, "LOGIN", Map.of("motivo", "abuso"));
        EventoAuditoria sinDatos = eventoDe(null, null, "CREAR", null);
        when(eventoAuditoriaRepository.buscar(eq(""), eq(""), eq(false), any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(completo, sinDatos));

        byte[] archivo = adminService.exportarAuditoriaCsv(admin, null, null, null);
        String[] lineas = new String(archivo, StandardCharsets.UTF_8).split("\r\n");

        assertEquals(3, lineas.length);
        assertEquals(ENCABEZADO, lineas[0]);
        assertEquals("\"2026-10-11 14:42\";\"" + actorId
                + "\";\"LOGIN\";\"usuarios\";\"EXITOSO\";\"motivo: abuso\"", lineas[1]);
        assertEquals("\"\";\"Sistema\";\"CREAR\";\"usuarios\";\"EXITOSO\";\"\"", lineas[2]);
        verify(auditService).registrar(eq(admin.getId()), eq("auditoria"), eq("EXPORTAR_AUDITORIA"),
                eq("EXITOSO"), eq(Map.<String, Object>of("filas", "2")));
    }

    @Test
    void exportarAuditoriaCsv_conFormulaOComillas_deberiaProtegerElTexto() {
        EventoAuditoria peligroso = eventoDe(OffsetDateTime.parse("2026-10-11T09:05:00-05:00"),
                null, "=CMD()", Map.of("motivo", "dijo \"hola\""));
        when(eventoAuditoriaRepository.buscar(eq(""), eq(""), eq(false), any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(peligroso));

        byte[] archivo = adminService.exportarAuditoriaCsv(admin, null, null, null);
        String[] lineas = new String(archivo, StandardCharsets.UTF_8).split("\r\n");

        assertEquals("\"2026-10-11 09:05\";\"Sistema\";\"'=CMD()\";\"usuarios\";\"EXITOSO\";"
                + "\"motivo: dijo \"\"hola\"\"\"", lineas[1]);
    }

    @Test
    void exportarAuditoriaCsv_conFiltros_deberiaConsultarConLosFiltrosIndicados() {
        UUID actorId = UUID.randomUUID();
        when(eventoAuditoriaRepository.buscar("usuarios", "LOGIN", true, actorId, PageRequest.of(0, 5000)))
                .thenReturn(List.of());

        byte[] archivo = adminService.exportarAuditoriaCsv(admin, " usuarios ", " LOGIN ", actorId);
        String[] lineas = new String(archivo, StandardCharsets.UTF_8).split("\r\n");

        assertEquals(1, lineas.length);
        assertEquals(ENCABEZADO, lineas[0]);
        verify(auditService).registrar(eq(admin.getId()), eq("auditoria"), eq("EXPORTAR_AUDITORIA"),
                eq("EXITOSO"), eq(Map.<String, Object>of("filas", "0")));
    }

    @Test
    void exportarAuditoriaCsv_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adminService.exportarAuditoriaCsv(huesped, null, null, null));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verifyNoInteractions(eventoAuditoriaRepository, auditService);
    }
    @Test
    void exportarAuditoriaCsv_fechaEnUtc_deberiaMostrarseEnHoraDeColombia() {
        EventoAuditoria enUtc = eventoDe(OffsetDateTime.parse("2026-10-12T02:30:00Z"),
                null, "LOGIN", null);
        when(eventoAuditoriaRepository.buscar(eq(""), eq(""), eq(false), any(UUID.class), any(Pageable.class)))
                .thenReturn(List.of(enUtc));

        byte[] archivo = adminService.exportarAuditoriaCsv(admin, null, null, null);
        String[] lineas = new String(archivo, StandardCharsets.UTF_8).split("\r\n");

        assertEquals("\"2026-10-11 21:30\";\"Sistema\";\"LOGIN\";\"usuarios\";\"EXITOSO\";\"\"", lineas[1]);
    }
}