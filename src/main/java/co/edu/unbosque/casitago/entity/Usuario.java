package co.edu.unbosque.casitago.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Tabla `usuarios` — RF-01 a RF-07, RF-25.
 * Implementa UserDetails para que Spring Security lo use directamente como
 * principal autenticado (sin una capa extra de UserDetailsService "envoltorio").
 */
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "correo", length = 150, nullable = false, unique = true)
    private String correo;

    @Column(name = "contrasena_hash", nullable = false)
    private String contrasenaHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "rol", nullable = false, columnDefinition = "rol_usuario")
    private RolUsuario rol;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    protected Usuario() {
        // JPA
    }

    public Usuario(String nombre, String correo, String contrasenaHash, RolUsuario rol) {
        this.nombre = nombre;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.activo = true;
    }

    @PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = OffsetDateTime.now();
        }
    }

    // ---------- Getters / setters de dominio ----------

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    // ---------- Contrato UserDetails (Spring Security) ----------

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Prefijo ROLE_ requerido por hasRole()/@PreAuthorize("hasRole(...)")
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // RF-25: una restricción activa (fuera de este módulo) también podría
        // reflejarse aquí más adelante; por ahora depende solo de `activo`.
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // RF-06: cuenta desactivada = no puede autenticarse
        return activo;
    }
}
