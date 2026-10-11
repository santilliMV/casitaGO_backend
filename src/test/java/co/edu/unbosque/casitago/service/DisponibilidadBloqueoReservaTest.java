package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.BloqueoRequest;
import co.edu.unbosque.casitago.entity.PeriodoDisponibilidad;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Reserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadBloqueoReservaTest {

    @Mock
    private PeriodoDisponibilidadRepository periodoRepository;

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DisponibilidadService disponibilidadService;

    private Usuario anfitrion;
    private Publicacion publicacion;
    private final LocalDate inicio = LocalDate.of(2026, 12, 20);
    private final LocalDate fin = LocalDate.of(2026, 12, 22);

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
    }

    private BloqueoRequest bloqueoRequest() {
        BloqueoRequest request = new BloqueoRequest();
        request.setFechaInicio(inicio);
        request.setFechaFin(fin);
        return request;
    }

    @Test
    void crearBloqueo_sobreUnaReservaExistente_deberiaLanzarExcepcion() {
        when(usuarioRepository.findById(anfitrion.getId())).thenReturn(Optional.of(anfitrion));
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoRepository.buscarSolapados(publicacion.getId(), inicio, fin)).thenReturn(List.of());
        when(reservaRepository.buscarSolapadas(publicacion.getId(), inicio, fin)).thenReturn(List.of(new Reserva()));

        UUID anfitrionId = anfitrion.getId();
        UUID publicacionId = publicacion.getId();
        BloqueoRequest request = bloqueoRequest();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> disponibilidadService.crearBloqueo(anfitrionId, publicacionId, request));

        assertEquals("El rango se cruza con otro bloqueo o con una reserva existente.", ex.getMessage());
        verify(periodoRepository, never()).save(any(PeriodoDisponibilidad.class));
    }
}