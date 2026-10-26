package ma.ltexpertise.securite;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Sécurité de l'API (F11, P9, §8) : API sans état, authentifiée par jeton JWT signé (HS256) transmis dans
 * l'en-tête Authorization ; droits par rôle vérifiés côté serveur sur chaque appel ({@code @PreAuthorize}).
 * Pas de protection CSRF sur l'API : aucun cookie n'y authentifie une requête, sauf le cookie de rafraîchissement,
 * limité à /api/auth et marqué SameSite=Strict.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class ConfigurationSecurite {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationSecurite.class);
    private static final int LONGUEUR_MIN_CLE = 32;

    @Bean
    SecurityFilterChain filtreSecurite(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acces -> acces
                        .requestMatchers(HttpMethod.POST, "/api/auth/connexion", "/api/auth/rafraichir",
                                "/api/auth/deconnexion").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(serveur -> serveur.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    PasswordEncoder encodeurMotDePasse() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey cleJetons(ProprietesSecurite proprietes) {
        String secret = proprietes.jwtSecret();
        if (secret == null || secret.isBlank()) {
            LOG.warn("lt.securite.jwt-secret non défini : clé aléatoire, les jetons d'accès ne survivent pas "
                    + "à un redémarrage (les sessions se rétablissent par rafraîchissement). À définir en production.");
            byte[] aleatoire = new byte[LONGUEUR_MIN_CLE];
            new SecureRandom().nextBytes(aleatoire);
            return new SecretKeySpec(aleatoire, "HmacSHA256");
        }
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < LONGUEUR_MIN_CLE) {
            throw new IllegalStateException("lt.securite.jwt-secret doit compter au moins " + LONGUEUR_MIN_CLE + " octets.");
        }
        return new SecretKeySpec(octets, "HmacSHA256");
    }

    @Bean
    JwtEncoder encodeurJetons(SecretKey cleJetons) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cleJetons));
    }

    @Bean
    JwtDecoder decodeurJetons(SecretKey cleJetons) {
        return NimbusJwtDecoder.withSecretKey(cleJetons).macAlgorithm(MacAlgorithm.HS256).build();
    }

    /** Le rôle porté par le jeton ({@code role}) devient l'autorité {@code ROLE_<ROLE>} utilisée par hasRole(). */
    @Bean
    JwtAuthenticationConverter convertisseurJetons() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName(ServiceJetons.REVENDICATION_ROLE);
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertisseur = new JwtAuthenticationConverter();
        convertisseur.setJwtGrantedAuthoritiesConverter(roles);
        return convertisseur;
    }
}
