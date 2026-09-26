package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "cancelaciones")
public class Cancelacion extends EntidadRegistrada {

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String motivo;

    @Column(name = "valor_devolucion", nullable = false)
    private BigDecimal valorDevolucion = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "estado_final", nullable = false)
    private EstadoReserva estadoFinal;

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
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
}