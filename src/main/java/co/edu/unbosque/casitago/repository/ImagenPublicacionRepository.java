package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.ImagenPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImagenPublicacionRepository extends JpaRepository<ImagenPublicacion, UUID> {
}