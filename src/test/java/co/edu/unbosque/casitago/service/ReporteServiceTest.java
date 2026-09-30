package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.DashboardResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Usuario admin;
    private Usuario anfitrion;

    @BeforeEach
    void setUp() {
        admin = new Usuario("Admin", "admin@correo.com", "hash", RolUsuario.ADMINISTRADOR);
        ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());

        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());
    }

    // ---------- RF-27: dashboard ----------

    @Test
    void obtenerDashboard_administrador_deberiaArmarTodosLosIndicadores() {
        when(usuarioRepository.count()).thenReturn(10L);
        when(usuarioRepository.countByRol(RolUsuario.HUESPED)).thenReturn(6L);
        when(usuarioRepository.countByRol(RolUsuario.ANFITRION)).thenReturn(3L);
        when(usuarioRepository.countByRol(RolUsuario.ADMINISTRADOR)).thenReturn(1L);
        when(publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA)).thenReturn(4L);
        when(reservaRepository.countByEstado(any(EstadoReserva.class))).thenReturn(2L);
        when(reservaRepository.sumarIngresos(anyList())).thenReturn(new BigDecimal("630050"));

        List<Object[]> filas = List.of(new Object[]{"Bogota", 3L}, new Object[]{"", 1L});
        when(reservaRepository.ciudadesConMasReservas(eq(EstadoReserva.CANCELADA), any(Pageable.class)))
                .thenReturn(filas);

        DashboardResponse dashboard = reporteService.obtenerDashboard(admin);

        assertEquals(10L, dashboard.getTotalUsuarios());
        assertEquals(6L, dashboard.getUsuariosHuespedes());
        assertEquals(3L, dashboard.getUsuariosAnfitriones());
        assertEquals(1L, dashboard.getUsuariosAdministradores());
        assertEquals(4L, dashboard.getPublicacionesActivas());

        assertEquals(4, dashboard.getReservasPorEstado().size());
        assertEquals(2L, dashboard.getReservasPorEstado().get("CONFIRMADA"));
        assertEquals(2L, dashboard.getReservasPorEstado().get("CANCELADA"));

        assertEquals(0, new BigDecimal("630050.00").compareTo(dashboard.getIngresosEstimados()));

        assertEquals(2, dashboard.getCiudadesConMayorDemanda().size());
        assertEquals("Bogota", dashboard.getCiudadesConMayorDemanda().get(0).getCiudad());
        assertEquals(3L, dashboard.getCiudadesConMayorDemanda().get(0).getReservas());
        assertEquals("Sin ciudad", dashboard.getCiudadesConMayorDemanda().get(1).getCiudad());
    }

    @Test
    void obtenerDashboard_sinReservas_deberiaDevolverIngresosEnCero() {
        DashboardResponse dashboard = reporteService.obtenerDashboard(admin);

        assertEquals(0, BigDecimal.ZERO.compareTo(dashboard.getIngresosEstimados()));
        assertTrue(dashboard.getCiudadesConMayorDemanda().isEmpty());
        assertEquals(0L, dashboard.getTotalUsuarios());
    }

    @Test
    void obtenerDashboard_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reporteService.obtenerDashboard(anfitrion));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
        verifyNoInteractions(usuarioRepository, publicacionRepository, reservaRepository);
    }

    // ---------- RF-28: reporte PDF ----------

    @Test
    void generarReportePdf_administrador_deberiaDevolverUnPdfValido() {
        when(usuarioRepository.count()).thenReturn(10L);
        when(reservaRepository.sumarIngresos(anyList())).thenReturn(new BigDecimal("630050"));

        byte[] pdf = reporteService.generarReportePdf(admin);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void generarReportePdf_noEsAdministrador_deberiaLanzarExcepcion() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reporteService.generarReportePdf(anfitrion));

        assertEquals("Solo un ADMINISTRADOR puede realizar esta acción", ex.getMessage());
    }
}