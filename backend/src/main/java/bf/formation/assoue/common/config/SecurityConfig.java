package bf.formation.assoue.common.config;

import bf.formation.assoue.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuration de securite unique pour toute l'application (auparavant
 * 6 SecurityConfig quasi identiques, une par microservice).
 *
 * Consequence directe de la fusion : toutes les routes qui n'existaient que
 * pour des appels internes entre microservices (proteges avant par
 * InternalApiKeyFilter) ont ete supprimees, car ces appels sont desormais de
 * simples methodes Java (@Autowired) au lieu de requetes HTTP -- il n'y a
 * plus rien a proteger a cette frontiere. Seul le webhook operateur
 * (/paiements/webhook) reste public car il est appele depuis l'exterieur
 * (Orange Money / Moov Money) ; il est protege par une signature HMAC
 * (cf. WebhookSignatureVerifier), pas par l'authentification applicative.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Origines autorisees a appeler l'API depuis un navigateur (frontend Angular).
    // "ng serve" ecoute par defaut sur 4200 ; ajouter d'autres origines via la
    // variable d'environnement CORS_ALLOWED_ORIGINS (liste separee par des virgules)
    // pour un deploiement (ex: https://app.assoue.bf).
    @Value("${cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    // BNF Securite : bcrypt avec salt factor >= 10 (cf. CDC section 4).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Le frontend Angular tourne sur une autre origine (port 4200) que l'API
    // (port 8080) : sans cette configuration, le navigateur bloque toutes les
    // requetes AJAX (y compris le preflight OPTIONS) avant meme qu'elles
    // n'atteignent Spring Security.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Content-Disposition")); // pour les exports CSV
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .anonymous(anon -> anon.disable())
            .authorizeHttpRequests(auth -> auth
                // --- Public, sans authentification ---
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/produits/**").permitAll() // BF-SHOP-01 : catalogue public
                .requestMatchers("/paiements/webhook").permitAll()           // signature HMAC, cf. classe ci-dessus
                .requestMatchers(HttpMethod.GET, "/photos-signalements/**").permitAll() // <img src> ne porte pas de JWT
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", "/actuator/health").permitAll()

                // --- Reserve a l'ADMIN (back-office) ---
                // Regle specifique AVANT la regle publique plus large sur /formations/**
                // (Spring Security applique la premiere regle qui matche).
                .requestMatchers(HttpMethod.GET, "/formations/*/inscrits").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/formations").hasRole("ADMIN")
                .requestMatchers("/entreprises/**").hasRole("ADMIN")
                .requestMatchers("/produits/**").hasRole("ADMIN") // POST/PUT/DELETE (GET deja permitAll ci-dessus)
                .requestMatchers("/commandes/admin/**", "/commandes/export").hasRole("ADMIN")
                .requestMatchers("/notifications/**").hasRole("ADMIN")
                .requestMatchers("/signalements/export", "/signalements/admin/**").hasRole("ADMIN")

                // --- Public : formations consultables sans etre connecte, comme le catalogue ---
                .requestMatchers(HttpMethod.GET, "/formations/**").permitAll()

                // --- Reserve aux comptes ENTREPRISE (marketplace des signalements) ---
                .requestMatchers("/signalements/marketplace", "/signalements/*/prendre-en-charge",
                        "/signalements/*/collecter").hasRole("ENTREPRISE")

                // --- Le reste necessite d'etre connecte (CITOYEN, ENTREPRISE ou ADMIN) ---
                // Couvre notamment POST /formations/{id}/inscription et GET /formations/mes-inscriptions.
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
