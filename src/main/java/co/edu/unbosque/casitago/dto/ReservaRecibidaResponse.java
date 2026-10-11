package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.Reserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class ReservaRecibidaResponse extends RespuestaConMarcaDeTiempo {

    private UUID publicacionId;
    private String tituloPublicacion;
    private String huespedNombre;
    private LocalDate fechaLlegada;
    private LocalDate fechaSalida;
    private EstadoReserva estado;
    private BigDecimal total;

    public static ReservaRecibidaResponse desde(Reserva reserva) {
        ReservaRecibidaResponse response = new ReservaRecibidaResponse();
        response.setId(reserva.getId());
        response.publicacionId = reserva.getPublicacion().getId();
        response.tituloPublicacion = reserva.getPublicacion().getTitulo();
        response.huespedNombre = reserva.getHuesped().getNombre();
        response.fechaLlegada = reserva.getFechaLlegada();
        response.fechaSalida = reserva.getFechaSalida();
        response.estado = reserva.getEstado();
        response.total = reserva.getCotizacion().getTotal();
        response.setCreadoEn(reserva.getCreadoEn());
        return response;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(UUID publicacionId) {
        this.publicacionId = publicacionId;
    }

    public String getTituloPublicacion() {
        return tituloPublicacion;
    }

    public void setTituloPublicacion(String tituloPublicacion) {
        this.tituloPublicacion = tituloPublicacion;
    }

    public String getHuespedNombre() {
        return huespedNombre;
    }

    public void setHuespedNombre(String huespedNombre) {
        this.huespedNombre = huespedNombre;
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
}