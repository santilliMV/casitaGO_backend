package co.edu.unbosque.casitago.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RNF-10: documentar los contratos de los servicios mediante OpenAPI.
 * springdoc-openapi genera el documento automáticamente a partir de los
 * @RestController existentes; esta clase solo agrega metadata general y
 * el esquema de autenticación Bearer para que "Authorize" en Swagger UI
 * funcione con los JWT emitidos por /api/auth/login.
 *
 * Una vez agregada la dependencia (ver pom-dependencias-auth.xml), queda
 * disponible en:
 *   /v3/api-docs       -> el contrato en JSON
 *   /swagger-ui.html    -> la UI interactiva
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI casitaGoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CasitaGO API")
                        .description("Marketplace de Alojamientos — Ingeniería de Software, Universidad El Bosque")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
