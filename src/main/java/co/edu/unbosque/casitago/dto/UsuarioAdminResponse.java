package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.Usuario;

public class UsuarioAdminResponse extends PerfilResponse {

    private boolean restringido;

    public UsuarioAdminResponse(Usuario usuario, boolean restringido) {
        super(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol(),
                usuario.isEnabled(), usuario.getCreadoEn());
        setMfaHabilitado(usuario.isMfaHabilitado());
        this.restringido = restringido;
    }

    public static UsuarioAdminResponse desde(Usuario usuario, boolean restringido) {
        return new UsuarioAdminResponse(usuario, restringido);
    }

    public boolean isRestringido() {
        return restringido;
    }

    public void setRestringido(boolean restringido) {
        this.restringido = restringido;
    }
}