package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.CotizacionRequest;
import co.edu.unbosque.casitago.dto.CotizacionResponse;
import co.edu.unbosque.casitago.dto.ReservaRequest;
import co.edu.unbosque.casitago.entity.Cotizacion;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.TipoAlojamiento;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.CotizacionRepository;
import co.edu.unbosque.casitago.repository.PeriodoDisponibilidadRepository;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceReglasDeRolTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PeriodoDisponibilidadRepository periodoDisponibilidadRepository;

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ReservaService reservaService;

    private final LocalDate llegada = LocalDate.of(2026, 12, 1);
    private final LocalDate salida = LocalDate.of(2026, 12, 4);

    private Usuario anfitrion;
    private Publicacion publicacion;

    @BeforeEach
    void setUp() {
        anfitrion = usuario("Ana Anfitriona", RolUsuario.ANFITRION);
        publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Publicación de prueba");
        publicacion.setDescripcion("Descripción");
        publicacion.setUbicacionTextual("Ubicación");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
    }

    // ---------- cotizar ----------

    @Test
    void generarCotizacion_administrador_noPuedeReservar() {
        Usuario admin = usuario("Admin", RolUsuario.ADMINISTRADOR);
        CotizacionRequest request = cotizacionRequest();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.generarCotizacion(request, admin));

        assertEquals("Los administradores no pueden hacer reservas.", ex.getMessage());
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void generarCotizacion_anfitrionDuenio_noPuedeReservarSuPropiaPublicacion() {
        CotizacionRequest request = cotizacionRequest();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.generarCotizacion(request, anfitrion));

        assertEquals("No puedes reservar tu propia publicación.", ex.getMessage());
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void generarCotizacion_otroAnfitrion_siPuedeReservar() {
        Usuario otroAnfitrion = usuario("Otro Anfitrion", RolUsuario.ANFITRION);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(reservaRepository.buscarSolapadas(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion cotizacion = inv.getArgument(0);
            cotizacion.setId(UUID.randomUUID());
            return cotizacion;
        });

        CotizacionResponse response = reservaService.generarCotizacion(cotizacionRequest(), otroAnfitrion);

        assertNotNull(response.getId());
        assertEquals(3, response.getNoches());
    }

    // ---------- reservar ----------

    @Test
    void crearReserva_administrador_noPuedeReservar() {
        Usuario admin = usuario("Admin", RolUsuario.ADMINISTRADOR);
        ReservaRequest request = reservaRequest(cotizacionDeLaPublicacion());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(request, admin));

        assertEquals("Los administradores no pueden hacer reservas.", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearReserva_anfitrionDuenio_noPuedeReservarSuPropiaPublicacion() {
        ReservaRequest request = reservaRequest(cotizacionDeLaPublicacion());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservaService.crearReserva(request, anfitrion));

        assertEquals("No puedes reservar tu propia publicación.", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    // ---------- helpers ----------

    private Usuario usuario(String nombre, RolUsuario rol) {
        Usuario usuario = new Usuario(nombre, nombre.replace(" ", "").toLowerCase() + "@correo.com", "hash", rol);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        return usuario;
    }

    private CotizacionRequest cotizacionRequest() {
        CotizacionRequest request = new CotizacionRequest();
        request.setPublicacionId(publicacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);
        request.setServiciosAdicionales(BigDecimal.ZERO);
        return request;
    }

    private Cotizacion cotizacionDeLaPublicacion() {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(UUID.randomUUID());
        cotizacion.setPublicacion(publicacion);
        when(cotizacionRepository.findById(cotizacion.getId())).thenReturn(Optional.of(cotizacion));
        return cotizacion;
    }

    private ReservaRequest reservaRequest(Cotizacion cotizacion) {
        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);
        return request;
    }
}