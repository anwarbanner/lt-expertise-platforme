import { mois, nombre } from "@/domain/format";

export interface Barre {
  /** Mois « AAAA-MM ». */
  mois: string;
  valeur: number;
  /** Seconde série empilée (barres claires), facultative. */
  valeur2?: number;
}

/**
 * Histogramme mensuel ; empilé si {@code valeur2} est renseigné. La dernière barre est mise en avant.
 * En dirhams (graduations en milliers) par défaut, ou en nombre de dossiers.
 */
export function BarChart({ barres, unite = "dirhams" }: { barres: Barre[]; unite?: "dirhams" | "dossiers" }) {
  const empile = barres.some((b) => b.valeur2 !== undefined);
  const maximum = Math.max(1, ...barres.map((b) => b.valeur + (b.valeur2 ?? 0)));
  const graduations = [1, 0.75, 0.5, 0.25, 0].map((f) => unite === "dirhams"
    ? nombre(Math.round((maximum * f) / 1000)) + "k" : nombre(Math.round(maximum * f)));
  const suffixe = unite === "dirhams" ? " DH" : " dossier(s)";
  return <div className="bar-chart">
    <div className="chart-y">{graduations.map((g, i) => <span key={i}>{g}</span>)}</div>
    <div className="bars">{barres.map((b, i) => <div className="bar-col" key={b.mois} title={`${mois(b.mois, true)} : ${nombre(b.valeur)}${empile ? ` / ${nombre(b.valeur2)}` : ""}${suffixe}`}>
      <div className="bar-slot">{empile
        ? <><i className="bar-light" style={{ height: `${((b.valeur2 ?? 0) / maximum) * 100}%` }} /><i className="bar-main stacked" style={{ height: `${(b.valeur / maximum) * 100}%` }} /></>
        : <i className={`bar-main ${i === barres.length - 1 ? "accent" : ""}`} style={{ height: `${(b.valeur / maximum) * 100}%` }} />}</div>
      <span>{mois(b.mois)}</span>
    </div>)}</div>
  </div>;
}
