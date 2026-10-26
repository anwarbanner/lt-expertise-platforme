package ma.ltexpertise.securite;

import ma.ltexpertise.utilisateur.ServiceUtilisateurs;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/** Ferme toutes les sessions d'un compte (désactivation, nouveau mot de passe). */
@Component
class RevocationSessionsJpa implements ServiceUtilisateurs.RevocationSessions {

    private final JetonRafraichissementRepository jetons;

    RevocationSessionsJpa(JetonRafraichissementRepository jetons) {
        this.jetons = jetons;
    }

    @Override
    public void revoquerTout(Utilisateur utilisateur, OffsetDateTime maintenant) {
        jetons.revoquerTout(utilisateur.getId(), maintenant);
    }
}
