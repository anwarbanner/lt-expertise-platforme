import { useEffect, useState } from "react";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { analysePrix, cles } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterPrix } from "@/api/exports";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { date, montant, nombre } from "@/domain/format";
import type { TypePiece } from "@/domain/referentiels";

const TYPES: readonly (readonly [TypePiece, string])[] = [["ORIGINE", "Origine"], ["ADAPTABLE", "Adaptable"], ["REEMPLOI", "Réemploi"]];
const LIBELLE_TYPE = Object.fromEntries(TYPES) as Record<TypePiece, string>;

/**
 * Analyse des prix unitaires des pièces relevées dans les rapports (ligne_detail), filtrable par type O/A/R,
 * compagnie et marque. « Référence » et « fournisseur » n'existent pas dans les rapports PDF : non affichés.
 */
export default function AnalysePrixPage() {
  const navigate = useNavigate();
  const [saisie, setSaisie] = useState("");
  const [q, setQ] = useState("");
  const [type, setType] = useState("");
  const [compagnie, setCompagnie] = useState("");
  const [marque, setMarque] = useState("");
  const filtres = { q, type, compagnie, marque };
  const analyse = useQuery({ queryKey: cles.prix(filtres), queryFn: () => analysePrix(filtres), placeholderData: keepPreviousData });

  useEffect(() => {
    const minuterie = setTimeout(() => setQ(saisie.trim()), 300);
    return () => clearTimeout(minuterie);
  }, [saisie]);

  const reinitialiser = () => { setSaisie(""); setQ(""); setType(""); setCompagnie(""); setMarque(""); };
  const a = analyse.data;
  const minimum = a?.minimum ?? null;
  const moyenne = a?.moyenne ?? null;
  const maximum = a?.maximum ?? null;
  const ecart = minimum !== null && maximum !== null ? maximum - minimum : null;
  const positionMoyenne = ecart && moyenne !== null && minimum !== null ? ((moyenne - minimum) / ecart) * 100 : 50;
  const moyennesParType = TYPES.map(([t, l]) => [t, l, a?.moyenneParType[t] ?? null] as const).filter(([, , m]) => m !== null);
  const plusForteMoyenne = Math.max(1, ...moyennesParType.map(([, , m]) => m ?? 0));

  return <main className="main pricing-page">
    <PageHead title="Analyse des prix" subtitle="Comparez les prix unitaires des pièces par compagnie et par véhicule" actions={<BoutonTelechargement action={() => exporterPrix(filtres)} title="Toutes les lignes répondant aux filtres">Exporter l’analyse</BoutonTelechargement>} />
    <article className="card pricing-filters">
      <div className="pricing-search"><Icon name="search" size={17} /><input aria-label="Rechercher une pièce" value={saisie} onChange={(e) => setSaisie(e.target.value)} placeholder="Rechercher une pièce (désignation)…" /></div>
      <label><span>Type de pièce</span><select value={type} onChange={(e) => setType(e.target.value)}><option value="">Tous</option>{TYPES.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</select></label>
      <label><span>Compagnie</span><select value={compagnie} onChange={(e) => setCompagnie(e.target.value)}><option value="">Toutes les compagnies</option>{a?.compagnies.map((c) => <option key={c}>{c}</option>)}</select></label>
      <label><span>Marque</span><select value={marque} onChange={(e) => setMarque(e.target.value)}><option value="">Toutes les marques</option>{a?.marques.map((m) => <option key={m}>{m}</option>)}</select></label>
      <Button variant="quiet" onClick={reinitialiser}>Réinitialiser</Button>
    </article>
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
        <div className="section-head"><div><h2>Détail des prix</h2><p>{a.nombre} résultat{a.nombre !== 1 ? "s" : ""}{a.lignes.length < a.nombre ? ` (${a.lignes.length} plus récents affichés)` : ""}</p></div></div>
        <DataTable headers={["DÉSIGNATION", "TYPE", "COMPAGNIE", "VÉHICULE", "DOSSIER", "VÉTUSTÉ", "PRIX UNITAIRE", "DATE"]}>{a.lignes.map((l, i) => <tr key={`${l.numeroRapport}-${i}`}>
          <td><strong>{l.designation}</strong></td>
          <td><span className={`price-type ${l.typePiece === "ORIGINE" ? "part" : "supply"}`}>{LIBELLE_TYPE[l.typePiece] ?? l.typePiece}</span></td>
          <td>{l.compagnie ?? "—"}</td>
          <td>{[l.marque, l.modele].filter(Boolean).join(" ") || "—"}</td>
          <td><button className="link-button" onClick={() => navigate(ROUTES.dossier(l.numeroRapport))}>{l.numeroRapport}</button></td>
          <td>{l.vetustePct !== null ? `${nombre(l.vetustePct)} %` : "—"}</td>
          <td className="money"><strong>{montant(l.prixUnitaire)}</strong></td>
          <td>{date(l.date)}</td>
        </tr>)}</DataTable>
        {!a.lignes.length && <div className="empty-prices"><Icon name="search" size={24} /><strong>Aucun prix trouvé</strong><span>Modifiez ou réinitialisez vos critères de recherche.</span></div>}
      </article>
    </>}
  </main>;
}
