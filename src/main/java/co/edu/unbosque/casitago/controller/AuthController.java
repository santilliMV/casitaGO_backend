package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.*;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // RF-01
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@Valid @RequestBody RegistroRequest request) {
        try {
            PerfilResponse perfil = authService.registrar(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(perfil);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-02
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse respuesta = authService.login(request);
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Correo o contraseña inválidos");
        }
    }

    // RF-03 (paso 1 de 2): solicitar el código
    @PostMapping("/recuperacion/solicitar")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        authService.solicitarRecuperacion(request);
        // Misma respuesta exista o no el correo, para no revelar qué correos están registrados.
        return ResponseEntity.ok("Si el correo existe, se envió un código de recuperación.");
    }

    // RF-03 (paso 2 de 2): confirmar código + nueva contraseña
    @PostMapping("/recuperacion/confirmar")
    public ResponseEntity<?> confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionRequest request) {
        try {
            authService.confirmarRecuperacion(request);
            return ResponseEntity.ok("Contraseña actualizada correctamente.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-04
    @GetMapping("/perfil")
    public ResponseEntity<?> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(authService.obtenerPerfil(usuario.getId()));
    }

    // RF-05
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody ActualizarPerfilRequest request
    ) {
        try {
            return ResponseEntity.ok(authService.actualizarPerfil(usuario.getId(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // RF-06
    @PatchMapping("/perfil/estado")
    public ResponseEntity<?> cambiarEstadoCuenta(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CambiarEstadoCuentaRequest request
    ) {
        return ResponseEntity.ok(authService.cambiarEstadoCuenta(usuario.getId(), request));
    }
}