package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.common.audit.AuditService;
import co.edu.unbosque.casitago.dto.PagoRequest;
import co.edu.unbosque.casitago.dto.TransaccionPagoResponse;
import co.edu.unbosque.casitago.entity.EstadoPago;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Reserva;
import co.edu.unbosque.casitago.entity.TransaccionPago;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.TransaccionPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RF-35: cobro simulado de una reserva con Stripe en modo test.
 * Sin @Transactional a propósito: si el cobro falla, la transacción FALLIDO
 * debe quedar guardada aunque se lance la excepción (RNF-14).
 */
@Service
public class PagoService {

    private static final String METODO_PAGO_POR_DEFECTO = "pm_card_visa";

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private TransaccionPagoRepository transaccionPagoRepository;

    @Autowired
    private PasarelaPagoStripe pasarelaPagoStripe;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private AuditService auditService;

    public TransaccionPagoResponse pagar(UUID reservaId, Usuario usuario, PagoRequest request) {
        Reserva reserva = buscarReservaDelHuesped(reservaId, usuario);

        // Reintento de un pago que ya salió bien: se devuelve el mismo, sin cobrar de nuevo (RNF-06).
        Optional<TransaccionPago> pagoExistente =
                transaccionPagoRepository.findFirstByReservaIdAndEstado(reservaId, EstadoPago.EXITOSO);
        if (pagoExistente.isPresent()) {
            return TransaccionPagoResponse.desde(pagoExistente.get());
        }

        if (reserva.getEstado() != EstadoReserva.PENDIENTE) {
            throw new RuntimeException("La reserva no se puede pagar en su estado actual");
        }

        String metodoPago = (request == null || request.getMetodoPago() == null
                || request.getMetodoPago().isBlank())
                ? METODO_PAGO_POR_DEFECTO : request.getMetodoPago();

        TransaccionPago transaccion = new TransaccionPago();
        transaccion.setReserva(reserva);
        transaccion.setMonto(reserva.getCotizacion().getTotal());
        transaccion = transaccionPagoRepository.save(transaccion);

        try {
            String referencia = pasarelaPagoStripe.cobrar(
                    transaccion.getMonto(), metodoPago, "pago-" + transaccion.getId());
            transaccion.setReferenciaExterna(referencia);
            transaccion.setEstado(EstadoPago.EXITOSO);
        } catch (RuntimeException e) {
            transaccion.setEstado(EstadoPago.FALLIDO);
            transaccionPagoRepository.save(transaccion);
            auditService.registrar(usuario.getId(), "PAGO", "COBRO", "FALLIDO",
                    Map.<String, Object>of("reservaId", reservaId.toString(),
                            "motivo", String.valueOf(e.getMessage())));
            throw e;
        }

        transaccion = transaccionPagoRepository.save(transaccion);

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.save(reserva);

        Publicacion publicacion = reserva.getPublicacion();
        String detalle = "\"" + publicacion.getTitulo() + "\" del "
                + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida();
        notificacionService.crear(usuario, "RESERVA_CONFIRMADA",
                "Tu reserva en " + detalle + " fue confirmada");
        notificacionService.crear(publicacion.getAnfitrion(), "RESERVA_CONFIRMADA",
                "Tienes una nueva reserva en " + detalle);

        auditService.registrar(usuario.getId(), "PAGO", "COBRO", "EXITOSO",
                Map.<String, Object>of("reservaId", reservaId.toString()));

        return TransaccionPagoResponse.desde(transaccion);
    }

    public List<TransaccionPagoResponse> listarPorReserva(UUID reservaId, Usuario usuario) {
        buscarReservaDelHuesped(reservaId, usuario);
        return transaccionPagoRepository.findByReservaIdOrderByCreadoEnDesc(reservaId)
                .stream()
                .map(TransaccionPagoResponse::desde)
                .collect(Collectors.toList());
    }

    /**
     * Lo llama ReservaService al cancelar. Si la reserva tiene un pago exitoso y hay
     * valor a devolver, reembolsa en Stripe y marca la transacción. Si Stripe falla,
     * la excepción sube y la reserva NO se cancela (RNF-14).
     */
    public void reembolsarSiCorresponde(Reserva reserva, BigDecimal valorDevolucion, UUID usuarioId) {
        if (valorDevolucion.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Optional<TransaccionPago> pago =
                transaccionPagoRepository.findFirstByReservaIdAndEstado(reserva.getId(), EstadoPago.EXITOSO);
        if (pago.isEmpty()) {
            return;
        }

        TransaccionPago transaccion = pago.get();
        pasarelaPagoStripe.reembolsar(transaccion.getReferenciaExterna(), valorDevolucion);

        transaccion.setEstado(EstadoPago.REEMBOLSADO);
        transaccionPagoRepository.save(transaccion);

        auditService.registrar(usuarioId, "PAGO", "REEMBOLSO", "EXITOSO",
                Map.<String, Object>of("reservaId", reserva.getId().toString()));
    }

    private Reserva buscarReservaDelHuesped(UUID reservaId, Usuario usuario) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("La reserva no existe"));

        if (!reserva.getHuesped().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso sobre esta reserva");
        }
        return reserva;
    }
}