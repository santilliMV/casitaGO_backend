package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarPerfilRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String correo
) {
}
