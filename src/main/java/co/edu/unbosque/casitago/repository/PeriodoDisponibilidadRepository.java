package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.PeriodoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PeriodoDisponibilidadRepository extends JpaRepository<PeriodoDisponibilidad, UUID> {

    // Todos los periodos de una publicación, del más antiguo al más reciente
    List<PeriodoDisponibilidad> findByPublicacionIdOrderByFechaInicioAsc(UUID publicacionId);

    // Periodos BLOQUEADO o RESERVADO que se cruzan con el rango [inicio, fin)
    // Dos rangos se cruzan cuando: inicioExistente < finNuevo Y finExistente > inicioNuevo
    @Query("SELECT p FROM PeriodoDisponibilidad p "
            + "WHERE p.publicacion.id = :publicacionId "
            + "AND p.estado <> co.edu.unbosque.casitago.entity.EstadoPeriodo.DISPONIBLE "
            + "AND p.fechaInicio < :fin "
            + "AND p.fechaFin > :inicio")
    List<PeriodoDisponibilidad> buscarSolapados(@Param("publicacionId") UUID publicacionId,
                                                @Param("inicio") LocalDate inicio,
                                                @Param("fin") LocalDate fin);
}