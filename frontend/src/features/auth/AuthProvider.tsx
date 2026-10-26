import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import * as apiAuth from "@/api/auth";
import { definirJetonAcces, rafraichirSession, surExpirationSession } from "@/api/client";
import { AuthContext, type EtatAuth, type ValeurAuth } from "./AuthContext";

/**
 * Session de l'utilisateur. Au chargement de la page, la session est rétablie grâce au cookie de rafraîchissement
 * (rechargement, nouvel onglet) ; sans cookie valide, l'utilisateur est renvoyé vers la connexion.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [etat, setEtat] = useState<EtatAuth>({ statut: "chargement" });

  const fermerSession = useCallback(() => {
    definirJetonAcces(null);
    queryClient.clear();
    setEtat({ statut: "deconnecte" });
  }, [queryClient]);

  useEffect(() => {
    surExpirationSession(fermerSession);
    let actif = true;
    rafraichirSession()
      .then((session) => actif && setEtat({ statut: "connecte", utilisateur: session.utilisateur }))
      .catch(() => actif && setEtat({ statut: "deconnecte" }));
    return () => {
      actif = false;
    };
  }, [fermerSession]);

  const connecter = useCallback(async (identifiant: string, motDePasse: string) => {
    const session = await apiAuth.connexion(identifiant, motDePasse);
    definirJetonAcces(session.jetonAcces);
    setEtat({ statut: "connecte", utilisateur: session.utilisateur });
  }, []);

  const deconnecter = useCallback(async () => {
    try {
      await apiAuth.deconnexion();
    } finally {
      fermerSession();
    }
  }, [fermerSession]);

  const valeur = useMemo<ValeurAuth>(() => ({ etat, connecter, deconnecter }), [etat, connecter, deconnecter]);
  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>;
}
