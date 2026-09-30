package co.edu.unbosque.casitago.service;

import co.edu.unbosque.casitago.dto.DashboardResponse;
import co.edu.unbosque.casitago.dto.DemandaCiudadResponse;
import co.edu.unbosque.casitago.entity.EstadoPublicacion;
import co.edu.unbosque.casitago.entity.EstadoReserva;
import co.edu.unbosque.casitago.entity.RolUsuario;
import co.edu.unbosque.casitago.entity.Usuario;
import co.edu.unbosque.casitago.repository.PublicacionRepository;
import co.edu.unbosque.casitago.repository.ReservaRepository;
import co.edu.unbosque.casitago.repository.UsuarioRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteService {

    private static final int MAXIMO_CIUDADES = 5;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Font FUENTE_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FUENTE_SECCION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FUENTE_ENCABEZADO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FUENTE_TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 10);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PublicacionRepository publicacionRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    // ---------- RF-27: dashboard ----------
    public DashboardResponse obtenerDashboard(Usuario admin) {
        verificarAdministrador(admin);

        DashboardResponse dashboard = new DashboardResponse();
        dashboard.setTotalUsuarios(usuarioRepository.count());
        dashboard.setUsuariosHuespedes(usuarioRepository.countByRol(RolUsuario.HUESPED));
        dashboard.setUsuariosAnfitriones(usuarioRepository.countByRol(RolUsuario.ANFITRION));
        dashboard.setUsuariosAdministradores(usuarioRepository.countByRol(RolUsuario.ADMINISTRADOR));
        dashboard.setPublicacionesActivas(publicacionRepository.countByEstado(EstadoPublicacion.ACTIVA));

        Map<String, Long> reservasPorEstado = new LinkedHashMap<>();
        for (EstadoReserva estado : EstadoReserva.values()) {
            reservasPorEstado.put(estado.name(), reservaRepository.countByEstado(estado));
        }
        dashboard.setReservasPorEstado(reservasPorEstado);

        BigDecimal ingresos = reservaRepository.sumarIngresos(
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.COMPLETADA));
        if (ingresos == null) {
            ingresos = BigDecimal.ZERO;
        }
        dashboard.setIngresosEstimados(ingresos.setScale(2, RoundingMode.HALF_UP));

        List<DemandaCiudadResponse> ciudades = new ArrayList<>();
        List<Object[]> filas = reservaRepository.ciudadesConMasReservas(
                EstadoReserva.CANCELADA, PageRequest.of(0, MAXIMO_CIUDADES));
        for (Object[] fila : filas) {
            String ciudad = (String) fila[0];
            if (ciudad == null || ciudad.isBlank()) {
                ciudad = "Sin ciudad";
            }
            ciudades.add(new DemandaCiudadResponse(ciudad, ((Number) fila[1]).longValue()));
        }
        dashboard.setCiudadesConMayorDemanda(ciudades);

        return dashboard;
    }

    // ---------- RF-28: reporte de gestión en PDF ----------
    public byte[] generarReportePdf(Usuario admin) {
        DashboardResponse dashboard = obtenerDashboard(admin);

        Document documento = new Document(PageSize.A4);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Reporte de gestión - CasitaGO", FUENTE_TITULO));
            documento.add(new Paragraph("Generado: " + LocalDateTime.now().format(FORMATO_FECHA), FUENTE_TEXTO));
            documento.add(new Paragraph(" "));

            List<String[]> usuarios = new ArrayList<>();
            usuarios.add(new String[]{"Total de usuarios", String.valueOf(dashboard.getTotalUsuarios())});
            usuarios.add(new String[]{"Huéspedes", String.valueOf(dashboard.getUsuariosHuespedes())});
            usuarios.add(new String[]{"Anfitriones", String.valueOf(dashboard.getUsuariosAnfitriones())});
            usuarios.add(new String[]{"Administradores", String.valueOf(dashboard.getUsuariosAdministradores())});
            agregarSeccion(documento, "Usuarios", "Indicador", "Cantidad", usuarios);

            List<String[]> publicaciones = new ArrayList<>();
            publicaciones.add(new String[]{"Publicaciones activas", String.valueOf(dashboard.getPublicacionesActivas())});
            agregarSeccion(documento, "Publicaciones", "Indicador", "Cantidad", publicaciones);

            List<String[]> reservas = new ArrayList<>();
            for (Map.Entry<String, Long> entrada : dashboard.getReservasPorEstado().entrySet()) {
                reservas.add(new String[]{entrada.getKey(), String.valueOf(entrada.getValue())});
            }
            agregarSeccion(documento, "Reservas por estado", "Estado", "Cantidad", reservas);

            List<String[]> ingresos = new ArrayList<>();
            ingresos.add(new String[]{"Ingresos estimados", dashboard.getIngresosEstimados().toPlainString()});
            agregarSeccion(documento, "Ingresos", "Indicador", "Valor", ingresos);

            List<String[]> demanda = new ArrayList<>();
            for (DemandaCiudadResponse ciudad : dashboard.getCiudadesConMayorDemanda()) {
                demanda.add(new String[]{ciudad.getCiudad(), String.valueOf(ciudad.getReservas())});
            }
            agregarSeccion(documento, "Ciudades con mayor demanda", "Ciudad", "Reservas", demanda);

            documento.close();
        } catch (DocumentException e) {
            throw new RuntimeException("No se pudo generar el reporte PDF");
        }

        return salida.toByteArray();
    }

    private void agregarSeccion(Document documento, String titulo, String encabezadoUno,
                                String encabezadoDos, List<String[]> filas) throws DocumentException {
        documento.add(new Paragraph(titulo, FUENTE_SECCION));

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(6);
        tabla.setSpacingAfter(14);

        tabla.addCell(crearCelda(encabezadoUno, FUENTE_ENCABEZADO));
        tabla.addCell(crearCelda(encabezadoDos, FUENTE_ENCABEZADO));
        for (String[] fila : filas) {
            tabla.addCell(crearCelda(fila[0], FUENTE_TEXTO));
            tabla.addCell(crearCelda(fila[1], FUENTE_TEXTO));
        }

        documento.add(tabla);
    }

    private PdfPCell crearCelda(String texto, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setPadding(5);
        return celda;
    }

    private void verificarAdministrador(Usuario usuario) {
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR) {
            throw new RuntimeException("Solo un ADMINISTRADOR puede realizar esta acción");
        }
    }
}