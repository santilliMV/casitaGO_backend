package co.edu.unbosque.casitago.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Único punto de contacto con Stripe (modo test). Si Stripe falla o no está
 * disponible, lanza RuntimeException con un mensaje claro para que PagoService
 * deje la reserva en un estado consistente (RNF-14).
 */
@Service
public class PasarelaPagoStripe {

    private static final String MONEDA = "usd";

    private final String claveSecreta;

    public PasarelaPagoStripe(@Value("${stripe.secret-key}") String claveSecreta) {
        this.claveSecreta = claveSecreta;
    }

    // Cobra con un método de pago de prueba y devuelve el id del PaymentIntent.
    public String cobrar(BigDecimal monto, String metodoPago, String llaveIdempotencia) {
        validarClave();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(aCentavos(monto))
                .setCurrency(MONEDA)
                .setPaymentMethod(metodoPago)
                .setConfirm(true)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .setAllowRedirects(
                                        PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                .build())
                .build();

        try {
            PaymentIntent intento = PaymentIntent.create(params, opciones(llaveIdempotencia));
            if (!"succeeded".equals(intento.getStatus())) {
                throw new RuntimeException("El pago no fue aprobado (estado: " + intento.getStatus() + ")");
            }
            return intento.getId();
        } catch (StripeException e) {
            throw new RuntimeException("El proveedor de pagos rechazó o no pudo procesar el cobro: "
                    + e.getMessage());
        }
    }

    // Reembolsa (total o parcialmente) un cobro hecho antes con cobrar().
    public void reembolsar(String referenciaPago, BigDecimal monto) {
        validarClave();

        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(referenciaPago)
                .setAmount(aCentavos(monto))
                .build();

        try {
            Refund.create(params, opciones("reembolso-" + referenciaPago));
        } catch (StripeException e) {
            throw new RuntimeException("El proveedor de pagos no pudo procesar el reembolso: "
                    + e.getMessage());
        }
    }

    private void validarClave() {
        if (claveSecreta == null || claveSecreta.isBlank()) {
            throw new RuntimeException("El proveedor de pagos no está configurado");
        }
    }

    private RequestOptions opciones(String llaveIdempotencia) {
        return RequestOptions.builder()
                .setApiKey(claveSecreta)
                .setIdempotencyKey(llaveIdempotencia)
                .build();
    }

    // Stripe trabaja en la unidad mínima de la moneda (centavos).
    private long aCentavos(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }
}