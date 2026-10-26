package ma.ltexpertise.securite;

import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;

/** Utilisateur connecté, tel que l'interface l'affiche. */
public record UtilisateurConnecte(String identifiant, String nom, String prenom, Role role) {

    static UtilisateurConnecte de(Utilisateur u) {
        return new UtilisateurConnecte(u.getIdentifiant(), u.getNom(), u.getPrenom(), u.getRole());
    }
}
