package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.Publicacion;
import co.edu.unbosque.casitago.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PublicacionRepository extends JpaRepository<Publicacion, UUID> {

    List<Publicacion> findByAnfitrion(Usuario anfitrion);

    List<Publicacion> findByAnfitrionAndEstado(Usuario anfitrion, EstadoPublicacion estado);
}