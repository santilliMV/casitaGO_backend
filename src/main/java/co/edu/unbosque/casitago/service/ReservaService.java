package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.*;
import co.edu.unbosque.casitago.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final BigDecimal TARIFA_LIMPIEZA_FIJA = new BigDecimal("25");
    private static final BigDecimal PORCENTAJE_TARIFA_SERVICIO = new BigDecimal("0.05");
    private static final int DIAS_LIMITE_CANCELACION = 3;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private PeriodoDisponibilidadRepository periodoDisponibilidadRepository;

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private CancelacionRepository cancelacionRepository;

    @Autowired
    private NotificacionService notificacionService;

    public CotizacionResponse generarCotizacion(CotizacionRequest request) {
        Publicacion publicacion = publicacionRepository.findById(request.getPublicacionId())
                .orElseThrow(() -> new RuntimeException("La publicación no existe"));

        validarPublicacionYFechas(publicacion, request.getFechaLlegada(), request.getFechaSalida());

        long noches = ChronoUnit.DAYS.between(request.getFechaLlegada(), request.getFechaSalida());
        BigDecimal precioBase = publicacion.getPrecioNoche().multiply(BigDecimal.valueOf(noches));
        BigDecimal tarifaServicio = precioBase.multiply(PORCENTAJE_TARIFA_SERVICIO);
        BigDecimal serviciosAdicionales = request.getServiciosAdicionales() == null
                ? BigDecimal.ZERO : request.getServiciosAdicionales();
        BigDecimal total = precioBase.add(TARIFA_LIMPIEZA_FIJA).add(tarifaServicio).add(serviciosAdicionales);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setPublicacion(publicacion);
        cotizacion.setNoches((int) noches);
        cotizacion.setPrecioBase(precioBase);
        cotizacion.setTarifaLimpieza(TARIFA_LIMPIEZA_FIJA);
        cotizacion.setTarifaServicio(tarifaServicio);
        cotizacion.setServiciosAdicionales(serviciosAdicionales);
        cotizacion.setTotal(total);

        cotizacion = cotizacionRepository.save(cotizacion);
        return CotizacionResponse.desde(cotizacion);
    }

    public ReservaResponse crearReserva(ReservaRequest request, Usuario huesped) {
        String idempotencyKey = request.getCotizacionId().toString() + "-" + huesped.getId().toString();

        Reserva existente = reservaRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existente != null) {
            return ReservaResponse.desde(existente);
        }

        Cotizacion cotizacion = cotizacionRepository.findById(request.getCotizacionId())
                .orElseThrow(() -> new RuntimeException("La cotización no existe"));

        Publicacion publicacion = cotizacion.getPublicacion();

        validarPublicacionYFechas(publicacion, request.getFechaLlegada(), request.getFechaSalida());

        Reserva reserva = new Reserva();
        reserva.setHuesped(huesped);
        reserva.setPublicacion(publicacion);
        reserva.setCotizacion(cotizacion);
        reserva.setFechaLlegada(request.getFechaLlegada());
        reserva.setFechaSalida(request.getFechaSalida());
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setIdempotencyKey(idempotencyKey);

        try {
            reserva = reservaRepository.save(reserva);
        } catch (RuntimeException e) {
            throw new RuntimeException("Las fechas ya no están disponibles, alguien más reservó primero");
        }

        String detalle = "\"" + publicacion.getTitulo() + "\" del "
                + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida();
        notificacionService.crear(huesped, "RESERVA_CONFIRMADA",
                "Tu reserva en " + detalle + " fue confirmada");
        notificacionService.crear(publicacion.getAnfitrion(), "RESERVA_CONFIRMADA",
                "Tienes una nueva reserva en " + detalle);

        return ReservaResponse.desde(reserva);
    }

    public List<ReservaResponse> listarPorHuesped(UUID huespedId) {
        return reservaRepository.findByHuespedIdOrderByCreadoEnDesc(huespedId)
                .stream()
                .map(ReservaResponse::desde)
                .collect(Collectors.toList());
    }

    public CancelacionResponse cancelarReserva(UUID reservaId, Usuario usuario, CancelacionRequest request) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("La reserva no existe"));

        if (!reserva.getHuesped().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar esta reserva");
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA || reserva.getEstado() == EstadoReserva.COMPLETADA) {
            throw new RuntimeException("La reserva no se puede cancelar en su estado actual");
        }

        long diasParaLlegada = ChronoUnit.DAYS.between(LocalDate.now(), reserva.getFechaLlegada());
        BigDecimal valorDevolucion = diasParaLlegada >= DIAS_LIMITE_CANCELACION
                ? reserva.getCotizacion().getTotal() : BigDecimal.ZERO;

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        Cancelacion cancelacion = new Cancelacion();
        cancelacion.setReserva(reserva);
        cancelacion.setUsuario(usuario);
        cancelacion.setMotivo(request.getMotivo());
        cancelacion.setValorDevolucion(valorDevolucion);
        cancelacion.setEstadoFinal(EstadoReserva.CANCELADA);

        cancelacion = cancelacionRepository.save(cancelacion);

        Publicacion publicacion = reserva.getPublicacion();
        String detalle = "\"" + publicacion.getTitulo() + "\" del "
                + reserva.getFechaLlegada() + " al " + reserva.getFechaSalida();
        notificacionService.crear(usuario, "RESERVA_CANCELADA",
                "Cancelaste tu reserva en " + detalle);
        notificacionService.crear(publicacion.getAnfitrion(), "RESERVA_CANCELADA",
                "Se canceló una reserva en " + detalle);

        return CancelacionResponse.desde(cancelacion);
    }

    private void validarPublicacionYFechas(Publicacion publicacion, LocalDate llegada, LocalDate salida) {
        if (publicacion.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new RuntimeException("La publicación no está activa");
        }
        if (!salida.isAfter(llegada)) {
            throw new RuntimeException("La fecha de salida debe ser posterior a la fecha de llegada");
        }
        verificarDisponibilidad(publicacion.getId(), llegada, salida);
    }

    private void verificarDisponibilidad(UUID publicacionId, LocalDate inicio, LocalDate fin) {
        boolean bloqueado = !periodoDisponibilidadRepository.buscarSolapados(publicacionId, inicio, fin).isEmpty();
        boolean reservado = !reservaRepository.buscarSolapadas(publicacionId, inicio, fin).isEmpty();

        if (bloqueado || reservado) {
            throw new RuntimeException("Las fechas seleccionadas no están disponibles");
        }
    }
}