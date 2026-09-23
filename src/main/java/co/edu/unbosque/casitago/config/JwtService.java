package co.edu.unbosque.casitago.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Emisión y validación de JWT.
 *
 * RNF-01 ("invalidar la sesión tras un periodo de inactividad") se resuelve
 * aquí con expiración corta del token (jwt.expiration-ms, default 30 min).
 * No hay refresh token todavía: cada request válido no renueva el vencimiento,
 * así que "inactividad" en la práctica es "tiempo desde el login". Si más
 * adelante se necesita expiración deslizante (renovar en cada request), lo
 * normal es agregar un endpoint /auth/refresh — lo dejamos pendiente porque
 * no está en el alcance de RF-01 a RF-07.
 */
@Component
public class JwtService {

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(
            @Value("${jwt.secret}") String secreto,
            @Value("${jwt.expiration-ms:1800000}") long expiracionMs
    ) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "jwt.secret debe estar definido y tener al menos 32 bytes (HS256). " +
                            "Configúralo en la variable de entorno JWT_SECRET.");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(UUID usuarioId, String correo, String rol) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiracionMs);

        return Jwts.builder()
                .subject(correo)
                .claims(Map.of("uid", usuarioId.toString(), "rol", rol))
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(clave)
                .compact();
    }

    public long getExpiracionSegundos() {
        return expiracionMs / 1000;
    }

    /** @return el correo (subject) si el token es válido; null si es inválido o expiró. */
    public String validarYObtenerCorreo(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}