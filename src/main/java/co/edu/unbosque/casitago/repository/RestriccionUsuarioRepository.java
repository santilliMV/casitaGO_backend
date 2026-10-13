package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.RestriccionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestriccionUsuarioRepository extends JpaRepository<RestriccionUsuario, UUID> {

    Optional<RestriccionUsuario> findByUsuarioIdAndActivaTrue(UUID usuarioId);

    List<RestriccionUsuario> findByActivaTrue();

    boolean existsByUsuarioIdAndActivaTrue(UUID usuarioId);
}