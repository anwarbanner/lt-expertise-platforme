package ma.ltexpertise.commun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Optional;

/** Remplit automatiquement les colonnes cree_le / cree_par / modifie_le / modifie_par. */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditeurCourant", dateTimeProviderRef = "horodatageCourant")
public class ConfigurationJpa {

    /** Auteur des écritures faites hors requête utilisateur (imports en lot, synchronisation Excel). */
    public static final String AUTEUR_SYSTEME = "systeme";

    @Bean
    AuditorAware<String> auditeurCourant() {
        return () -> {
            Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
            if (authentification == null || !authentification.isAuthenticated()
                    || authentification instanceof AnonymousAuthenticationToken) {
                return Optional.of(AUTEUR_SYSTEME);
            }
            return Optional.of(authentification.getName());
        };
    }

    @Bean
    DateTimeProvider horodatageCourant() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
