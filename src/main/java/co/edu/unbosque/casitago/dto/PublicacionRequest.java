package co.edu.unbosque.casitago.dto;

import java.util.List;

public class PublicacionRequest extends DatosPublicacion {

    private List<String> servicios;

    private List<String> reglas;

    public List<String> getServicios() {
        return servicios;
    }

    public void setServicios(List<String> servicios) {
        this.servicios = servicios;
    }

    public List<String> getReglas() {
        return reglas;
    }

    public void setReglas(List<String> reglas) {
        this.reglas = reglas;
    }
}