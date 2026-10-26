import { QueryClient } from "@tanstack/react-query";
import axios from "axios";

/** Pas de nouvel essai sur une erreur de droits ou de validation : seule une panne réseau ou serveur se retente. */
export function creerQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        retry: (essais, erreur) => {
          const statut = axios.isAxiosError(erreur) ? erreur.response?.status : undefined;
          return essais < 2 && (statut === undefined || statut >= 500);
        },
      },
    },
  });
}
