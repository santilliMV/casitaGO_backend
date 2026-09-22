package co.edu.unbosque.casitago.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String mensaje,
        List<String> detalles
) {
    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, mensaje, List.of());
    }

    public static ErrorResponse de(int status, String error, String mensaje, List<String> detalles) {
        return new ErrorResponse(OffsetDateTime.now(), status, error, mensaje, detalles);
    }
}
