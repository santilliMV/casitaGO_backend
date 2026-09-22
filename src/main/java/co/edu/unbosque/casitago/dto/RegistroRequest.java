package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 150) String correo,
        @NotBlank @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String contrasena,
        @NotNull RolUsuario rol
) {
    public RegistroRequest {
        // RF-01: solo se permite auto-registro como HUÉSPED o ANFITRIÓN;
        // ADMINISTRADOR nunca se asigna vía este endpoint público.
        if (rol == RolUsuario.ADMINISTRADOR) {
            throw new IllegalArgumentException("El rol ADMINISTRADOR no puede autoasignarse en el registro.");
        }
    }
}
