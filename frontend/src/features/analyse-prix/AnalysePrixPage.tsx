import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { analysePrix, cles, suggestionsVehicule } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterPrix } from "@/api/exports";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { ROUTES } from "@/app/routes";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { ETAT_INITIAL, parametresTableau, TableauInteractif, type Colonne } from "@/components/ui/TableauInteractif";
import { VIDE, type AnalysePrix, type FiltresPrix, type LignePrix, type TypePieceOuVide } from "@/domain/dossiers";
import { date, montant, nombre } from "@/domain/format";
import { LIBELLES_TYPE_PIECE } from "@/domain/referentiels";
import {
  BarreFiltres, ChampListe, ChampTexte, ChampTexteOuVide, ChampTypePiece, ChampZones, LIBELLE_VIDE, useDiffere,
} from "@/features/recherche/ChampsFiltre";

interface Saisie { designation: string; type: string; zones: number[]; marque: string; modele: string; typeMine: string; annee: string; chassis: string }
const VIDES: Saisie = { designation: "", type: "", zones: [], marque: "", modele: "", typeMine: "", annee: "", chassis: "" };

/** Libellés et couleurs des types (couleurs dans l'ordre fixe Origine, Adaptable, Réemploi, Vide : voir index.css). */
const TYPES: Record<TypePieceOuVide, string> = { ...LIBELLES_TYPE_PIECE, VIDE: LIBELLE_VIDE };
const libelleType = (t: string | null) => TYPES[(t ?? "VIDE") as TypePieceOuVide] ?? t ?? LIBELLE_VIDE;

function versFiltres(s: Saisie): FiltresPrix {
  return { ...s, zones: s.zones.join(",") };
}

/**
 * Analyse des prix des pièces relevées dans les rapports (ancienne recherche de pièces comprise) : filtres par pièce,
 * type, zones de choc, véhicule (marque, modèle, type mine, MEC, châssis), indicateurs, répartition par type et détail
 * des prix. Les champs texte cherchent la saisie n'importe où, sans casse ; « vide » demande un champ non renseigné.
 */
export default function AnalysePrixPage() {
  const navigate = useNavigate();
  const [saisie, setSaisie] = useState(VIDES);
  const differee = useDiffere(saisie);
  const filtres = versFiltres(differee);
  const [tableau, setTableau] = useState(ETAT_INITIAL);
  const changer = (modif: Partial<Saisie>) => { setSaisie((f) => ({ ...f, ...modif })); setTableau((t) => ({ ...t, page: 0 })); };
  const parametres = parametresTableau(tableau);
  const analyse = useQuery({ queryKey: cles.prix({ ...filtres, ...parametres }), queryFn: () => analysePrix(filtres, parametres), placeholderData: keepPreviousData });

  // Modèles connus pour la marque saisie, types mine pour le modèle choisi
  const vehicule = {
    marque: differee.marque === VIDE ? undefined : differee.marque || undefined,
    modele: differee.modele === VIDE ? undefined : differee.modele || undefined,
  };
  const suggestions = useQuery({ queryKey: cles.suggestions(vehicule), queryFn: () => suggestionsVehicule(vehicule), staleTime: 5 * 60_000, placeholderData: keepPreviousData }).data;

  const colonnes: Colonne<LignePrix>[] = [
    { cle: "designation", titre: "DÉSIGNATION", texte: (l) => l.designation,
      rendu: (l) => <button className="link-button" title={`Ouvrir le dossier ${l.numeroRapport}`} onClick={() => navigate(ROUTES.dossier(l.numeroRapport))}><strong>{l.designation}</strong></button> },
    { cle: "type", titre: "TYPE", texte: (l) => libelleType(l.typePiece),
      rendu: (l) => <span className={`price-type type-${(l.typePiece ?? "VIDE").toLowerCase()}`}>{libelleType(l.typePiece)}</span> },
    { cle: "marque", titre: "MARQUE", texte: (l) => l.marque ?? "—" },
    { cle: "modele", titre: "MODÈLE", texte: (l) => l.modele ?? "—" },
    { cle: "typeMine", titre: "TYPE MINE", texte: (l) => l.typeMine ?? "—" },
    { cle: "prix", titre: "PRIX UNITAIRE", texte: (l) => montant(l.prixUnitaire), rendu: (l) => <strong>{montant(l.prixUnitaire)}</strong>, numerique: true },
    { cle: "date", titre: "DATE DU PRIX", texte: (l) => date(l.date) },
  ];

  const a = analyse.data;
  return <main className="main pricing-page">
    <PageHead title="Analyse des prix" subtitle="Recherchez les pièces relevées dans les rapports et comparez leurs prix unitaires"
      actions={<BoutonTelechargement action={() => exporterPrix(filtres)} title="Toutes les lignes répondant aux filtres">Exporter l’analyse</BoutonTelechargement>} />
    <BarreFiltres reinitialiser={() => { setSaisie(VIDES); setTableau(ETAT_INITIAL); }}>
      <ChampTexte libelle="Pièce" valeur={saisie.designation} changer={(v) => changer({ designation: v })} placeholder="Ex. pare-choc, phare…" />
      <ChampTypePiece avecVide valeur={saisie.type} changer={(v) => changer({ type: v })} />
      <ChampZones valeurs={saisie.zones} changer={(zones) => changer({ zones })} />
      <ChampTexteOuVide libelle="Marque" valeur={saisie.marque} changer={(v) => changer({ marque: v, modele: "", typeMine: "" })} suggestions={suggestions?.marques} placeholder="Toutes" />
      <ChampListe libelle="Modèle" valeur={saisie.modele} changer={(v) => changer({ modele: v, typeMine: "" })} options={suggestions?.modeles} />
      <ChampListe libelle="Type mine" valeur={saisie.typeMine} changer={(v) => changer({ typeMine: v })} options={suggestions?.typesMine} />
      <ChampTexteOuVide libelle="Année de MEC" type="number" valeur={saisie.annee} changer={(v) => changer({ annee: v })} placeholder="Ex. 2022" />
      <ChampTexteOuVide libelle="N° de châssis" valeur={saisie.chassis} changer={(v) => changer({ chassis: v })} placeholder="Tout ou partie" />
    </BarreFiltres>
    {analyse.isPending && <p className="etat-chargement">Chargement de l’analyse…</p>}
    {analyse.isError && <p className="import-erreur" role="alert">{messageErreur(analyse.error, "Analyse des prix indisponible.")}</p>}
    {a && <>
      <section className="price-kpis" aria-label="Détails des prix">
        <article className="card price-kpi records"><span><Icon name="folder" size={18} /></span><div><small>Dossiers trouvés</small><strong>{nombre(a.dossiers)}</strong><p>{nombre(a.nombre)} ligne{a.nombre !== 1 ? "s" : ""} de pièces</p></div></article>
        <article className="card price-kpi average"><span><Icon name="chart" size={18} /></span><div><small>Prix moyen</small><strong>{montant(a.moyenne)}</strong><p>Moyenne des prix unitaires</p></div></article>
        <article className="card price-kpi maximum"><span><Icon name="arrow" size={18} /></span><div><small>Prix maximum</small><strong>{montant(a.maximum)}</strong><p>Prix unitaire le plus élevé</p></div></article>
        <article className="card price-kpi minimum"><span><Icon name="arrow" size={18} /></span><div><small>Prix minimum</small><strong>{montant(a.minimum)}</strong><p>Prix unitaire le plus bas</p></div></article>
      </section>
      <RepartitionTypes analyse={a} />
      <article className="card pricing-results">
        <div className="section-head"><div><h2>Liste des pièces</h2><p>{nombre(a.nombre)} résultat{a.nombre !== 1 ? "s" : ""} · cliquez sur une désignation pour ouvrir son dossier</p></div></div>
        <TableauInteractif colonnes={colonnes} lignes={a.lignes} total={a.nombre} etat={tableau} changer={setTableau} cleLigne={(l, i) => `${l.numeroRapport}-${i}`}
          vide={<div className="empty-prices"><Icon name="search" size={24} /><strong>Aucun prix trouvé</strong><span>Modifiez ou réinitialisez vos critères de recherche.</span></div>} />
      </article>
    </>}
  </main>;
}

/**
 * Répartition des lignes par type (Origine, Adaptable, Réemploi, non renseigné) : barre empilée en proportion du
 * nombre de lignes, et prix unitaire moyen par type. Chaque type porte son libellé et sa valeur (jamais la couleur seule).
 */
function RepartitionTypes({ analyse }: { analyse: AnalysePrix }) {
  const total = analyse.parType.reduce((s, t) => s + t.nombre, 0);
  const plusForteMoyenne = Math.max(1, ...analyse.parType.map((t) => t.moyenne ?? 0));
  const part = (n: number) => (total ? (n / total) * 100 : 0);
  const pourcentage = (n: number) => `${part(n).toLocaleString("fr-FR", { maximumFractionDigits: 1 })} %`;
  return <section className="pricing-insights repartition-types">
    <article className="card distribution-card">
      <div className="section-head"><div><h2>Répartition par type</h2><p>Nombre de lignes : origine, adaptable, réemploi ou non renseigné</p></div></div>
      {total === 0 ? <p className="etat-chargement">Aucune donnée.</p> : <>
        <div className="barre-empilee" role="img" aria-label={analyse.parType.map((t) => `${libelleType(t.type)} : ${t.nombre}`).join(", ")}>
          {analyse.parType.map((t) => <i key={t.type} className={`type-${t.type.toLowerCase()}`} style={{ width: `${part(t.nombre)}%` }}
            title={`${libelleType(t.type)} : ${nombre(t.nombre)} ligne${t.nombre !== 1 ? "s" : ""} (${pourcentage(t.nombre)})`} />)}
        </div>
        <ul className="legende-types">{analyse.parType.map((t) => <li key={t.type}>
          <i className={`type-${t.type.toLowerCase()}`} /><span>{libelleType(t.type)}</span><b>{nombre(t.nombre)}</b><small>{pourcentage(t.nombre)}</small>
        </li>)}</ul>
      </>}
    </article>
    <article className="card distribution-card">
      <div className="section-head"><div><h2>Prix moyen par type</h2><p>Prix unitaire moyen des lignes de chaque type</p></div></div>
      <div className="distribution-bars">{analyse.parType.some((t) => t.moyenne !== null)
        ? analyse.parType.map((t) => <div key={t.type} title={`${libelleType(t.type)} : ${montant(t.moyenne)}`}>
          <span>{libelleType(t.type)}</span><i><b className={`type-${t.type.toLowerCase()}`} style={{ width: `${((t.moyenne ?? 0) / plusForteMoyenne) * 100}%` }} /></i><strong>{montant(t.moyenne)}</strong>
        </div>)
        : <p>Aucun prix.</p>}</div>
    </article>
  </section>;
}
