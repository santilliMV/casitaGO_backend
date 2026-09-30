package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.RestriccionRequest;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/usuarios/{id}/restringir")
    public ResponseEntity<Object> restringirUsuario(@PathVariable UUID id,
                                                    @Valid @RequestBody RestriccionRequest request,
                                                    @AuthenticationPrincipal Usuario admin) {
        try {
            return ResponseEntity.ok(adminService.restringirUsuario(id, admin, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/usuarios/{id}/levantar-restriccion")
    public ResponseEntity<Object> levantarRestriccion(@PathVariable UUID id,
                                                      @AuthenticationPrincipal Usuario admin) {
        try {
            return ResponseEntity.ok(adminService.levantarRestriccion(id, admin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/auditoria")
    public ResponseEntity<Object> consultarAuditoria(@RequestParam(required = false) String entidad,
                                                     @RequestParam(required = false) String accion,
                                                     @RequestParam(required = false) UUID usuarioId,
                                                     @AuthenticationPrincipal Usuario admin) {
        try {
            return ResponseEntity.ok(adminService.consultarAuditoria(admin, entidad, accion, usuarioId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}