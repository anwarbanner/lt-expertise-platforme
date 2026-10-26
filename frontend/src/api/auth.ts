import type { ReponseConnexion, UtilisateurConnecte } from "@/domain/auth";
import { api } from "./client";

export async function connexion(identifiant: string, motDePasse: string): Promise<ReponseConnexion> {
  return (await api.post<ReponseConnexion>("/auth/connexion", { identifiant, motDePasse })).data;
}

export async function deconnexion(): Promise<void> {
  await api.post("/auth/deconnexion");
}

export async function moi(): Promise<UtilisateurConnecte> {
  return (await api.get<UtilisateurConnecte>("/auth/moi")).data;
}
