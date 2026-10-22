// Miroir des énumérations du backend (ma.ltexpertise.dossier.referentiel, importation, utilisateur).
// Toute évolution côté Java doit être reportée ici.

export type StatutDossier = "EN_COURS" | "IMPORTE" | "A_COMPLETER" | "A_VERIFIER" | "VALIDE" | "ARCHIVE";

/**
 * Statuts affichés par l'interface. Deux d'entre eux n'existent pas dans StatutDossier côté backend :
 * - PRET_A_VALIDER : dossier dont tous les contrôles passent, en attente du gestionnaire (décision en attente,
 *   voir docs/conception/analyse-frontend-roadmap.md) ;
 * - REFUSE : fichier refusé à l'import (StatutImport ≠ ACCEPTE), pas un statut de dossier.
 */
export type StatutAffiche = StatutDossier | "PRET_A_VALIDER" | "REFUSE";

export const LIBELLES_STATUT: Record<StatutAffiche, string> = {
  EN_COURS: "En cours",
  IMPORTE: "Importé",
  A_COMPLETER: "À compléter",
  A_VERIFIER: "À vérifier",
  PRET_A_VALIDER: "Prêt à valider",
  VALIDE: "Validé",
  ARCHIVE: "Archivé",
  REFUSE: "Refusé",
};

export type TypeProcedure =
  | "PROCEDURE_CLASSIQUE"
  | "PRISE_EN_CHARGE_ET_PLUS_QUE_20000"
  | "PLUS_DE_20000"
  | "PRISE_EN_CHARGE"
  | "WAFA_24";

export const LIBELLES_PROCEDURE: Record<TypeProcedure, string> = {
  PROCEDURE_CLASSIQUE: "Procédure classique",
  PRISE_EN_CHARGE_ET_PLUS_QUE_20000: "PEC et plus que 20 000",
  PLUS_DE_20000: "Plus de 20 000",
  PRISE_EN_CHARGE: "Prise en charge",
  WAFA_24: "Wafa 24 heures",
};

export type NatureRapport = "DEFINITIF" | "NON_DEFINITIF" | "REFORME";
export type NatureContrat = "RC" | "ASSURANCE_DOMMAGE";
export type TypePiece = "ORIGINE" | "ADAPTABLE" | "REEMPLOI";
export type TypeOperation = "ECHANGE" | "REPARATION";
export type Role = "ADMINISTRATEUR" | "GESTIONNAIRE" | "CONSULTATION";

export const LIBELLES_ROLE: Record<Role, string> = {
  ADMINISTRATEUR: "Administrateur",
  GESTIONNAIRE: "Gestionnaire",
  CONSULTATION: "Consultation",
};

/** Zones de choc codées 1 à 8, comme ZoneChoc.java (la zone 3 est déduite, aucun échantillon). */
export const ZONES_CHOC: Record<number, string> = {
  1: "Avant/Latéral gauche",
  2: "Avant/Latéral droit",
  3: "Central/Latéral gauche",
  4: "Central/Latéral droit",
  5: "Arrière/Latéral gauche",
  6: "Arrière/Latéral droit",
  7: "Arrière",
  8: "Avant",
};
