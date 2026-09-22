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
    public ResponseEntity<PerfilResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    // RF-02
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // RF-03 (paso 1 de 2): solicitar el código
    @PostMapping("/recuperacion/solicitar")
    public ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request) {
        authService.solicitarRecuperacion(request);
        // Respuesta 202 idéntica exista o no el correo, para no filtrar qué
        // correos están registrados.
        return ResponseEntity.accepted().build();
    }

    // RF-03 (paso 2 de 2): confirmar código + nueva contraseña
    @PostMapping("/recuperacion/confirmar")
    public ResponseEntity<Void> confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionRequest request) {
        authService.confirmarRecuperacion(request);
        return ResponseEntity.noContent().build();
    }

    // RF-04
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> obtenerPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(authService.obtenerPerfil(usuario.getId()));
    }

    // RF-05
    @PutMapping("/perfil")
    public ResponseEntity<PerfilResponse> actualizarPerfil(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody ActualizarPerfilRequest request
    ) {
        return ResponseEntity.ok(authService.actualizarPerfil(usuario.getId(), request));
    }

    // RF-06
    @PatchMapping("/perfil/estado")
    public ResponseEntity<PerfilResponse> cambiarEstadoCuenta(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CambiarEstadoCuentaRequest request
    ) {
        return ResponseEntity.ok(authService.cambiarEstadoCuenta(usuario.getId(), request));
    }
}
