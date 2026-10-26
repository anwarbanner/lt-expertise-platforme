// Libellés affichés pour les valeurs des dossiers.

export const PROCEDURES: readonly (readonly [string, string])[] = [
  ["PROCEDURE_CLASSIQUE", "Procédure classique"], ["PRISE_EN_CHARGE_ET_PLUS_QUE_20000", "PEC et plus que 20 000"],
  ["PLUS_DE_20000", "Plus de 20 000"], ["PRISE_EN_CHARGE", "Prise en charge"], ["WAFA_24", "Wafa 24 heures"],
];
export const CONTRATS: readonly (readonly [string, string])[] = [["RC", "RC"], ["ASSURANCE_DOMMAGE", "Assurance dommage"]];
export const EVENEMENTS: readonly (readonly [string, string])[] = [
  ["CIRCULATION_HORS_STATIONNEMENT", "Circulation hors stationnement"], ["STATIONNEMENT", "Stationnement"], ["AUTRE", "Autre"],
];
export const ENERGIES: readonly (readonly [string, string])[] = [
  ["DIESEL", "Diesel"], ["ESSENCE", "Essence"], ["HYBRIDE", "Hybride"], ["ELECTRIQUE", "Électrique"], ["AUTRE", "Autre"],
];
export const OUI_NON: readonly (readonly [string, string])[] = [["true", "Oui"], ["false", "Non"]];
export const NATURES: Record<string, string> = { DEFINITIF: "Définitif", NON_DEFINITIF: "Non définitif", REFORME: "Réforme" };

export function libelle(liste: readonly (readonly [string, string])[], valeur: string | null | undefined): string | null {
  return liste.find(([v]) => v === valeur)?.[1] ?? null;
}

const ACTIONS: Record<string, string> = {
  IMPORT: "Importé", IMPORT_REFUSE: "Import refusé", CREATION: "Créé", MODIFICATION: "Modifié",
  CHANGEMENT_STATUT: "Statut changé", VALIDATION: "Validé", ARCHIVAGE: "Archivé", EXPORT_PDF: "Rapport PDF généré",
  EXPORT_EXCEL: "Export Excel", CONNEXION: "Connexion", ECHEC_CONNEXION: "Échec de connexion",
  GESTION_UTILISATEUR: "Gestion des comptes",
};

export function libelleAction(action: string): string {
  return ACTIONS[action] ?? action;
}
