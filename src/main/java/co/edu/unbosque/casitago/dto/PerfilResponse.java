package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PerfilResponse(
        UUID id,
        String nombre,
        String correo,
        RolUsuario rol,
        boolean activo,
        OffsetDateTime creadoEn
) {
    public static PerfilResponse desde(Usuario usuario) {
        return new PerfilResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.isEnabled(),
                usuario.getCreadoEn()
        );
    }
}
