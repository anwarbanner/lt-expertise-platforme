import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { vi } from "vitest";
import { RoutesApplication } from "@/app/App";
import type { UtilisateurConnecte } from "@/domain/auth";
import { AuthContext, type EtatAuth, type ValeurAuth } from "@/features/auth/AuthContext";

export const GESTIONNAIRE: UtilisateurConnecte = { identifiant: "gestionnaire", nom: "Benali", prenom: "Samira", role: "GESTIONNAIRE" };
export const CONSULTANT: UtilisateurConnecte = { identifiant: "consultant", nom: "Alaoui", prenom: "Nadia", role: "CONSULTATION" };
export const ADMINISTRATEUR: UtilisateurConnecte = { identifiant: "admin", nom: "El Khatib", prenom: "Amine", role: "ADMINISTRATEUR" };

/** Rend l'application à l'adresse donnée, avec une session simulée (aucun appel d'authentification). */
export function rendreApplication(adresse: string, utilisateur: UtilisateurConnecte | null, etat?: EtatAuth) {
  const auth: ValeurAuth = {
    etat: etat ?? (utilisateur ? { statut: "connecte", utilisateur } : { statut: "deconnecte" }),
    connecter: vi.fn().mockResolvedValue(undefined),
    deconnecter: vi.fn().mockResolvedValue(undefined),
  };
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  const rendu = render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={auth}>
        <MemoryRouter initialEntries={[adresse]}><RoutesApplication /></MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>,
  );
  return { auth, ...rendu };
}
