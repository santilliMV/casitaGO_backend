package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelacionRequest {

    @NotBlank
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}