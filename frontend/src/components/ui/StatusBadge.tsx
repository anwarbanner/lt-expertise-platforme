import { LIBELLES_STATUT, type StatutAffiche } from "@/domain/referentiels";

/** Classe CSS de chaque statut (couleurs définies dans index.css : .status.imported, .status.verify…). */
export const CLASSES_STATUT: Record<StatutAffiche, string> = {
  EN_COURS: "imported",
  IMPORTE: "imported",
  A_COMPLETER: "incomplete",
  A_VERIFIER: "verify",
  PRET_A_VALIDER: "ready",
  VALIDE: "validated",
  ARCHIVE: "archived",
  REFUSE: "refused",
};

export function StatusBadge({ statut }: { statut: StatutAffiche }) {
  return <span className={`status ${CLASSES_STATUT[statut]}`}><i />{LIBELLES_STATUT[statut]}</span>;
}
