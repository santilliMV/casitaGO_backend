package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ReservaResponse {

    private UUID id;
    private UUID publicacionId;
    private LocalDate fechaLlegada;
    private LocalDate fechaSalida;
    private EstadoReserva estado;
    private BigDecimal total;
    private OffsetDateTime creadoEn;

    public static ReservaResponse desde(Reserva reserva) {
        ReservaResponse response = new ReservaResponse();
        response.id = reserva.getId();
        response.publicacionId = reserva.getPublicacion().getId();
        response.fechaLlegada = reserva.getFechaLlegada();
        response.fechaSalida = reserva.getFechaSalida();
        response.estado = reserva.getEstado();
        response.total = reserva.getCotizacion().getTotal();
        response.creadoEn = reserva.getCreadoEn();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public LocalDate getFechaLlegada() {
        return fechaLlegada;
    }

    public void setFechaLlegada(LocalDate fechaLlegada) {
        this.fechaLlegada = fechaLlegada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }

    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}