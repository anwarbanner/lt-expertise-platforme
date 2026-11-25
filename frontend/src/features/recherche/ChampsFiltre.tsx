import { useEffect, useRef, useState, type ReactNode } from "react";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { VIDE } from "@/domain/dossiers";
import { LIBELLES_TYPE_PIECE, ZONES_CHOC, type TypePiece } from "@/domain/referentiels";

/** Valeur stabilisée 300 ms après la dernière frappe : le serveur n'est interrogé qu'une fois la saisie finie. */
export function useDiffere<T>(valeur: T, delai = 300): T {
  const [differee, setDifferee] = useState(valeur);
  const cle = JSON.stringify(valeur);
  useEffect(() => {
    const minuterie = setTimeout(() => setDifferee(valeur), delai);
    return () => clearTimeout(minuterie);
  }, [cle, delai]); // valeur comparée par son contenu (cle), pas par référence
  return differee;
}

/** Barre de filtres : une grille de champs puis le bouton de remise à zéro. */
export function BarreFiltres({ children, reinitialiser }: { children: ReactNode; reinitialiser: () => void }) {
  return <article className="card filtres-recherche">{children}<Button variant="quiet" onClick={reinitialiser}>Réinitialiser</Button></article>;
}

export function ChampTexte({ libelle, valeur, changer, suggestions, placeholder, type = "text" }: {
  libelle: string; valeur: string; changer: (v: string) => void; suggestions?: string[]; placeholder?: string; type?: "text" | "number" | "date";
}) {
  const liste = suggestions ? `suggestions-${libelle.replace(/\W+/g, "-")}` : undefined;
  return <label><span>{libelle}</span>
    <input aria-label={libelle} type={type} value={valeur} onChange={(e) => changer(e.target.value)} placeholder={placeholder} list={liste} min={type === "number" ? 0 : undefined} />
    {liste && <datalist id={liste}>{suggestions?.map((s) => <option key={s} value={s} />)}</datalist>}
  </label>;
}

/** @param avecVide propose aussi « Non renseigné » (lignes sans type O, A ou R) */
export function ChampTypePiece({ valeur, changer, avecVide = false }: { valeur: string; changer: (v: string) => void; avecVide?: boolean }) {
  return <label><span>Type de pièce</span><select aria-label="Type de pièce" value={valeur} onChange={(e) => changer(e.target.value)}>
    <option value="">Tous</option>{(Object.entries(LIBELLES_TYPE_PIECE) as [TypePiece, string][]).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
    {avecVide && <option value="VIDE">{LIBELLE_VIDE}</option>}
  </select></label>;
}

export const LIBELLE_VIDE = "Non renseigné";

/**
 * Champ texte (recherche partielle, sans casse) avec une case « Vide » : cochée, le filtre demande les lignes où le
 * champ n'est pas renseigné ({@link VIDE}) et la saisie est désactivée.
 */
export function ChampTexteOuVide({ libelle, valeur, changer, suggestions, placeholder, type = "text" }: {
  libelle: string; valeur: string; changer: (v: string) => void; suggestions?: string[]; placeholder?: string; type?: "text" | "number";
}) {
  const vide = valeur === VIDE;
  const liste = suggestions ? `suggestions-${libelle.replace(/\W+/g, "-")}` : undefined;
  return <div className="champ-filtre">
    <span className="champ-entete">{libelle}<CaseVide libelle={libelle} coche={vide} changer={(c) => changer(c ? VIDE : "")} /></span>
    <input aria-label={libelle} type={type} value={vide ? "" : valeur} disabled={vide} onChange={(e) => changer(e.target.value)}
      placeholder={vide ? LIBELLE_VIDE : placeholder} list={liste} min={type === "number" ? 0 : undefined} />
    {liste && <datalist id={liste}>{suggestions?.map((s) => <option key={s} value={s} />)}</datalist>}
  </div>;
}

function CaseVide({ libelle, coche, changer }: { libelle: string; coche: boolean; changer: (coche: boolean) => void }) {
  return <label className="case-vide" title={`${libelle} non renseigné(e)`}>
    <input type="checkbox" aria-label={`${libelle} vide`} checked={coche} onChange={(e) => changer(e.target.checked)} />vide
  </label>;
}

/** Liste déroulante des valeurs connues, avec « Tous » (aucun filtre) et « Non renseigné » ({@link VIDE}). */
export function ChampListe({ libelle, valeur, changer, options, tous = "Tous" }: {
  libelle: string; valeur: string; changer: (v: string) => void; options: string[] | undefined; tous?: string;
}) {
  // Une valeur choisie reste affichée même si elle sort de la liste (autre marque saisie entre-temps)
  const valeurs = valeur && valeur !== VIDE && !(options ?? []).includes(valeur) ? [valeur, ...(options ?? [])] : options ?? [];
  return <label><span>{libelle}</span><select aria-label={libelle} value={valeur} onChange={(e) => changer(e.target.value)}>
    <option value="">{tous}</option><option value={VIDE}>{LIBELLE_VIDE}</option>
    {valeurs.map((v) => <option key={v} value={v}>{v}</option>)}
  </select></label>;
}

/** Sélection de plusieurs zones de choc : une ligne répond si sa zone est l'une des zones cochées. */
export function ChampZones({ valeurs, changer }: { valeurs: number[]; changer: (zones: number[]) => void }) {
  const [ouvert, setOuvert] = useState(false);
  const racine = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!ouvert) return;
    const fermer = (e: MouseEvent) => { if (!racine.current?.contains(e.target as Node)) setOuvert(false); };
    const echap = (e: KeyboardEvent) => { if (e.key === "Escape") setOuvert(false); };
    document.addEventListener("mousedown", fermer);
    document.addEventListener("keydown", echap);
    return () => { document.removeEventListener("mousedown", fermer); document.removeEventListener("keydown", echap); };
  }, [ouvert]);
  const basculer = (code: number) => changer(valeurs.includes(code) ? valeurs.filter((z) => z !== code) : [...valeurs, code].sort((a, b) => a - b));
  const resume = valeurs.length === 0 ? "Toutes" : valeurs.length <= 3 ? valeurs.map((z) => `${z} · ${ZONES_CHOC[z]}`).join(", ") : `${valeurs.length} zones`;
  return <div className="champ-filtre champ-zones" ref={racine}>
    <span className="champ-entete">Zones de choc</span>
    <button type="button" className="selection-multiple" aria-haspopup="true" aria-expanded={ouvert} aria-label="Zones de choc" title={resume} onClick={() => setOuvert((o) => !o)}>
      <span>{resume}</span><Icon name="chevron" size={12} />
    </button>
    {ouvert && <div className="liste-zones" role="group" aria-label="Choix des zones de choc">
      {Object.entries(ZONES_CHOC).map(([code, libelle]) => <label key={code}>
        <input type="checkbox" checked={valeurs.includes(Number(code))} onChange={() => basculer(Number(code))} />{code} · {libelle}
      </label>)}
      {valeurs.length > 0 && <button type="button" className="link-button" onClick={() => changer([])}>Toutes les zones</button>}
    </div>}
  </div>;
}

/** « 3 ans 11 mois » pour 47 mois. */
export function age(mois: number | null): string {
  if (mois === null) return "—";
  const ans = Math.floor(mois / 12);
  const reste = mois % 12;
  return [ans > 0 ? `${ans} an${ans > 1 ? "s" : ""}` : "", reste > 0 || ans === 0 ? `${reste} mois` : ""].filter(Boolean).join(" ");
}
