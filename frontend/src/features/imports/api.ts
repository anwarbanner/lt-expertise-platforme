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

/**
 * Limites d'un envoi : le serveur refuse (413) une requête de plus de 100 parties (server.tomcat.max-part-count) ou de
 * plus de 200 Mo (max-request-size, client_max_body_size de nginx). Un gros dépôt part donc en plusieurs envois.
 */
export const FICHIERS_PAR_ENVOI = 40;
export const OCTETS_PAR_ENVOI = 100 * 1024 * 1024;

/** Découpe un dépôt en envois d'au plus 40 fichiers et 100 Mo, dans l'ordre du dépôt. */
export function decouperEnEnvois(fichiers: File[]): File[][] {
  const envois: File[][] = [];
  let courant: File[] = [];
  let taille = 0;
  for (const fichier of fichiers) {
    if (courant.length > 0 && (courant.length >= FICHIERS_PAR_ENVOI || taille + fichier.size > OCTETS_PAR_ENVOI)) {
      envois.push(courant);
      courant = [];
      taille = 0;
    }
    courant.push(fichier);
    taille += fichier.size;
  }
  if (courant.length > 0) envois.push(courant);
  return envois;
}

/** Récapitulatif unique d'un dépôt envoyé en plusieurs lots : compteurs additionnés, résultats dans l'ordre. */
export function fusionnerLots(lots: RecapitulatifLot[]): RecapitulatifLot | undefined {
  if (lots.length <= 1) return lots[0];
  const somme = (cle: "total" | "traites" | "reussis" | "aCompleter" | "aVerifier" | "pretsAValider" | "refuses" | "enErreur") =>
    lots.reduce((t, l) => t + l[cle], 0);
  const enCours = lots.some((l) => l.statut === "EN_COURS");
  return {
    lot: lots[0].lot, statut: enCours ? "EN_COURS" : "TERMINE", total: somme("total"), traites: somme("traites"),
    reussis: somme("reussis"), aCompleter: somme("aCompleter"), aVerifier: somme("aVerifier"),
    pretsAValider: somme("pretsAValider"), refuses: somme("refuses"), enErreur: somme("enErreur"),
    creeLe: lots[0].creeLe, termineLe: enCours ? null : lots[lots.length - 1].termineLe,
    resultats: lots.flatMap((l) => l.resultats),
  };
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
