package co.edu.unbosque.casitago.controller;

import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.service.ReporteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class ReporteController {

    @Autowired
    private ReporteService reporteService;

    @GetMapping("/dashboard")
    public ResponseEntity<Object> obtenerDashboard(@AuthenticationPrincipal Usuario admin) {
        try {
            return ResponseEntity.ok(reporteService.obtenerDashboard(admin));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reportes/gestion")
    public ResponseEntity<Object> descargarReporteGestion(@AuthenticationPrincipal Usuario admin) {
        try {
            byte[] pdf = reporteService.generarReportePdf(admin);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte-gestion.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}