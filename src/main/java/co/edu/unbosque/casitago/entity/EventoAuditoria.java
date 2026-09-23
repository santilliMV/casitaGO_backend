package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Tabla `eventos_auditoria` — RF-26, RNF-07.
 * Cualquier módulo puede escribir aquí a través de common.audit.AuditService;
 * este entity vive en el paquete `entity` general para mantener la
 * convención de estructura plana por capa del proyecto.
 */
@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Nullable: hay eventos (ej. intento de login fallido con correo inexistente)
    // que no tienen un usuario_id resoluble.
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "entidad", length = 100, nullable = false)
    private String entidad;

    @Column(name = "accion", length = 100, nullable = false)
    private String accion;

    @Column(name = "resultado", length = 50, nullable = false)
    private String resultado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle")
    private Map<String, Object> detalle;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected EventoAuditoria() {
        // JPA
    }

    public EventoAuditoria(UUID usuarioId, String entidad, String accion, String resultado, Map<String, Object> detalle) {
        this.usuarioId = usuarioId;
        this.entidad = entidad;
        this.accion = accion;
        this.resultado = resultado;
        this.detalle = detalle;
    }

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }
}
