import { api } from "@/api/client";
import type { ImportFichier, RecapitulatifLot } from "@/domain/imports";

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Dépôt d'un ou plusieurs PDF : un seul fichier est traité aussitôt, un lot en arrière-plan. */
export async function importerFichiers(fichiers: File[]): Promise<RecapitulatifLot> {
  const formulaire = new FormData();
  fichiers.forEach((fichier) => formulaire.append("fichiers", fichier));
  return (await api.post<RecapitulatifLot>("/imports", formulaire)).data;
}

export async function recapitulatifLot(lot: string): Promise<RecapitulatifLot> {
  return (await api.get<RecapitulatifLot>(`/imports/${lot}`)).data;
}

export async function historiqueImports(refusesSeulement: boolean, taille = 10): Promise<Page<ImportFichier>> {
  return (await api.get<Page<ImportFichier>>("/imports", { params: { refuses: refusesSeulement, size: taille } })).data;
}

export const clesImports = {
  tout: ["imports"] as const,
  historique: (refuses: boolean) => ["imports", "historique", refuses] as const,
  lot: (lot: string | null) => ["imports", "lot", lot] as const,
};
