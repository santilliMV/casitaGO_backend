package co.edu.unbosque.casitago.dto;

import co.edu.unbosque.casitago.entity.PeriodoDisponibilidad;

import java.time.LocalDate;
import java.util.UUID;

public class PeriodoResponse {

    private UUID id;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estado;

    public static PeriodoResponse desde(PeriodoDisponibilidad periodo) {
        PeriodoResponse response = new PeriodoResponse();
        response.setId(periodo.getId());
        response.setFechaInicio(periodo.getFechaInicio());
        response.setFechaFin(periodo.getFechaFin());
        response.setEstado(periodo.getEstado().name());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}