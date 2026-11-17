import { useEffect, useState, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { cles, suggestionsVehicule } from "@/api/dossiers";
import { Button } from "@/components/ui/Button";
import { LIBELLES_TYPE_PIECE, type TypePiece } from "@/domain/referentiels";

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

export function ChampTypePiece({ valeur, changer }: { valeur: string; changer: (v: string) => void }) {
  return <label><span>Type de pièce</span><select aria-label="Type de pièce" value={valeur} onChange={(e) => changer(e.target.value)}>
    <option value="">Tous</option>{(Object.entries(LIBELLES_TYPE_PIECE) as [TypePiece, string][]).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
  </select></label>;
}

/** Marque, modèle et type mine, avec les valeurs déjà présentes dans les dossiers en suggestion. */
export function ChampsVehicule({ marque, modele, typeMine, changer }: {
  marque: string; modele: string; typeMine: string; changer: (champ: "marque" | "modele" | "typeMine", valeur: string) => void;
}) {
  const s = useQuery({ queryKey: cles.suggestions, queryFn: suggestionsVehicule, staleTime: 5 * 60_000 }).data;
  return <>
    <ChampTexte libelle="Marque" valeur={marque} changer={(v) => changer("marque", v)} suggestions={s?.marques} placeholder="Toutes" />
    <ChampTexte libelle="Modèle" valeur={modele} changer={(v) => changer("modele", v)} suggestions={s?.modeles} placeholder="Tous" />
    <ChampTexte libelle="Type mine" valeur={typeMine} changer={(v) => changer("typeMine", v)} suggestions={s?.typesMine} placeholder="Tous" />
  </>;
}

/** « 3 ans 11 mois » pour 47 mois. */
export function age(mois: number | null): string {
  if (mois === null) return "—";
  const ans = Math.floor(mois / 12);
  const reste = mois % 12;
  return [ans > 0 ? `${ans} an${ans > 1 ? "s" : ""}` : "", reste > 0 || ans === 0 ? `${reste} mois` : ""].filter(Boolean).join(" ");
}
