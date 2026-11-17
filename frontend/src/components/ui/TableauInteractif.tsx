import { useEffect, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from "react";
import { Icon } from "./Icon";
import { Pagination, TAILLES_PAGE } from "./Pagination";

export interface Colonne<T> {
  /** Clé de la colonne, partagée avec le serveur (tri et filtre de colonne). */
  cle: string;
  titre: string;
  /** Texte affiché ; le filtre de la colonne cherche dans ce texte, côté serveur. */
  texte: (ligne: T) => string;
  /** Contenu de la cellule, s'il diffère du texte (lien, badge…). */
  rendu?: (ligne: T) => ReactNode;
  numerique?: boolean;
}

/** Page, tri et filtres de colonne d'un tableau, envoyés au serveur. */
export interface EtatTableau {
  page: number;
  taille: number;
  tri: { cle: string; sens: "asc" | "desc" } | null;
  filtres: Record<string, string>;
}

export const ETAT_INITIAL: EtatTableau = { page: 0, taille: TAILLES_PAGE[0], tri: null, filtres: {} };

/** Paramètres de requête du tableau : page, taille, tri=cle,sens et un f_<cle> par filtre de colonne renseigné. */
export function parametresTableau(etat: EtatTableau): Record<string, string | number> {
  const parametres: Record<string, string | number> = { page: etat.page, taille: etat.taille };
  if (etat.tri) parametres.tri = `${etat.tri.cle},${etat.tri.sens}`;
  for (const [cle, valeur] of Object.entries(etat.filtres)) if (valeur.trim()) parametres[`f_${cle}`] = valeur.trim();
  return parametres;
}

/**
 * Tableau de données paginé par le serveur : tri en cliquant sur un en-tête (croissant, décroissant, aucun), filtre
 * texte sous chaque en-tête, pages en bas. Les colonnes se réordonnent par glisser-déposer des en-têtes (ou
 * Alt + flèches au clavier), à l'affichage seulement.
 */
export function TableauInteractif<T>({ colonnes, lignes, total, etat, changer, cleLigne, vide }: {
  colonnes: Colonne<T>[];
  /** Lignes de la page courante. */
  lignes: T[];
  /** Nombre total de lignes répondant aux filtres. */
  total: number;
  etat: EtatTableau;
  changer: (etat: EtatTableau) => void;
  cleLigne: (ligne: T, index: number) => string;
  vide?: ReactNode;
}) {
  const [ordre, setOrdre] = useState(() => colonnes.map((c) => c.cle));
  const [saisies, setSaisies] = useState<Record<string, string>>(etat.filtres);
  const [deplacee, setDeplacee] = useState<string | null>(null);
  const [cible, setCible] = useState<string | null>(null);
  const etatCourant = useRef(etat);
  etatCourant.current = etat;

  // Filtres de colonne envoyés 300 ms après la dernière frappe, en revenant à la première page
  const cleSaisies = JSON.stringify(saisies);
  useEffect(() => {
    if (cleSaisies === JSON.stringify(etatCourant.current.filtres)) return;
    const minuterie = setTimeout(() => changer({ ...etatCourant.current, filtres: saisies, page: 0 }), 300);
    return () => clearTimeout(minuterie);
  }, [cleSaisies]); // saisies et changer lus au déclenchement ; seule la saisie relance la minuterie

  // Réinitialisation venue de la page (bouton « Réinitialiser ») : les cases suivent
  const cleFiltres = JSON.stringify(etat.filtres);
  useEffect(() => { setSaisies(etat.filtres); }, [cleFiltres]);

  const parCle = useMemo(() => new Map(colonnes.map((c) => [c.cle, c])), [colonnes]);
  const visibles = ordre.map((cle) => parCle.get(cle)).filter((c): c is Colonne<T> => c !== undefined);

  const trier = (cle: string) => {
    const t = etat.tri;
    changer({ ...etat, page: 0, tri: t?.cle !== cle ? { cle, sens: "asc" } : t.sens === "asc" ? { cle, sens: "desc" } : null });
  };

  const deplacer = (source: string, destination: string) => {
    if (source === destination) return;
    setOrdre((o) => {
      const sans = o.filter((c) => c !== source);
      sans.splice(sans.indexOf(destination) + (o.indexOf(source) < o.indexOf(destination) ? 1 : 0), 0, source);
      return sans;
    });
  };

  const clavier = (e: KeyboardEvent, cle: string) => {
    if (!e.altKey || (e.key !== "ArrowLeft" && e.key !== "ArrowRight")) return;
    e.preventDefault();
    const i = ordre.indexOf(cle);
    const voisine = ordre[e.key === "ArrowLeft" ? i - 1 : i + 1];
    if (voisine) deplacer(cle, voisine);
  };

  const filtresActifs = Object.values(saisies).some((v) => v.trim() !== "");
  const modifie = etat.tri !== null || filtresActifs || ordre.some((cle, i) => cle !== colonnes[i]?.cle);

  return <div className="tableau-interactif">
    <div className="tableau-outils">
      <span><Icon name="filter" size={14} />{total} ligne{total !== 1 ? "s" : ""}</span>
      <small>Cliquez sur un en-tête pour trier, faites-le glisser pour déplacer la colonne.</small>
      {modifie && <button className="link-button" onClick={() => {
        setOrdre(colonnes.map((c) => c.cle));
        setSaisies({});
        changer({ ...etat, page: 0, tri: null, filtres: {} });
      }}>Réinitialiser le tableau</button>}
    </div>
    <div className="table-scroll"><table>
      <thead>
        <tr>{visibles.map((c) => {
          const sens = etat.tri?.cle === c.cle ? etat.tri.sens : null;
          return <th key={c.cle} draggable aria-sort={sens === "asc" ? "ascending" : sens === "desc" ? "descending" : "none"}
            className={`${c.numerique ? "num" : ""} ${deplacee === c.cle ? "en-deplacement" : ""} ${cible === c.cle && deplacee !== c.cle ? "cible-depot" : ""}`}
            onDragStart={(e) => { setDeplacee(c.cle); e.dataTransfer?.setData("text/plain", c.cle); }}
            onDragOver={(e) => { e.preventDefault(); setCible(c.cle); }}
            onDragLeave={() => setCible((x) => (x === c.cle ? null : x))}
            onDrop={(e) => { e.preventDefault(); if (deplacee) deplacer(deplacee, c.cle); setDeplacee(null); setCible(null); }}
            onDragEnd={() => { setDeplacee(null); setCible(null); }}>
            <button className="entete-tri" onClick={() => trier(c.cle)} onKeyDown={(e) => clavier(e, c.cle)} title="Trier (Alt + ← / → pour déplacer la colonne)">
              {c.titre}<span aria-hidden="true">{sens === "asc" ? "▲" : sens === "desc" ? "▼" : "⇅"}</span>
            </button>
          </th>;
        })}</tr>
        <tr className="ligne-filtres">{visibles.map((c) => <th key={c.cle}>
          <input aria-label={`Filtrer ${c.titre}`} placeholder="Filtrer…" value={saisies[c.cle] ?? ""} onChange={(e) => setSaisies((f) => ({ ...f, [c.cle]: e.target.value }))} />
        </th>)}</tr>
      </thead>
      <tbody>{lignes.map((l, i) => <tr key={cleLigne(l, i)}>{visibles.map((c) =>
        <td key={c.cle} className={c.numerique ? "num" : undefined}>{c.rendu ? c.rendu(l) : c.texte(l)}</td>)}</tr>)}</tbody>
    </table></div>
    {lignes.length === 0 && (vide ?? <div className="empty-prices"><Icon name="search" size={24} /><strong>Aucun résultat</strong><span>Modifiez ou réinitialisez vos critères.</span></div>)}
    <Pagination page={etat.page} taille={etat.taille} total={total}
      changerPage={(page) => changer({ ...etat, page })} changerTaille={(taille) => changer({ ...etat, taille, page: 0 })} />
  </div>;
}
