// Contrats de l'API d'import (ma.ltexpertise.importation) :
// POST /api/imports (1 fichier → 201 ; plusieurs → 202, traitement en arrière-plan),
// GET /api/imports/{lot} (récapitulatif), GET /api/imports (historique), GET /api/imports/{id}/pdf.
import type { StatutDossier } from "./referentiels";

export type StatutImport = "ACCEPTE" | "REFUSE_GABARIT" | "REFUSE_DOUBLON_NUMERO" | "ERREUR_LECTURE";

export const LIBELLES_STATUT_IMPORT: Record<StatutImport, string> = {
  ACCEPTE: "Accepté",
  REFUSE_GABARIT: "Modèle non reconnu",
  REFUSE_DOUBLON_NUMERO: "N° déjà enregistré",
  ERREUR_LECTURE: "PDF illisible",
};

export type IssueImport =
  | "ACCEPTE"
  | "RATTACHE_DOSSIER_EN_COURS"
  | "DOUBLON_FICHIER"
  | "REFUSE_DOUBLON_NUMERO"
  | "REFUSE_GABARIT"
  | "ERREUR_LECTURE"
  | "FICHIER_INVALIDE"
  | "ERREUR";

export interface ResultatImport {
  nomFichier: string;
  issue: IssueImport;
  importId: number | null;
  rapportId: number | null;
  numeroRapport: string | null;
  motif: string | null;
  /** Renseignés pour un PDF accepté : statut calculé (jamais VALIDE) et bilan des 28 contrôles. */
  statutDossier: StatutDossier | null;
  controlesConformes: number | null;
  controlesTotal: number | null;
  champsASaisir: number | null;
}

export interface RecapitulatifLot {
  lot: string;
  statut: "EN_COURS" | "TERMINE";
  total: number;
  traites: number;
  reussis: number;
  aCompleter: number;
  aVerifier: number;
  pretsAValider: number;
  refuses: number;
  enErreur: number;
  creeLe: string;
  termineLe: string | null;
  resultats: ResultatImport[];
}

export interface ImportFichier {
  id: number;
  nomFichier: string;
  hashSha256: string;
  tailleOctets: number;
  nombrePages: number | null;
  statutImport: StatutImport;
  motifRefus: string | null;
  gabaritDetecte: string | null;
  lotImport: string | null;
  importePar: string;
  importeLe: string;
}

/** Aligné sur lt.import.taille-max-octets (application.yml). */
export const TAILLE_MAX_IMPORT_MO = 20;
