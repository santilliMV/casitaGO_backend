package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Cancelacion;
import co.edu.unbosque.casitago.entity.EstadoReserva;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CancelacionResponse {

    private UUID id;
    private UUID reservaId;
    private String motivo;
    private BigDecimal valorDevolucion;
    private EstadoReserva estadoFinal;
    private OffsetDateTime creadoEn;

    public static CancelacionResponse desde(Cancelacion cancelacion) {
        CancelacionResponse response = new CancelacionResponse();
        response.id = cancelacion.getId();
        response.reservaId = cancelacion.getReserva().getId();
        response.motivo = cancelacion.getMotivo();
        response.valorDevolucion = cancelacion.getValorDevolucion();
        response.estadoFinal = cancelacion.getEstadoFinal();
        response.creadoEn = cancelacion.getCreadoEn();
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public void setReservaId(UUID reservaId) {
        this.reservaId = reservaId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getValorDevolucion() {
        return valorDevolucion;
    }

    public void setValorDevolucion(BigDecimal valorDevolucion) {
        this.valorDevolucion = valorDevolucion;
    }

    public EstadoReserva getEstadoFinal() {
        return estadoFinal;
    }

    public void setEstadoFinal(EstadoReserva estadoFinal) {
        this.estadoFinal = estadoFinal;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}