import { createContext, useContext } from "react";
import type { UtilisateurConnecte } from "@/domain/auth";

export type EtatAuth =
  | { statut: "chargement" }
  | { statut: "deconnecte" }
  | { statut: "connecte"; utilisateur: UtilisateurConnecte };

export interface ValeurAuth {
  etat: EtatAuth;
  connecter: (identifiant: string, motDePasse: string) => Promise<void>;
  deconnecter: () => Promise<void>;
}

export const AuthContext = createContext<ValeurAuth | null>(null);

export function useAuth(): ValeurAuth {
  const valeur = useContext(AuthContext);
  if (!valeur) throw new Error("useAuth doit être utilisé dans un AuthProvider");
  return valeur;
}

/** Utilisateur connecté ; à n'utiliser que sous une route protégée. */
export function useUtilisateur(): UtilisateurConnecte {
  const { etat } = useAuth();
  if (etat.statut !== "connecte") throw new Error("Aucun utilisateur connecté");
  return etat.utilisateur;
}
