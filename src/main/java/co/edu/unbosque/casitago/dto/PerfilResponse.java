package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PerfilResponse {

    private UUID id;
    private String nombre;
    private String correo;
    private RolUsuario rol;
    private boolean activo;
    private OffsetDateTime creadoEn;

    public PerfilResponse() {
    }

    public PerfilResponse(UUID id, String nombre, String correo, RolUsuario rol, boolean activo, OffsetDateTime creadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.rol = rol;
        this.activo = activo;
        this.creadoEn = creadoEn;
    }

    public static PerfilResponse desde(Usuario usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isEnabled(),
                usuario.getCreadoEn()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(OffsetDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}