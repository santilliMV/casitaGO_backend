package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CambiarMfaRequest {

    @NotNull(message = "Debes indicar si activas o desactivas la verificación")
    private Boolean habilitado;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;

    public Boolean getHabilitado() {
        return habilitado;
    }

    public void setHabilitado(Boolean habilitado) {
        this.habilitado = habilitado;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}