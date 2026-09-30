package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    private String contrasena;

    // RF-33: solo se usa si el usuario tiene la autenticación multifactor activa
    private String codigoMfa;

    public LoginRequest() {
    }

    public LoginRequest(String correo, String contrasena) {
        this.correo = correo;
        this.contrasena = contrasena;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getCodigoMfa() {
        return codigoMfa;
    }

    public void setCodigoMfa(String codigoMfa) {
        this.codigoMfa = codigoMfa;
    }
}