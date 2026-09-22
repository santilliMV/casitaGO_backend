package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitarRecuperacionRequest(
        @NotBlank @Email String correo
) {
}
