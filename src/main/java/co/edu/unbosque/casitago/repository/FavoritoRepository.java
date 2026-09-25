package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    List<Favorito> findByUsuarioId(UUID usuarioId);

    Optional<Favorito> findByUsuarioIdAndPublicacionId(UUID usuarioId, UUID publicacionId);

    boolean existsByUsuarioIdAndPublicacionId(UUID usuarioId, UUID publicacionId);
}