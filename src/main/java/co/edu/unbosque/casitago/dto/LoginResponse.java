package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;

import java.util.UUID;

public class LoginResponse {

    private String token;
    private long expiraEnSegundos;
    private UUID usuarioId;
    private String nombre;
    private RolUsuario rol;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiraEnSegundos, UUID usuarioId, String nombre, RolUsuario rol) {
        this.token = token;
        this.expiraEnSegundos = expiraEnSegundos;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.rol = rol;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiraEnSegundos() {
        return expiraEnSegundos;
    }

    public void setExpiraEnSegundos(long expiraEnSegundos) {
        this.expiraEnSegundos = expiraEnSegundos;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }
}