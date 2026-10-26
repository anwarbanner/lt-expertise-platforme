import { DataTable } from "@/components/ui/DataTable";
import type { DossierDetail } from "@/domain/dossiers";
import { montant } from "@/domain/format";

const POSTES: Record<string, string> = {
  MO_1_TOLERIE: "MO 1 Tôlerie", MO_2_TOLERIE: "MO 2 Tôlerie", MO_3_TOLERIE: "MO 3 Tôlerie", MO_PEINTURE: "MO Peinture",
  MO_1_MECANIQUE: "MO 1 Mécanique", MO_2_MECANIQUE: "MO 2 Mécanique", MO_3_MECANIQUE: "MO 3 Mécanique",
};
const FOURNITURES: Record<string, string> = {
  PIECES: "Pièces", INGREDIENT_PEINTURE: "Ingrédient peinture", PETITE_FOURNITURE: "Petite fourniture", AUTRES: "Autres",
};

/** Évaluation des dommages (page 1 du rapport) : 7 postes de main d'œuvre, fournitures et totaux, tels que lus. */
export function DamageTable({ dossier }: { dossier: DossierDetail }) {
  const n = (v: number | null | undefined) => montant(v, "");
  return <>
    <DataTable headers={["POSTE", "NBR H", "PRIX UNIT.", "TOTAL HT", "TVA", "TOTAL TTC"]}>{dossier.mainOeuvre.map((l) =>
      <tr key={l.poste}><td><strong>{POSTES[l.poste] ?? l.poste}</strong></td><td className="money">{n(l.nombreHeures)}</td><td className="money">{n(l.tauxHoraire)}</td>
        <td className="money">{n(l.montants.ht)}</td><td className="money">{n(l.montants.tva)}</td><td className="money">{n(l.montants.ttc)}</td></tr>)}
      <tr className="ligne-total"><td><strong>Total main d’œuvre</strong></td><td /><td /><td className="money">{n(dossier.totalMainOeuvre?.ht)}</td><td className="money">{n(dossier.totalMainOeuvre?.tva)}</td><td className="money">{n(dossier.totalMainOeuvre?.ttc)}</td></tr>
    </DataTable>
    <div className="damage-totals">
      {dossier.fournitures.map((f) => <div key={f.categorie}><span>{FOURNITURES[f.categorie] ?? f.categorie}</span><b>{montant(f.montants.ttc)}</b></div>)}
      <div><span>Total fournitures TTC</span><b>{montant(dossier.totalFournitures?.ttc)}</b></div>
      <div className="grand"><span>Total général TTC</span><b>{montant(dossier.totalGeneral?.ttc)}</b></div>
    </div>
  </>;
}
