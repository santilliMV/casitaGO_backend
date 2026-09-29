package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.CancelacionRequest;
import co.edu.unbosque.casitago.dto.CancelacionResponse;
import co.edu.unbosque.casitago.dto.CotizacionRequest;
import co.edu.unbosque.casitago.dto.CotizacionResponse;
import co.edu.unbosque.casitago.dto.ReservaRequest;
import co.edu.unbosque.casitago.dto.ReservaResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.CancelacionRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private PublicacionRepository publicacionRepository;

    @Mock
    private PeriodoDisponibilidadRepository periodoDisponibilidadRepository;

    @Mock
    private CotizacionRepository cotizacionRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private CancelacionRepository cancelacionRepository;

    @InjectMocks
    private ReservaService reservaService;

    @Mock
    private NotificacionService notificacionService;

    private Usuario huesped;
    private Usuario anfitrion;

    private final LocalDate llegada = LocalDate.of(2026, 12, 1);
    private final LocalDate salida = LocalDate.of(2026, 12, 4);

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private Publicacion publicacionActiva() {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Publicación de prueba");
        publicacion.setDescripcion("Descripción");
        publicacion.setUbicacionTextual("Ubicación");
        publicacion.setTipo(TipoAlojamiento.APARTAMENTO);
        publicacion.setCapacidad(2);
        publicacion.setPrecioNoche(new BigDecimal("100000"));
        publicacion.setEstado(EstadoPublicacion.ACTIVA);
        return publicacion;
    }

    private CotizacionRequest cotizacionRequest(UUID publicacionId) {
        CotizacionRequest request = new CotizacionRequest();
        request.setPublicacionId(publicacionId);
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);
        request.setServiciosAdicionales(BigDecimal.ZERO);
        return request;
    }

    private Cotizacion cotizacionExistente(Publicacion publicacion) {
        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(UUID.randomUUID());
        cotizacion.setPublicacion(publicacion);
        cotizacion.setNoches(3);
        cotizacion.setPrecioBase(new BigDecimal("300000"));
        cotizacion.setTarifaLimpieza(new BigDecimal("25"));
        cotizacion.setTarifaServicio(new BigDecimal("15000.00"));
        cotizacion.setServiciosAdicionales(BigDecimal.ZERO);
        cotizacion.setTotal(new BigDecimal("315025.00"));
        return cotizacion;
    }

    private Reserva reservaConfirmada(Usuario huesped, Publicacion publicacion, Cotizacion cotizacion,
                                      LocalDate llegada, LocalDate salida) {
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setHuesped(huesped);
        reserva.setPublicacion(publicacion);
        reserva.setCotizacion(cotizacion);
        reserva.setFechaLlegada(llegada);
        reserva.setFechaSalida(salida);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        return reserva;
    }

    // ---------- RF-17: generar cotización ----------

    @Test
    void generarCotizacion_publicacionActivaSinCruces_calculaTotalesCorrectamente() {
        Publicacion publicacion = publicacionActiva();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(reservaRepository.buscarSolapadas(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion cotizacion = inv.getArgument(0);
            cotizacion.setId(UUID.randomUUID());
            return cotizacion;
        });

        CotizacionResponse response = reservaService.generarCotizacion(cotizacionRequest(publicacion.getId()));

        assertEquals(3, response.getNoches());
        assertEquals(0, new BigDecimal("300000").compareTo(response.getPrecioBase()));
        assertEquals(0, new BigDecimal("25").compareTo(response.getTarifaLimpieza()));
        assertEquals(0, new BigDecimal("15000.00").compareTo(response.getTarifaServicio()));
        assertEquals(0, new BigDecimal("315025.00").compareTo(response.getTotal()));
    }

    @Test
    void generarCotizacion_publicacionNoActiva_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        publicacion.setEstado(EstadoPublicacion.PAUSADA);
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.generarCotizacion(cotizacionRequest(publicacion.getId())));

        assertEquals("La publicación no está activa", ex.getMessage());
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void generarCotizacion_fechaSalidaAnteriorAFechaLlegada_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));

        CotizacionRequest request = cotizacionRequest(publicacion.getId());
        request.setFechaLlegada(salida);
        request.setFechaSalida(llegada);

        assertThrows(RuntimeException.class, () -> reservaService.generarCotizacion(request));
    }

    @Test
    void generarCotizacion_conPeriodoBloqueado_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        PeriodoDisponibilidad periodo = new PeriodoDisponibilidad();
        periodo.setEstado(EstadoPeriodo.BLOQUEADO);
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida))
                .thenReturn(List.of(periodo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.generarCotizacion(cotizacionRequest(publicacion.getId())));

        assertEquals("Las fechas seleccionadas no están disponibles", ex.getMessage());
    }

    @Test
    void generarCotizacion_conReservaExistente_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        when(publicacionRepository.findById(publicacion.getId())).thenReturn(Optional.of(publicacion));
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida)).thenReturn(List.of());

        Reserva reservaExistente = new Reserva();
        when(reservaRepository.buscarSolapadas(publicacion.getId(), llegada, salida))
                .thenReturn(List.of(reservaExistente));

        assertThrows(RuntimeException.class,
                () -> reservaService.generarCotizacion(cotizacionRequest(publicacion.getId())));
    }

    @Test
    void generarCotizacion_conPublicacionInexistente_deberiaLanzarExcepcion() {
        UUID publicacionId = UUID.randomUUID();
        when(publicacionRepository.findById(publicacionId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.generarCotizacion(cotizacionRequest(publicacionId)));

        assertEquals("La publicación no existe", ex.getMessage());
    }

    // ---------- RF-18: crear reserva ----------

    @Test
    void crearReserva_datosValidos_deberiaQuedarConfirmada() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        when(reservaRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(cotizacionRepository.findById(cotizacion.getId())).thenReturn(Optional.of(cotizacion));
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(reservaRepository.buscarSolapadas(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva reserva = inv.getArgument(0);
            reserva.setId(UUID.randomUUID());
            return reserva;
        });

        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);

        ReservaResponse response = reservaService.crearReserva(request, huesped);

        assertEquals(EstadoReserva.CONFIRMADA, response.getEstado());
        assertEquals(0, cotizacion.getTotal().compareTo(response.getTotal()));
        verify(notificacionService).crear(eq(huesped), eq("RESERVA_CONFIRMADA"), anyString());
        verify(notificacionService).crear(eq(anfitrion), eq("RESERVA_CONFIRMADA"), anyString());
    }

    @Test
    void crearReserva_conIdempotencyKeyExistente_deberiaDevolverLaMisma() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reservaExistente = reservaConfirmada(huesped, publicacion, cotizacion, llegada, salida);

        when(reservaRepository.findByIdempotencyKey(any())).thenReturn(Optional.of(reservaExistente));

        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);

        ReservaResponse response = reservaService.crearReserva(request, huesped);

        assertEquals(reservaExistente.getId(), response.getId());
        verify(cotizacionRepository, never()).findById(any());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearReserva_conCotizacionInexistente_deberiaLanzarExcepcion() {
        UUID cotizacionId = UUID.randomUUID();
        when(reservaRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.empty());

        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacionId);
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.crearReserva(request, huesped));

        assertEquals("La cotización no existe", ex.getMessage());
    }

    @Test
    void crearReserva_conFechasYaNoDisponibles_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        when(reservaRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(cotizacionRepository.findById(cotizacion.getId())).thenReturn(Optional.of(cotizacion));

        Reserva otraReserva = new Reserva();
        when(periodoDisponibilidadRepository.buscarSolapados(publicacion.getId(), llegada, salida)).thenReturn(List.of());
        when(reservaRepository.buscarSolapadas(publicacion.getId(), llegada, salida)).thenReturn(List.of(otraReserva));

        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.crearReserva(request, huesped));

        assertEquals("Las fechas seleccionadas no están disponibles", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearReserva_conPublicacionNoActiva_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        publicacion.setEstado(EstadoPublicacion.PAUSADA);
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        when(reservaRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(cotizacionRepository.findById(cotizacion.getId())).thenReturn(Optional.of(cotizacion));

        ReservaRequest request = new ReservaRequest();
        request.setCotizacionId(cotizacion.getId());
        request.setFechaLlegada(llegada);
        request.setFechaSalida(salida);

        assertThrows(RuntimeException.class, () -> reservaService.crearReserva(request, huesped));
    }

    // ---------- RF-19: listar historial ----------

    @Test
    void listarPorHuesped_deberiaMapearTodasLasReservas() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reserva = reservaConfirmada(huesped, publicacion, cotizacion, llegada, salida);
        reserva.setCreadoEn(OffsetDateTime.now());

        when(reservaRepository.findByHuespedIdOrderByCreadoEnDesc(huesped.getId())).thenReturn(List.of(reserva));

        List<ReservaResponse> resultado = reservaService.listarPorHuesped(huesped.getId());

        assertEquals(1, resultado.size());
        assertEquals(reserva.getId(), resultado.get(0).getId());
    }

    // ---------- RF-20: cancelar reserva ----------

    @Test
    void cancelarReserva_conMasDeTresDiasDeAnticipacion_deberiaReembolsarTotal() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reserva = reservaConfirmada(huesped, publicacion, cotizacion,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(inv -> {
            Cancelacion cancelacion = inv.getArgument(0);
            cancelacion.setId(UUID.randomUUID());
            return cancelacion;
        });

        CancelacionRequest request = new CancelacionRequest();
        request.setMotivo("Cambio de planes");

        CancelacionResponse response = reservaService.cancelarReserva(reserva.getId(), huesped, request);

        assertEquals(0, cotizacion.getTotal().compareTo(response.getValorDevolucion()));
        assertEquals(EstadoReserva.CANCELADA, response.getEstadoFinal());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        verify(notificacionService).crear(eq(huesped), eq("RESERVA_CANCELADA"), anyString());
        verify(notificacionService).crear(eq(anfitrion), eq("RESERVA_CANCELADA"), anyString());
    }

    @Test
    void cancelarReserva_conMenosDeTresDiasDeAnticipacion_noDeberiaReembolsar() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reserva = reservaConfirmada(huesped, publicacion, cotizacion,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(cancelacionRepository.save(any(Cancelacion.class))).thenAnswer(inv -> {
            Cancelacion cancelacion = inv.getArgument(0);
            cancelacion.setId(UUID.randomUUID());
            return cancelacion;
        });

        CancelacionRequest request = new CancelacionRequest();
        request.setMotivo("Cambio de planes");

        CancelacionResponse response = reservaService.cancelarReserva(reserva.getId(), huesped, request);

        assertEquals(0, BigDecimal.ZERO.compareTo(response.getValorDevolucion()));
        verify(notificacionService).crear(eq(huesped), eq("RESERVA_CANCELADA"), anyString());
        verify(notificacionService).crear(eq(anfitrion), eq("RESERVA_CANCELADA"), anyString());
    }

    @Test
    void cancelarReserva_deOtroHuesped_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reserva = reservaConfirmada(huesped, publicacion, cotizacion,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));

        Usuario otroHuesped = new Usuario("Otro Huesped", "otro@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(otroHuesped, "id", UUID.randomUUID());

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        CancelacionRequest request = new CancelacionRequest();
        request.setMotivo("Cambio de planes");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.cancelarReserva(reserva.getId(), otroHuesped, request));

        assertEquals("No tienes permiso para cancelar esta reserva", ex.getMessage());
        verify(cancelacionRepository, never()).save(any());
    }

    @Test
    void cancelarReserva_yaCancelada_deberiaLanzarExcepcion() {
        Publicacion publicacion = publicacionActiva();
        Cotizacion cotizacion = cotizacionExistente(publicacion);
        Reserva reserva = reservaConfirmada(huesped, publicacion, cotizacion,
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));
        reserva.setEstado(EstadoReserva.CANCELADA);

        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        CancelacionRequest request = new CancelacionRequest();
        request.setMotivo("Cambio de planes");

        assertThrows(RuntimeException.class,
                () -> reservaService.cancelarReserva(reserva.getId(), huesped, request));
    }

    @Test
    void cancelarReserva_conReservaInexistente_deberiaLanzarExcepcion() {
        UUID reservaId = UUID.randomUUID();
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.empty());

        CancelacionRequest request = new CancelacionRequest();
        request.setMotivo("Cambio de planes");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservaService.cancelarReserva(reservaId, huesped, request));

        assertEquals("La reserva no existe", ex.getMessage());
    }
}