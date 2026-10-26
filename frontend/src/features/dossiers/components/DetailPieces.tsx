import { DataTable } from "@/components/ui/DataTable";
import type { LigneDetail } from "@/domain/dossiers";
import { montant } from "@/domain/format";

const TYPES: Record<string, string> = { ORIGINE: "O", ADAPTABLE: "A", REEMPLOI: "R" };
const FAMILLES: Record<string, string> = { TOLERIE: "Tôlerie", PEINTURE: "Peinture", MECANIQUE: "Mécanique" };
const OPERATIONS: Record<string, string> = { ECHANGE: "Échange", REPARATION: "Réparation" };

/**
 * Détail fournitures / travaux (page 2), regroupé par zone puis par famille, dans l'ordre du rapport. Une ligne dont
 * le prix unitaire ne redonne pas le total est en rouge ; avec {@code changerPrix} (écran de vérification), son prix
 * se corrige, le bouton proposant le prix déduit du total imprimé.
 */
export function DetailPieces({ lignes, prix, changerPrix }: {
  lignes: LigneDetail[];
  prix?: Record<number, number | null>;
  changerPrix?: (ordre: number, prix: number | null) => void;
}) {
  if (lignes.length === 0) return <p className="etat-chargement">Aucune ligne de détail (rapport de réforme ou non définitif).</p>;
  const groupes: { cle: string; zone: string | null; zoneChoc: number | null; famille: string | null; lignes: LigneDetail[] }[] = [];
  for (const l of lignes) {
    const cle = `${l.zoneLibelle}|${l.famille}`;
    const dernier = groupes[groupes.length - 1];
    if (dernier?.cle === cle) dernier.lignes.push(l);
    else groupes.push({ cle, zone: l.zoneLibelle, zoneChoc: l.zoneChoc, famille: l.famille, lignes: [l] });
  }
  const n = (v: number | null) => montant(v, "");
  const cellulePrix = (l: LigneDetail) => {
    if (!changerPrix || l.prixCoherent) return n(l.prixUnitaire);
    const valeur = prix?.[l.ordre] ?? null;
    return <div className="prix-correction">
      <input type="number" step="0.01" aria-label={`Prix unitaire ligne ${l.ordre + 1}`} value={valeur ?? ""}
        onChange={(e) => changerPrix(l.ordre, e.target.value === "" ? null : Number(e.target.value))} />
      {l.prixCalcule !== null && valeur !== l.prixCalcule && <button type="button" onClick={() => changerPrix(l.ordre, l.prixCalcule)}
        title="Prix qui redonne le total imprimé">→ {n(l.prixCalcule)}</button>}
    </div>;
  };
  return <div className="parts-table">{groupes.map((g, i) => <div key={`${g.cle}-${i}`}>
    {(i === 0 || groupes[i - 1].zone !== g.zone) && <div className="group-label">ZONE : {(g.zone ?? "non précisée").toUpperCase()} {g.zoneChoc && <span>{g.zoneChoc}</span>}</div>}
    <div className="subgroup">{g.famille ? FAMILLES[g.famille] : "Autres"}</div>
    <DataTable headers={["N°", "QTÉ", "DÉSIGNATION", "TYPE", "PRIX", "VÉTUSTÉ", "TVA", "TOTAL", "OPÉRATION", "HEURES", "MONTANT"]}>{g.lignes.map((l) =>
      <tr key={l.ordre} className={l.prixCoherent ? "" : "prix-faux"} title={l.prixCoherent ? undefined : "Prix unitaire incohérent avec le total de la ligne"}>
        <td>{l.ordre + 1}</td><td>{l.quantite}</td><td><strong>{l.designation}</strong></td><td>{l.typePiece ? TYPES[l.typePiece] : "—"}</td>
        <td className="money">{cellulePrix(l)}</td><td>{l.vetustePct ?? 0} %</td><td>{l.tvaPct ?? 0} %</td><td className="money">{n(l.totalFourniture)}</td>
        <td>{l.operation ? OPERATIONS[l.operation] : "—"}</td><td>{l.nombreHeures ? `${l.nombreHeures} h` : "—"}</td><td className="money">{n(l.montantMoTtc)}</td>
      </tr>)}</DataTable>
  </div>)}</div>;
}
