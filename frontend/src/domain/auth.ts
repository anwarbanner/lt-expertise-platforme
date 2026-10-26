// Contrats de l'API d'authentification (ma.ltexpertise.securite) : /api/auth/connexion, rafraichir, deconnexion, moi.
import type { Role } from "./referentiels";

export interface UtilisateurConnecte {
  identifiant: string;
  nom: string;
  prenom: string;
  role: Role;
}

export interface ReponseConnexion {
  /** JWT à joindre à chaque appel ; gardé en mémoire uniquement (jamais dans le stockage du navigateur). */
  jetonAcces: string;
  /** Durée de validité du jeton d'accès, en secondes. */
  expireDans: number;
  utilisateur: UtilisateurConnecte;
}
