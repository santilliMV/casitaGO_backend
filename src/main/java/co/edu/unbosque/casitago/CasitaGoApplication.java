package co.edu.unbosque.casitago;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend del Marketplace de Alojamientos (Inversiones LR).
 * <p>
 * Arquitectura: monolito modular. Cada paquete de primer nivel bajo
 * {@code co.edu.unbosque.casitago} corresponde a un "servicio de software"
 * documentado en el diseño orientado a servicios (Confluence), con responsabilidad
 * y datos delimitados, aunque todos corran dentro del mismo proceso/JAR por decisión
 * de equipo para el MVP académico.
 */
@SpringBootApplication
public class CasitaGoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CasitaGoApplication.class, args);
    }
}
