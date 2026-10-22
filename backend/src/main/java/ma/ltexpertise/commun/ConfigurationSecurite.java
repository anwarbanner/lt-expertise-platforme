package ma.ltexpertise.commun;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Droits par rôle vérifiés côté serveur sur chaque appel (P9), via {@code @PreAuthorize}.
 * L'authentification JWT (F11) viendra compléter cette configuration.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class ConfigurationSecurite {
}
