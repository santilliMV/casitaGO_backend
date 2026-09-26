package co.edu.unbosque.casitago.repository;

import co.edu.unbosque.casitago.entity.EstadoPeriodo;
import co.edu.unbosque.casitago.entity.PeriodoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PeriodoDisponibilidadRepository extends JpaRepository<PeriodoDisponibilidad, UUID> {

    List<PeriodoDisponibilidad> findByPublicacionIdOrderByFechaInicioAsc(UUID publicacionId);

    default List<PeriodoDisponibilidad> buscarSolapados(UUID publicacionId, LocalDate inicio, LocalDate fin) {
        return buscarSolapadosInterno(publicacionId, inicio, fin, EstadoPeriodo.DISPONIBLE);
    }

    @Query("SELECT p FROM PeriodoDisponibilidad p "
            + "WHERE p.publicacion.id = :publicacionId "
            + "AND p.estado <> :disponible "
            + "AND p.fechaInicio < :fin "
            + "AND p.fechaFin > :inicio")
    List<PeriodoDisponibilidad> buscarSolapadosInterno(@Param("publicacionId") UUID publicacionId,
                                                       @Param("inicio") LocalDate inicio,
                                                       @Param("fin") LocalDate fin,
                                                       @Param("disponible") EstadoPeriodo disponible);
}