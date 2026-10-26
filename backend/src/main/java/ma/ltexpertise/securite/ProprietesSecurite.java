package ma.ltexpertise.securite;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Propriétés {@code lt.securite.*} (§8 du cahier des charges).
 *
 * @param jwtSecret          clé HMAC des jetons d'accès (au moins 32 caractères) ; vide : clé aléatoire à chaque
 *                           démarrage, les jetons d'accès émis avant un redémarrage deviennent invalides
 * @param dureeJetonAcces    durée de vie d'un jeton d'accès
 * @param dureeInactivite    déconnexion automatique après cette durée sans requête
 * @param dureeSessionMax    durée maximale d'une session, activité comprise
 * @param cookieSecurise     cookie de rafraîchissement réservé à HTTPS ({@code true} en production)
 * @param tentativesMax      échecs de connexion avant verrouillage temporaire
 * @param dureeVerrouillage  durée du verrouillage temporaire
 * @param motDePasseInitial  mot de passe des comptes créés au premier démarrage (base vide), à changer
 */
@Validated
@ConfigurationProperties("lt.securite")
public record ProprietesSecurite(
        @DefaultValue("") String jwtSecret,
        @DefaultValue("10m") @NotNull Duration dureeJetonAcces,
        @DefaultValue("30m") @NotNull Duration dureeInactivite,
        @DefaultValue("10h") @NotNull Duration dureeSessionMax,
        @DefaultValue("true") boolean cookieSecurise,
        @DefaultValue("5") @Min(1) int tentativesMax,
        @DefaultValue("15m") @NotNull Duration dureeVerrouillage,
        @NotBlank String motDePasseInitial) {
}
