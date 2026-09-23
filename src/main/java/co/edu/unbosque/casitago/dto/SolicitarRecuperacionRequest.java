package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SolicitarRecuperacionRequest {

    @NotBlank
    @Email
    private String correo;

    public SolicitarRecuperacionRequest() {
    }

    public SolicitarRecuperacionRequest(String correo) {
        this.correo = correo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}