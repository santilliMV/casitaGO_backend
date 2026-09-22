package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;

import java.util.UUID;

public record LoginResponse(
        String token,
        long expiraEnSegundos,
        UUID usuarioId,
        String nombre,
        RolUsuario rol
) {
}
