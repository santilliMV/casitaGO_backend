package co.edu.unbosque.casitago.dto;

import java.util.List;

public class ResenasPublicacionResponse {

    private double promedio;
    private int total;
    private List<ResenaResponse> resenas;

    public double getPromedio() {
        return promedio;
    }

    public void setPromedio(double promedio) {
        this.promedio = promedio;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<ResenaResponse> getResenas() {
        return resenas;
    }

    public void setResenas(List<ResenaResponse> resenas) {
        this.resenas = resenas;
    }
}