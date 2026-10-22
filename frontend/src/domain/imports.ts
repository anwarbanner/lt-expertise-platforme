// Contrats de l'API d'import (ma.ltexpertise.importation) : POST/GET /api/imports, GET /api/imports/{id}/pdf.

export type StatutImport = "ACCEPTE" | "REFUSE_GABARIT" | "REFUSE_DOUBLON_NUMERO" | "ERREUR_LECTURE";

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
}

export interface RecapitulatifImport {
  lot: string | null;
  total: number;
  acceptes: number;
  refuses: number;
  enErreur: number;
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
