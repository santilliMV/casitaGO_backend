package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;

import java.util.UUID;

public class LoginResponse {

    private String token;
    private long expiraEnSegundos;
    private UUID usuarioId;
    private String nombre;
    private RolUsuario rol;
    private boolean mfaRequerido;

    public LoginResponse() {
    }

    public LoginResponse(String token, long expiraEnSegundos, UUID usuarioId, String nombre, RolUsuario rol) {
        this.token = token;
        this.expiraEnSegundos = expiraEnSegundos;
        this.usuarioId = usuarioId;
        this.nombre = nombre;
        this.rol = rol;
    }

    // RF-33: el usuario tiene MFA y todavia no mando el codigo; se le envio por correo.
    public static LoginResponse codigoMfaRequerido() {
        LoginResponse respuesta = new LoginResponse();
        respuesta.setMfaRequerido(true);
        return respuesta;
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


    public boolean isMfaRequerido() {
        return mfaRequerido;
    }

    public void setMfaRequerido(boolean mfaRequerido) {
        this.mfaRequerido = mfaRequerido;
    }
}