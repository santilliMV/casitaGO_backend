package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ConfirmarRecuperacionRequest {

    @NotBlank
    @Email
    private String correo;

    @NotBlank
    private String codigo;

    @NotBlank
    @Size(min = 8, max = 72)
    private String nuevaContrasena;

    public ConfirmarRecuperacionRequest() {
    }

    public ConfirmarRecuperacionRequest(String correo, String codigo, String nuevaContrasena) {
        this.correo = correo;
        this.codigo = codigo;
        this.nuevaContrasena = nuevaContrasena;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNuevaContrasena() {
        return nuevaContrasena;
    }

    public void setNuevaContrasena(String nuevaContrasena) {
        this.nuevaContrasena = nuevaContrasena;
    }
}