package co.edu.unbosque.casitago.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;

import java.util.List;

/**
 * RF-07: restringe el acceso a funcionalidades según el rol del usuario
 * autenticado. Este módulo (auth) solo configura la infraestructura:
 * qué rutas son públicas (registro/login/recuperación) y cuáles requieren
 * un JWT válido. La restricción fina por rol para cada RF de negocio se
 * aplica con @PreAuthorize("hasRole('...')") directamente en cada
 * controller del módulo correspondiente (Listing*, Booking*, Admin*, etc.).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Orígenes (dominios del frontend) que pueden llamar a la API, separados por comas.
     * Se configura con la variable de entorno CORS_ORIGINS; admite comodines
     * (por ejemplo https://*.onrender.com).
     */
    @Value("${cors.origenes-permitidos}")
    private List<String> origenesPermitidos;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // RNF-02: contraseñas cifradas
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            co.edu.unbosque.casitago.repository.UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(correo ->
                usuarioRepository.findByCorreo(correo)
                        .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException(
                                "Credenciales inválidas")));
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // API stateless con JWT, sin sesiones ni cookies
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(
                                "/api/auth/registro",
                                "/api/auth/login",
                                "/api/auth/recuperacion/**"
                        ).permitAll()
                        // Lecturas publicas: cualquier visitante puede explorar sin iniciar sesion.
                        .requestMatchers(HttpMethod.GET,
                                "/api/publicaciones/buscar",
                                "/api/publicaciones/*/detalle",
                                "/api/publicaciones/*/ubicacion",
                                "/api/publicaciones/*/resenas",
                                "/api/publicaciones/*/disponibilidad"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll() // config/openapi, ver RNF-10
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origenesPermitidos.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
