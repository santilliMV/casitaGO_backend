package co.edu.unbosque.casitago.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private long totalUsuarios;
    private long usuariosHuespedes;
    private long usuariosAnfitriones;
    private long usuariosAdministradores;
    private long publicacionesActivas;
    private Map<String, Long> reservasPorEstado;
    private BigDecimal ingresosEstimados;
    private List<DemandaCiudadResponse> ciudadesConMayorDemanda;

    public long getTotalUsuarios() {
        return totalUsuarios;
    }

    public void setTotalUsuarios(long totalUsuarios) {
        this.totalUsuarios = totalUsuarios;
    }

    public long getUsuariosHuespedes() {
        return usuariosHuespedes;
    }

    public void setUsuariosHuespedes(long usuariosHuespedes) {
        this.usuariosHuespedes = usuariosHuespedes;
    }

    public long getUsuariosAnfitriones() {
        return usuariosAnfitriones;
    }

    public void setUsuariosAnfitriones(long usuariosAnfitriones) {
        this.usuariosAnfitriones = usuariosAnfitriones;
    }

    public long getUsuariosAdministradores() {
        return usuariosAdministradores;
    }

    public void setUsuariosAdministradores(long usuariosAdministradores) {
        this.usuariosAdministradores = usuariosAdministradores;
    }

    public long getPublicacionesActivas() {
        return publicacionesActivas;
    }

    public void setPublicacionesActivas(long publicacionesActivas) {
        this.publicacionesActivas = publicacionesActivas;
    }

    public Map<String, Long> getReservasPorEstado() {
        return reservasPorEstado;
    }

    public void setReservasPorEstado(Map<String, Long> reservasPorEstado) {
        this.reservasPorEstado = reservasPorEstado;
    }

    public BigDecimal getIngresosEstimados() {
        return ingresosEstimados;
    }

    public void setIngresosEstimados(BigDecimal ingresosEstimados) {
        this.ingresosEstimados = ingresosEstimados;
    }

    public List<DemandaCiudadResponse> getCiudadesConMayorDemanda() {
        return ciudadesConMayorDemanda;
    }

    public void setCiudadesConMayorDemanda(List<DemandaCiudadResponse> ciudadesConMayorDemanda) {
        this.ciudadesConMayorDemanda = ciudadesConMayorDemanda;
    }
}