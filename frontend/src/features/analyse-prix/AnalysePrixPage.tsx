import { useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { analysePrix, cles } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterPrix } from "@/api/exports";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { ROUTES } from "@/app/routes";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { ETAT_INITIAL, parametresTableau, TableauInteractif, type Colonne } from "@/components/ui/TableauInteractif";
import type { FiltresPrix, LignePrix } from "@/domain/dossiers";
import { date, montant, nombre } from "@/domain/format";
import { LIBELLES_TYPE_PIECE, type TypePiece } from "@/domain/referentiels";
import { BarreFiltres, ChampsVehicule, ChampTexte, ChampTypePiece, useDiffere } from "@/features/recherche/ChampsFiltre";

const TYPES = Object.entries(LIBELLES_TYPE_PIECE) as [TypePiece, string][];
const VIDES: Required<FiltresPrix> = { designation: "", type: "", marque: "", modele: "", typeMine: "" };

/**
 * Analyse des prix unitaires des pièces relevées dans les rapports (ligne_detail), filtrable par désignation, type
 * O/A/R et véhicule (marque, modèle, type mine). La date du prix est celle du rapport.
 */
export default function AnalysePrixPage() {
  const navigate = useNavigate();
  const [saisie, setSaisie] = useState(VIDES);
  const filtres = useDiffere(saisie);
  const [tableau, setTableau] = useState(ETAT_INITIAL);
  const changer = (champ: keyof FiltresPrix, valeur: string) => { setSaisie((f) => ({ ...f, [champ]: valeur })); setTableau((t) => ({ ...t, page: 0 })); };
  const parametres = parametresTableau(tableau);
  const analyse = useQuery({ queryKey: cles.prix({ ...filtres, ...parametres }), queryFn: () => analysePrix(filtres, parametres), placeholderData: keepPreviousData });

  const colonnes: Colonne<LignePrix>[] = [
    { cle: "dossier", titre: "DOSSIER", texte: (l) => l.numeroRapport, rendu: (l) => <button className="link-button" onClick={() => navigate(ROUTES.dossier(l.numeroRapport))}>{l.numeroRapport}</button> },
    { cle: "designation", titre: "DÉSIGNATION", texte: (l) => l.designation, rendu: (l) => <strong>{l.designation}</strong> },
    { cle: "type", titre: "TYPE", texte: (l) => LIBELLES_TYPE_PIECE[l.typePiece] ?? l.typePiece, rendu: (l) => <span className={`price-type ${l.typePiece === "ORIGINE" ? "part" : "supply"}`}>{LIBELLES_TYPE_PIECE[l.typePiece] ?? l.typePiece}</span> },
    { cle: "marque", titre: "MARQUE", texte: (l) => l.marque ?? "—" },
    { cle: "modele", titre: "MODÈLE", texte: (l) => l.modele ?? "—" },
    { cle: "typeMine", titre: "TYPE MINE", texte: (l) => l.typeMine ?? "—" },
    { cle: "prix", titre: "PRIX UNITAIRE", texte: (l) => montant(l.prixUnitaire), rendu: (l) => <strong>{montant(l.prixUnitaire)}</strong>, numerique: true },
    { cle: "date", titre: "DATE DU PRIX", texte: (l) => date(l.date) },
  ];

  const a = analyse.data;
  const minimum = a?.minimum ?? null;
  const moyenne = a?.moyenne ?? null;
  const maximum = a?.maximum ?? null;
  const ecart = minimum !== null && maximum !== null ? maximum - minimum : null;
  const positionMoyenne = ecart && moyenne !== null && minimum !== null ? ((moyenne - minimum) / ecart) * 100 : 50;
  const moyennesParType = TYPES.map(([t, l]) => [t, l, a?.moyenneParType[t] ?? null] as const).filter(([, , m]) => m !== null);
  const plusForteMoyenne = Math.max(1, ...moyennesParType.map(([, , m]) => m ?? 0));

  return <main className="main pricing-page">
    <PageHead title="Analyse des prix" subtitle="Comparez les prix unitaires des pièces par type et par véhicule" actions={<BoutonTelechargement action={() => exporterPrix(filtres)} title="Toutes les lignes répondant aux filtres">Exporter l’analyse</BoutonTelechargement>} />
    <BarreFiltres reinitialiser={() => { setSaisie(VIDES); setTableau(ETAT_INITIAL); }}>
      <ChampTexte libelle="Désignation de la pièce" valeur={saisie.designation} changer={(v) => changer("designation", v)} placeholder="Ex. pare-choc, phare…" />
      <ChampTypePiece valeur={saisie.type} changer={(v) => changer("type", v)} />
      <ChampsVehicule marque={saisie.marque} modele={saisie.modele} typeMine={saisie.typeMine} changer={changer} />
    </BarreFiltres>
    {analyse.isPending && <p className="etat-chargement">Chargement de l’analyse…</p>}
    {analyse.isError && <p className="import-erreur" role="alert">{messageErreur(analyse.error, "Analyse des prix indisponible.")}</p>}
    {a && <>
      <section className="price-kpis">
        <article className="card price-kpi minimum"><span><Icon name="arrow" size={18} /></span><div><small>Prix minimum</small><strong>{montant(minimum)}</strong><p>Prix unitaire le plus bas</p></div></article>
        <article className="card price-kpi average"><span><Icon name="chart" size={18} /></span><div><small>Prix moyen</small><strong>{montant(moyenne)}</strong><p>Moyenne des résultats filtrés</p></div></article>
        <article className="card price-kpi maximum"><span><Icon name="arrow" size={18} /></span><div><small>Prix maximum</small><strong>{montant(maximum)}</strong><p>Prix unitaire le plus élevé</p></div></article>
        <article className="card price-kpi records"><span><Icon name="file" size={18} /></span><div><small>Prix analysés</small><strong>{nombre(a.nombre)}</strong><p>Lignes de pièces correspondantes</p></div></article>
      </section>
      <section className="pricing-insights">
        <article className="card price-range-card">
          <div className="section-head"><div><h2>Fourchette de prix</h2><p>Positionnement des prix sélectionnés</p></div><span className="price-spread">Écart : {montant(ecart)}</span></div>
          <div className="range-visual"><div className="range-labels"><span>Minimum<b>{montant(minimum)}</b></span><span>Moyenne<b>{montant(moyenne)}</b></span><span>Maximum<b>{montant(maximum)}</b></span></div><div className="range-track"><i /><i style={{ left: `${positionMoyenne}%` }} /><i /></div></div>
          <div className="price-advice"><Icon name="check" size={16} /><div><strong>Indicateur de référence</strong><p>Un prix proche de la moyenne facilite une estimation cohérente. Vérifiez les montants éloignés de plus de 20 %.</p></div></div>
        </article>
        <article className="card distribution-card"><div className="section-head"><div><h2>Répartition par type</h2><p>Prix unitaire moyen par type de pièce</p></div></div>
          <div className="distribution-bars">{moyennesParType.length
            ? moyennesParType.map(([t, l, m]) => <div key={t}><span>{l}</span><i><b className={t === "ORIGINE" ? "parts-bar" : "supplies-bar"} style={{ width: `${((m ?? 0) / plusForteMoyenne) * 100}%` }} /></i><strong>{montant(m)}</strong></div>)
            : <p>Aucune donnée.</p>}</div>
        </article>
      </section>
      <article className="card pricing-results">
        <div className="section-head"><div><h2>Détail des prix</h2><p>{nombre(a.nombre)} résultat{a.nombre !== 1 ? "s" : ""}</p></div></div>
        <TableauInteractif colonnes={colonnes} lignes={a.lignes} total={a.nombre} etat={tableau} changer={setTableau} cleLigne={(l, i) => `${l.numeroRapport}-${i}`}
          vide={<div className="empty-prices"><Icon name="search" size={24} /><strong>Aucun prix trouvé</strong><span>Modifiez ou réinitialisez vos critères de recherche.</span></div>} />
      </article>
    </>}
  </main>;
}
