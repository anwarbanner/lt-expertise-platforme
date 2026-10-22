package ma.ltexpertise.utilisateur;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Compte applicatif correspondant à l'utilisateur authentifié de la requête. */
@Component
public class UtilisateurCourant {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurCourant(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    /** @throws AccessDeniedException si aucun compte actif ne correspond (réponse 403) */
    public Utilisateur obtenir() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null || !authentification.isAuthenticated()) {
            throw new AccessDeniedException("Utilisateur non authentifié.");
        }
        return utilisateurs.findByIdentifiant(authentification.getName())
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new AccessDeniedException("Compte applicatif introuvable ou désactivé."));
    }
}
