package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoCuentaRequest(
        @NotNull Boolean activo
) {
}
