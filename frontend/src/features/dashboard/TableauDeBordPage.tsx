import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { cles, tableauDeBord } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { adresseDossier, ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { Kpi } from "@/components/ui/Kpi";
import { PageHead } from "@/components/ui/PageHead";
import { CLASSES_STATUT, StatusBadge } from "@/components/ui/StatusBadge";
import { BarChart } from "@/components/charts/BarChart";
import { HorizontalBars } from "@/components/charts/HorizontalBars";
import { date, mois, montant, montantArrondi, nombre } from "@/domain/format";
import { LIBELLES_STATUT, ZONES_CHOC, type StatutDossier } from "@/domain/referentiels";
import { useUtilisateur } from "@/features/auth/AuthContext";

const ORDRE_STATUTS: StatutDossier[] = ["VALIDE", "PRET_A_VALIDER", "A_VERIFIER", "A_COMPLETER", "IMPORTE", "EN_COURS", "ARCHIVE"];

/** Douze derniers mois, du plus récent au plus ancien, au format « AAAA-MM ». */
function derniersMois(): string[] {
  const maintenant = new Date();
  return Array.from({ length: 12 }, (_, i) => {
    const d = new Date(maintenant.getFullYear(), maintenant.getMonth() - i, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  });
}

/** Tableau de bord (F10) : indicateurs, répartitions et alertes calculés sur les dossiers enregistrés. */
export default function TableauDeBordPage() {
  const navigate = useNavigate();
  const { role } = useUtilisateur();
  const [periode, setPeriode] = useState("");
  const stats = useQuery({ queryKey: cles.tableauDeBord(periode), queryFn: () => tableauDeBord(periode) });
  const versDossiers = (parametres = "") => navigate(`${ROUTES.dossiers}${parametres}`);

  const selecteur = <label className="select-period"><Icon name="clock" size={15} />
    <select aria-label="Période" value={periode} onChange={(e) => setPeriode(e.target.value)}>
      <option value="">Toutes les périodes</option>
      {derniersMois().map((m) => <option key={m} value={m}>{mois(m, true)}</option>)}
    </select></label>;

  if (stats.isPending) return <main className="main dashboard-page"><PageHead title="Tableau de bord" subtitle="Vue d’ensemble de l’activité du cabinet" actions={selecteur} /><p className="etat-chargement">Chargement des statistiques…</p></main>;
  if (stats.isError) return <main className="main dashboard-page"><PageHead title="Tableau de bord" actions={selecteur} /><p className="import-erreur" role="alert">{messageErreur(stats.error, "Statistiques indisponibles.")}</p></main>;

  const s = stats.data;
  const statuts = ORDRE_STATUTS.map((st) => [st, s.parStatut[st] ?? 0] as const).filter(([st, n]) => n > 0 || st !== "EN_COURS");
  const totalStatuts = statuts.reduce((t, [, n]) => t + n, 0);
  const zones = s.parZone.map((z) => [`${z.cle} · ${ZONES_CHOC[Number(z.cle)] ?? "?"}`, z.nombre] as const);
  const compagnies = s.parCompagnie.map((c) => [c.cle, c.nombre] as const);
  const totalMontantPeriode = s.mensuel.reduce((t, m) => t + m.montant, 0);

  return <main className="main dashboard-page">
    <PageHead title="Tableau de bord" subtitle={periode ? `Activité de ${mois(periode, true)}` : "Vue d’ensemble de l’activité du cabinet"} actions={selecteur} />
    <section className="kpi-grid">
      <Kpi label="Dossiers traités" value={nombre(s.dossiers)} meta={`${s.dossiersEnAnomalie} en anomalie`} icon="folder" />
      <Kpi label="Montant total des indemnisations" value={montantArrondi(s.montantTotal)} icon="chart" tone="green" />
      <Kpi label="Coût moyen par sinistre" value={montantArrondi(s.coutMoyen)} icon="car" tone="blue" />
      <Kpi label="En attente de validation" value={nombre(s.enAttenteValidation)} icon="clock" tone="amber" />
    </section>
    <article className="card status-overview">
      <div className="card-head"><div><h2>Dossiers par statut</h2><p>Répartition des {nombre(totalStatuts)} dossiers</p></div><Button variant="quiet" onClick={() => versDossiers()}>Voir les dossiers <Icon name="arrow" size={14} /></Button></div>
      <div className="segmented">{statuts.filter(([, n]) => n > 0).map(([st, n]) => <button key={st} className={CLASSES_STATUT[st]} style={{ flexGrow: n }} onClick={() => versDossiers(`?statut=${st}`)} title={`${LIBELLES_STATUT[st]} : ${n}`} aria-label={`${LIBELLES_STATUT[st]} : ${n}`} />)}</div>
      <div className="segment-legend">{statuts.map(([st, n]) => <button key={st} onClick={() => versDossiers(`?statut=${st}`)}><i className={CLASSES_STATUT[st]} /><span>{LIBELLES_STATUT[st]}</span><b>{n}</b></button>)}</div>
    </article>
    <section className="chart-grid">
      <article className="card chart-card"><div className="card-head"><div><h2>Montants indemnisés par mois</h2><p>12 derniers mois, en dirhams (date du rapport)</p></div><span className="chart-total">{montantArrondi(totalMontantPeriode)}</span></div>
        <BarChart barres={s.mensuel.map((m) => ({ mois: m.mois, valeur: m.montant }))} /></article>
      <article className="card chart-card"><div className="card-head"><div><h2>Répartition main d’œuvre / fournitures</h2><p>Totaux TTC par mois</p></div><div className="mini-legend"><i />Main d’œuvre <i />Fournitures</div></div>
        <BarChart barres={s.mensuel.map((m) => ({ mois: m.mois, valeur: m.mainOeuvreTtc, valeur2: m.fournituresTtc }))} /></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Sinistres par zone de choc</h2><p>Nombre de dossiers</p></div></div>
        <HorizontalBars data={zones} onSelect={(i) => versDossiers(`?zone=${s.parZone[i].cle}`)} /></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Dossiers par compagnie d’assurance</h2><p>Les 5 premières compagnies</p></div></div>
        <HorizontalBars data={compagnies} onSelect={(i) => versDossiers(`?compagnie=${encodeURIComponent(s.parCompagnie[i].cle)}`)} /></article>
    </section>
    <section className="bottom-grid">
      <article className="card alerts-card"><div className="card-head"><div><h2>Alertes récentes</h2><p>Contrôles nécessitant votre attention</p></div><span className="alert-count">{s.dossiersEnAnomalie}</span></div>
        <div className="alert-list">{s.alertes.length === 0 ? <p className="etat-chargement">Aucune anomalie.</p> : s.alertes.map((a) =>
          <button key={a.numeroRapport} onClick={() => navigate(adresseDossier(a.numeroRapport, "A_VERIFIER", role))}>
            <span className={a.incoherences > 0 ? "error" : "warn"}><Icon name="warning" size={15} /></span>
            <div><strong>Rapport {a.numeroRapport}</strong><p>{a.incoherences > 0 ? `${a.premierControle ?? "Contrôle en échec"}${a.incoherences > 1 ? ` (+${a.incoherences - 1})` : ""}` : `${a.champsManquants} champ${a.champsManquants > 1 ? "s" : ""} à compléter`}</p></div>
            <Icon name="chevron" size={14} />
          </button>)}</div>
        <Button variant="secondary" onClick={() => versDossiers("?anomalies=true")}>Voir toutes les alertes</Button></article>
      <article className="card latest-card"><div className="card-head"><div><h2>Derniers imports</h2><p>Dossiers ajoutés récemment</p></div>{role === "GESTIONNAIRE" && <Button variant="quiet" onClick={() => navigate(ROUTES.imports)}>Importer <Icon name="arrow" size={14} /></Button>}</div>
        {s.derniersDossiers.length === 0 ? <p className="etat-chargement">Aucun dossier pour l’instant.</p>
          : <DataTable headers={["N° RAPPORT", "IMPORTÉ LE", "COMPAGNIE", "MONTANT", "STATUT", ""]}>{s.derniersDossiers.map((d) => <tr key={d.numeroRapport}>
            <td><strong>{d.numeroRapport}</strong></td><td>{date(d.importeLe)}</td><td>{d.compagnie ?? "—"}</td><td className="money">{montant(d.montantIndemnisation)}</td>
            <td><StatusBadge statut={d.statut} /></td><td><Button variant="quiet" onClick={() => navigate(adresseDossier(d.numeroRapport, d.statut, role))}>Ouvrir</Button></td></tr>)}</DataTable>}
      </article>
    </section>
  </main>;
}
