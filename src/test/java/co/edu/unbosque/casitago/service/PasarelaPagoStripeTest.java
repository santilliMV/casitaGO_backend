package co.edu.unbosque.casitago.service;

import com.stripe.exception.ApiConnectionException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PasarelaPagoStripeTest {

    private final PasarelaPagoStripe pasarela = new PasarelaPagoStripe("sk_test_falsa");

    @Test
    void cobrar_conPagoAprobado_devuelveElIdDelCobro() {
        PaymentIntent intento = mock(PaymentIntent.class);
        when(intento.getStatus()).thenReturn("succeeded");
        when(intento.getId()).thenReturn("pi_123");

        try (MockedStatic<PaymentIntent> stripe = mockStatic(PaymentIntent.class)) {
            stripe.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(intento);

            String referencia = pasarela.cobrar(new BigDecimal("150.50"), "pm_card_visa", "pago-1");

            assertEquals("pi_123", referencia);
        }
    }

    @Test
    void cobrar_siElEstadoNoEsSucceeded_lanzaRuntimeException() {
        PaymentIntent intento = mock(PaymentIntent.class);
        when(intento.getStatus()).thenReturn("requires_action");

        try (MockedStatic<PaymentIntent> stripe = mockStatic(PaymentIntent.class)) {
            stripe.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(intento);

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> pasarela.cobrar(new BigDecimal("150.50"), "pm_card_visa", "pago-1"));

            assertTrue(error.getMessage().contains("requires_action"));
        }
    }

    @Test
    void cobrar_siStripeNoEstaDisponible_lanzaRuntimeException() {
        try (MockedStatic<PaymentIntent> stripe = mockStatic(PaymentIntent.class)) {
            stripe.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(new ApiConnectionException("sin conexión"));

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> pasarela.cobrar(new BigDecimal("150.50"), "pm_card_visa", "pago-1"));

            assertTrue(error.getMessage().contains("proveedor de pagos"));
        }
    }

    @Test
    void cobrar_sinClaveConfigurada_lanzaRuntimeException() {
        PasarelaPagoStripe sinClave = new PasarelaPagoStripe("");

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> sinClave.cobrar(new BigDecimal("10"), "pm_card_visa", "pago-1"));

        assertEquals("El proveedor de pagos no está configurado", error.getMessage());
    }

    @Test
    void reembolsar_conStripeDisponible_noLanzaError() {
        Refund reembolso = mock(Refund.class);

        try (MockedStatic<Refund> stripe = mockStatic(Refund.class)) {
            stripe.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                    .thenReturn(reembolso);

            pasarela.reembolsar("pi_123", new BigDecimal("150.50"));
        }
    }

    @Test
    void reembolsar_siStripeNoEstaDisponible_lanzaRuntimeException() {
        try (MockedStatic<Refund> stripe = mockStatic(Refund.class)) {
            stripe.when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
                    .thenThrow(new ApiConnectionException("sin conexión"));

            RuntimeException error = assertThrows(RuntimeException.class,
                    () -> pasarela.reembolsar("pi_123", new BigDecimal("150.50")));

            assertTrue(error.getMessage().contains("reembolso"));
        }
    }

    @Test
    void reembolsar_sinClaveConfigurada_lanzaRuntimeException() {
        PasarelaPagoStripe sinClave = new PasarelaPagoStripe(null);

        assertThrows(RuntimeException.class,
                () -> sinClave.reembolsar("pi_123", new BigDecimal("10")));
    }
}