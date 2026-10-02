package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.dto.PagoRequest;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reservas/{reservaId}")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    // RF-35
    @PostMapping("/pagar")
    public ResponseEntity<Object> pagar(@PathVariable UUID reservaId,
                                        @Valid @RequestBody(required = false) PagoRequest request,
                                        @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(pagoService.pagar(reservaId, usuario, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/pagos")
    public ResponseEntity<Object> listarPagos(@PathVariable UUID reservaId,
                                              @AuthenticationPrincipal Usuario usuario) {
        try {
            return ResponseEntity.ok(pagoService.listarPorReserva(reservaId, usuario));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}