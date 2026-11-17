import { nombre } from "@/domain/format";

export const TAILLES_PAGE = [25, 50, 100, 200] as const;

/** Numéros de pages affichés : première, dernière et voisines de la page courante (null = « … »). */
export function pagesAffichees(courante: number, total: number): (number | null)[] {
  const resultat: (number | null)[] = [];
  for (let p = 0; p < total; p++) {
    if (p === 0 || p === total - 1 || Math.abs(p - courante) <= 1) resultat.push(p);
    else if (resultat[resultat.length - 1] !== null) resultat.push(null);
  }
  return resultat;
}

/** Pied de tableau : lignes affichées, pages et nombre de lignes par page. Pages numérotées à partir de 0. */
export function Pagination({ page, taille, total, changerPage, changerTaille }: {
  page: number; taille: number; total: number; changerPage: (page: number) => void; changerTaille?: (taille: number) => void;
}) {
  if (total === 0) return null;
  const totalPages = Math.ceil(total / taille);
  return <div className="pagination">
    <span>{nombre(page * taille + 1)}–{nombre(Math.min((page + 1) * taille, total))} sur {nombre(total)}</span>
    <div>
      {changerTaille && <label className="taille-page">Lignes par page
        <select aria-label="Lignes par page" value={taille} onChange={(e) => changerTaille(Number(e.target.value))}>
          {TAILLES_PAGE.map((t) => <option key={t} value={t}>{t}</option>)}
        </select></label>}
      <button disabled={page === 0} onClick={() => changerPage(page - 1)} aria-label="Page précédente">‹</button>
      {pagesAffichees(page, totalPages).map((p, i) => p === null ? <span key={`e${i}`}>…</span>
        : <button key={p} className={p === page ? "active" : ""} aria-current={p === page ? "page" : undefined} onClick={() => changerPage(p)}>{p + 1}</button>)}
      <button disabled={page + 1 >= totalPages} onClick={() => changerPage(page + 1)} aria-label="Page suivante">›</button>
    </div>
  </div>;
}
