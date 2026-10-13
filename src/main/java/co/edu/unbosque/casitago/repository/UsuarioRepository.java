package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);
    List<Usuario> findAllByOrderByCreadoEnDesc();

    boolean existsByCorreo(String correo);

    long countByRol(RolUsuario rol);
}
