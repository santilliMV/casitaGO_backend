package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.PagoRequest;
import co.edu.unbosque.casitago.dto.TransaccionPagoResponse;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.TransaccionPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private PasarelaPagoStripe pasarelaPagoStripe;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PagoService pagoService;

    private Usuario huesped;
    private Usuario anfitrion;

    @BeforeEach
    void setUp() {
        anfitrion = new Usuario("Ana Anfitriona", "ana@correo.com", "hash", RolUsuario.ANFITRION);
        ReflectionTestUtils.setField(anfitrion, "id", UUID.randomUUID());

        huesped = new Usuario("Hugo Huesped", "hugo@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(huesped, "id", UUID.randomUUID());
    }

    private Reserva reservaPendiente() {
        Publicacion publicacion = new Publicacion();
        publicacion.setId(UUID.randomUUID());
        publicacion.setAnfitrion(anfitrion);
        publicacion.setTitulo("Casa de prueba");

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setId(UUID.randomUUID());
        cotizacion.setTotal(new BigDecimal("315025.00"));

        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setHuesped(huesped);
        reserva.setPublicacion(publicacion);
        reserva.setCotizacion(cotizacion);
        reserva.setFechaLlegada(LocalDate.of(2026, 12, 1));
        reserva.setFechaSalida(LocalDate.of(2026, 12, 4));
        reserva.setEstado(EstadoReserva.PENDIENTE);
        return reserva;
    }

    private TransaccionPago pagoExitoso(Reserva reserva) {
        TransaccionPago pago = new TransaccionPago();
        pago.setId(UUID.randomUUID());
        pago.setReserva(reserva);
        pago.setMonto(reserva.getCotizacion().getTotal());
        pago.setEstado(EstadoPago.EXITOSO);
        pago.setReferenciaExterna("pi_123");
        return pago;
    }

    // ---------- RF-35: pagar ----------

    @Test
    void pagar_sinCuerpo_cobraConTarjetaPorDefectoYConfirmaLaReserva() {
        Reserva reserva = reservaPendiente();
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.empty());
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pasarelaPagoStripe.cobrar(any(BigDecimal.class), anyString(), anyString())).thenReturn("pi_123");

        TransaccionPagoResponse response = pagoService.pagar(reserva.getId(), huesped, null);

        assertEquals(EstadoPago.EXITOSO, response.getEstado());
        assertEquals("pi_123", response.getReferenciaExterna());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        verify(pasarelaPagoStripe).cobrar(eq(reserva.getCotizacion().getTotal()), eq("pm_card_visa"), anyString());
        verify(reservaRepository).save(reserva);
        verify(notificacionService).crear(eq(huesped), eq("RESERVA_CONFIRMADA"), anyString());
        verify(notificacionService).crear(eq(anfitrion), eq("RESERVA_CONFIRMADA"), anyString());
    }

    @Test
    void pagar_siRechazanElCobro_guardaFallidoYDejaLaReservaPendiente() {
        Reserva reserva = reservaPendiente();
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.empty());
        when(transaccionPagoRepository.save(any(TransaccionPago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pasarelaPagoStripe.cobrar(any(BigDecimal.class), eq("pm_card_chargeDeclined"), anyString()))
                .thenThrow(new RuntimeException("El proveedor de pagos rechazó el cobro"));

        PagoRequest request = new PagoRequest();
        request.setMetodoPago("pm_card_chargeDeclined");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pagoService.pagar(reserva.getId(), huesped, request));

        assertEquals("El proveedor de pagos rechazó el cobro", ex.getMessage());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());

        ArgumentCaptor<TransaccionPago> captor = ArgumentCaptor.forClass(TransaccionPago.class);
        verify(transaccionPagoRepository, times(2)).save(captor.capture());
        assertEquals(EstadoPago.FALLIDO, captor.getValue().getEstado());

        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(notificacionService);
    }

    @Test
    void pagar_siYaTienePagoExitoso_devuelveElMismoSinCobrarOtraVez() {
        Reserva reserva = reservaPendiente();
        TransaccionPago pago = pagoExitoso(reserva);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.of(pago));

        TransaccionPagoResponse response = pagoService.pagar(reserva.getId(), huesped, null);

        assertEquals(pago.getId(), response.getId());
        verifyNoInteractions(pasarelaPagoStripe);
    }

    @Test
    void pagar_reservaCancelada_deberiaLanzarExcepcion() {
        Reserva reserva = reservaPendiente();
        reserva.setEstado(EstadoReserva.CANCELADA);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pagoService.pagar(reserva.getId(), huesped, null));

        assertEquals("La reserva no se puede pagar en su estado actual", ex.getMessage());
        verifyNoInteractions(pasarelaPagoStripe);
    }

    @Test
    void pagar_reservaDeOtroHuesped_deberiaLanzarExcepcion() {
        Reserva reserva = reservaPendiente();
        Usuario otro = new Usuario("Otro", "otro@correo.com", "hash", RolUsuario.HUESPED);
        ReflectionTestUtils.setField(otro, "id", UUID.randomUUID());
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pagoService.pagar(reserva.getId(), otro, null));

        assertEquals("No tienes permiso sobre esta reserva", ex.getMessage());
        verifyNoInteractions(pasarelaPagoStripe);
    }

    @Test
    void pagar_reservaInexistente_deberiaLanzarExcepcion() {
        UUID reservaId = UUID.randomUUID();
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> pagoService.pagar(reservaId, huesped, null));

        assertEquals("La reserva no existe", ex.getMessage());
    }

    // ---------- listar pagos ----------

    @Test
    void listarPorReserva_devuelveLasTransacciones() {
        Reserva reserva = reservaPendiente();
        TransaccionPago pago = pagoExitoso(reserva);
        when(reservaRepository.findById(reserva.getId())).thenReturn(Optional.of(reserva));
        when(transaccionPagoRepository.findByReservaIdOrderByCreadoEnDesc(reserva.getId()))
                .thenReturn(List.of(pago));

        List<TransaccionPagoResponse> resultado = pagoService.listarPorReserva(reserva.getId(), huesped);

        assertEquals(1, resultado.size());
        assertEquals(pago.getId(), resultado.get(0).getId());
    }

    // ---------- reembolso al cancelar ----------

    @Test
    void reembolsarSiCorresponde_conPagoExitoso_reembolsaYMarcaLaTransaccion() {
        Reserva reserva = reservaPendiente();
        TransaccionPago pago = pagoExitoso(reserva);
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.of(pago));

        pagoService.reembolsarSiCorresponde(reserva, new BigDecimal("315025.00"), huesped.getId());

        verify(pasarelaPagoStripe).reembolsar("pi_123", new BigDecimal("315025.00"));
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
        verify(transaccionPagoRepository).save(pago);
    }

    @Test
    void reembolsarSiCorresponde_conValorCero_noHaceNada() {
        Reserva reserva = reservaPendiente();

        pagoService.reembolsarSiCorresponde(reserva, BigDecimal.ZERO, huesped.getId());

        verifyNoInteractions(pasarelaPagoStripe, transaccionPagoRepository);
    }

    @Test
    void reembolsarSiCorresponde_sinPagoExitoso_noHaceNada() {
        Reserva reserva = reservaPendiente();
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.empty());

        pagoService.reembolsarSiCorresponde(reserva, new BigDecimal("315025.00"), huesped.getId());

        verifyNoInteractions(pasarelaPagoStripe);
    }

    @Test
    void reembolsarSiCorresponde_siFallaStripe_propagaElErrorYNoCambiaElPago() {
        Reserva reserva = reservaPendiente();
        TransaccionPago pago = pagoExitoso(reserva);
        when(transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO))
                .thenReturn(Optional.of(pago));
        doThrow(new RuntimeException("El proveedor de pagos no pudo procesar el reembolso"))
                .when(pasarelaPagoStripe).reembolsar(anyString(), any(BigDecimal.class));

        assertThrows(RuntimeException.class,
                () -> pagoService.reembolsarSiCorresponde(reserva, new BigDecimal("315025.00"), huesped.getId()));

        assertEquals(EstadoPago.EXITOSO, pago.getEstado());
        verify(transaccionPagoRepository, never()).save(any());
    }
}