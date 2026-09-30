package co.edu.unbosque.casitago.dto;

public class DemandaCiudadResponse {

    private String ciudad;
    private long reservas;

    public DemandaCiudadResponse() {
    }

    public DemandaCiudadResponse(String ciudad, long reservas) {
        this.ciudad = ciudad;
        this.reservas = reservas;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public long getReservas() {
        return reservas;
    }

    public void setReservas(long reservas) {
        this.reservas = reservas;
    }
}