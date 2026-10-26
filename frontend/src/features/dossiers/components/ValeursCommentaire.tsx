import type { CleCommentaire, ValeurCommentaire } from "@/domain/dossiers";
import { date, montant } from "@/domain/format";

const LIBELLES: Record<CleCommentaire, string> = {
  VALEUR_NEUF_CATALOGUE: "Valeur à neuf (prix catalogue)",
  VALEUR_VENALE_FMA: "Valeur vénale (barème FMA)",
  DIVERGENCE_USAGE: "Divergence usage / contrat constatée",
  PIECES_RECUPERATION_FERRAILLE: "Pièces de récupération (ferraille) valorisées",
  PEINTURE_REGLES_ART: "Peinture retenue selon les règles de l’art",
  LOCALISATION_CHOC: "Choc décrit par l’expert",
  CADRE_EXPERTISE: "Cadre de l’expertise",
  MONTANT_DEVIS: "Montant du devis",
  ESTIMATION_SANS_DEVIS: "Estimation sans devis",
  EXPERT_CONFRERE: "Expert confrère",
  MODE_EXPERTISE_CONFRERE: "Mode d’expertise du confrère",
  DATE_ACCORD: "Date d’accord",
};

/**
 * Valeurs clés lues dans le commentaire de l'expert (F12, rapports 2026). Recalculées par le serveur à chaque
 * modification du commentaire.
 */
export function ValeursCommentaire({ valeurs }: { valeurs: ValeurCommentaire[] }) {
  if (!valeurs.length) return <p className="etat-chargement">Aucune valeur clé reconnue dans le commentaire.</p>;
  return <dl className="valeurs-commentaire">{valeurs.map((v) => <div key={v.cle}>
    <dt>{LIBELLES[v.cle] ?? v.cle}</dt>
    <dd>{v.montant !== null ? montant(v.montant) : v.date !== null ? date(v.date) : v.texte === "OUI" ? "Oui" : v.texte === "NON" ? "Non" : v.texte ?? "—"}</dd>
  </div>)}</dl>;
}
