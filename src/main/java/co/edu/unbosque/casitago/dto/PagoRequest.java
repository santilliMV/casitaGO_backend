package co.edu.unbosque.casitago.dto;

import jakarta.validation.constraints.Pattern;

public class PagoRequest {

    // Opcional: identificador de método de pago de prueba de Stripe (ej. pm_card_visa,
    // pm_card_chargeDeclined). Si no se envía, se usa pm_card_visa.
    @Pattern(regexp = "^pm_[A-Za-z0-9_]+$",
            message = "El método de pago debe ser un identificador de prueba de Stripe (pm_...)")
    private String metodoPago;

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}