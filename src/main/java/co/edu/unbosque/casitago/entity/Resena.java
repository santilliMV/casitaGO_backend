package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "resenas")
public class Resena extends EntidadRegistrada {

    @ManyToOne
    @JoinColumn(name = "huesped_id", nullable = false)
    private Usuario huesped;

    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @ManyToOne
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(nullable = false)
    private int calificacion;

    private String comentario;

    @Column(nullable = false)
    private boolean oculta = false;

    public Usuario getHuesped() {
        return huesped;
    }

    public void setHuesped(Usuario huesped) {
        this.huesped = huesped;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(Publicacion publicacion) {
        this.publicacion = publicacion;
    }

    public int getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(int calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public boolean isOculta() {
        return oculta;
    }

    public void setOculta(boolean oculta) {
        this.oculta = oculta;
    }
}