package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "notificaciones")
public class Notificacion extends EntidadRegistrada {

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "tipo_evento", nullable = false)
    private String tipoEvento;

    @Column(nullable = false)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }
}