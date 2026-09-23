package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotNull;

public class CambiarEstadoCuentaRequest {

    @NotNull
    private Boolean activo;

    public CambiarEstadoCuentaRequest() {
    }

    public CambiarEstadoCuentaRequest(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}