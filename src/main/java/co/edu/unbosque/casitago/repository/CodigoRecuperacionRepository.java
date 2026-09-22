package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.CodigoRecuperacion;
import co.edu.unbosque.casitago.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, UUID> {

    // El más reciente primero: si un usuario pide varios códigos, solo
    // el último emitido debe considerarse válido en la práctica.
    Optional<CodigoRecuperacion> findFirstByUsuarioAndCodigoOrderByExpiraEnDesc(Usuario usuario, String codigo);
}
