package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tabla `codigos_recuperacion` — RF-03.
 */
@Entity
@Table(name = "codigos_recuperacion")
public class CodigoRecuperacion {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo", length = 10, nullable = false)
    private String codigo;

    @Column(name = "expira_en", nullable = false)
    private OffsetDateTime expiraEn;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    protected CodigoRecuperacion() {
        // JPA
    }

    public CodigoRecuperacion(Usuario usuario, String codigo, OffsetDateTime expiraEn) {
        this.usuario = usuario;
        this.codigo = codigo;
        this.expiraEn = expiraEn;
        this.usado = false;
    }

    public UUID getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getCodigo() {
        return codigo;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public boolean isUsado() {
        return usado;
    }

    public void marcarUsado() {
        this.usado = true;
    }

    public boolean esValido(String candidato) {
        return !usado
                && codigo.equals(candidato)
                && expiraEn.isAfter(OffsetDateTime.now());
    }
}
