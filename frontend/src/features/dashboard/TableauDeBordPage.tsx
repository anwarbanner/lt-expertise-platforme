import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { cles, tableauDeBord } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { adresseDossier, peutAcceder, ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { Kpi } from "@/components/ui/Kpi";
import { PageHead } from "@/components/ui/PageHead";
import { CLASSES_STATUT, StatusBadge } from "@/components/ui/StatusBadge";
import { BarChart } from "@/components/charts/BarChart";
import { HorizontalBars } from "@/components/charts/HorizontalBars";
import { date, mois, montant, montantArrondi, nombre } from "@/domain/format";
import type { TableauDeBord } from "@/domain/dossiers";
import { LIBELLES_STATUT, ZONES_CHOC, type Role, type StatutDossier } from "@/domain/referentiels";
import { useUtilisateur } from "@/features/auth/AuthContext";

const ORDRE_STATUTS: StatutDossier[] = ["VALIDE", "PRET_A_VALIDER", "A_VERIFIER", "A_COMPLETER", "IMPORTE", "EN_COURS", "ARCHIVE"];

const SOUS_TITRES: Record<Role, string> = {
  ADMINISTRATEUR: "Vue d’ensemble de l’activité du cabinet",
  GESTIONNAIRE: "Vos dossiers à traiter et les anomalies à corriger",
  CONSULTATION: "Statistiques des sinistres",
};

/** Douze derniers mois, du plus récent au plus ancien, au format « AAAA-MM ». */
function derniersMois(): string[] {
  const maintenant = new Date();
  return Array.from({ length: 12 }, (_, i) => {
    const d = new Date(maintenant.getFullYear(), maintenant.getMonth() - i, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  });
}

interface Vue {
  s: TableauDeBord;
  role: Role;
  versDossiers: (parametres?: string) => void;
}

/**
 * Tableau de bord (F10), selon le rôle (P9) : l'administrateur voit toute l'activité et les montants ; le gestionnaire
 * ses tâches (dossiers à traiter, anomalies, derniers dossiers) ; la consultation les statistiques sans montant.
 */
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

  if (stats.isPending) return <main className="main dashboard-page"><PageHead title="Tableau de bord" subtitle={SOUS_TITRES[role]} actions={selecteur} /><p className="etat-chargement">Chargement des statistiques…</p></main>;
  if (stats.isError) return <main className="main dashboard-page"><PageHead title="Tableau de bord" actions={selecteur} /><p className="import-erreur" role="alert">{messageErreur(stats.error, "Statistiques indisponibles.")}</p></main>;

  const vue: Vue = { s: stats.data, role, versDossiers };
  return <main className="main dashboard-page">
    <PageHead title="Tableau de bord" subtitle={periode ? `${SOUS_TITRES[role]} · ${mois(periode, true)}` : SOUS_TITRES[role]} actions={selecteur} />
    {role === "ADMINISTRATEUR" ? <VueAdministrateur {...vue} /> : role === "GESTIONNAIRE" ? <VueGestionnaire {...vue} /> : <VueConsultation {...vue} />}
  </main>;
}

function VueAdministrateur({ s, role, versDossiers }: Vue) {
  const totalMontantPeriode = s.mensuel.reduce((t, m) => t + (m.montant ?? 0), 0);
  return <>
    <section className="kpi-grid">
      <Kpi label="Dossiers traités" value={nombre(s.dossiers)} meta={`${s.dossiersEnAnomalie} en anomalie`} icon="folder" />
      <Kpi label="Montant total des indemnisations" value={montantArrondi(s.montantTotal)} icon="chart" tone="green" />
      <Kpi label="Coût moyen par sinistre" value={montantArrondi(s.coutMoyen)} icon="car" tone="blue" />
      <Kpi label="En attente de validation" value={nombre(s.enAttenteValidation)} icon="clock" tone="amber" />
    </section>
    <RepartitionStatuts s={s} versDossiers={versDossiers} />
    <section className="chart-grid">
      <article className="card chart-card"><div className="card-head"><div><h2>Montants indemnisés par mois</h2><p>12 derniers mois, en dirhams (date du rapport)</p></div><span className="chart-total">{montantArrondi(totalMontantPeriode)}</span></div>
        <BarChart barres={s.mensuel.map((m) => ({ mois: m.mois, valeur: m.montant ?? 0 }))} /></article>
      <article className="card chart-card"><div className="card-head"><div><h2>Répartition main d’œuvre / fournitures</h2><p>Totaux TTC par mois</p></div><div className="mini-legend"><i />Main d’œuvre <i />Fournitures</div></div>
        <BarChart barres={s.mensuel.map((m) => ({ mois: m.mois, valeur: m.mainOeuvreTtc ?? 0, valeur2: m.fournituresTtc ?? 0 }))} /></article>
      <ZonesChoc s={s} versDossiers={versDossiers} />
      <Compagnies s={s} versDossiers={versDossiers} />
    </section>
    <section className="bottom-grid">
      <Alertes s={s} role={role} versDossiers={versDossiers} />
      <DerniersDossiers s={s} role={role} avecMontant titre="Derniers imports" sousTitre="Dossiers ajoutés récemment" />
    </section>
  </>;
}

function VueGestionnaire({ s, role, versDossiers }: Vue) {
  const n = (statut: StatutDossier) => nombre(s.parStatut[statut] ?? 0);
  return <>
    <section className="kpi-grid">
      <Kpi label="À vérifier" value={n("A_VERIFIER")} meta="Contrôles à revoir" icon="warning" tone="amber" />
      <Kpi label="À compléter" value={n("A_COMPLETER")} meta="Champs à saisir" icon="file" tone="blue" />
      <Kpi label="Prêts à valider" value={nombre(s.enAttenteValidation)} meta="En attente de votre validation" icon="clock" tone="green" />
      <Kpi label="Dossiers en anomalie" value={nombre(s.dossiersEnAnomalie)} meta="Contrôles bloquants" icon="folder" />
    </section>
    <RepartitionStatuts s={s} versDossiers={versDossiers} />
    <section className="bottom-grid">
      <Alertes s={s} role={role} versDossiers={versDossiers} />
      <DerniersDossiers s={s} role={role} titre="Derniers dossiers traités" sousTitre="Dossiers importés récemment et leur statut" />
    </section>
  </>;
}

function VueConsultation({ s, versDossiers }: Vue) {
  const zone = s.parZone[0];
  const compagnie = s.parCompagnie[0];
  return <>
    <section className="kpi-grid">
      <Kpi label="Dossiers" value={nombre(s.dossiers)} icon="folder" />
      <Kpi label="Dossiers validés" value={nombre(s.parStatut.VALIDE ?? 0)} icon="chart" tone="green" />
      <Kpi label="Zone de choc la plus fréquente" value={zone ? `${zone.cle} · ${ZONES_CHOC[Number(zone.cle)] ?? "?"}` : "—"} meta={zone ? `${nombre(zone.nombre)} dossier(s)` : undefined} icon="car" tone="blue" />
      <Kpi label="Première compagnie" value={compagnie?.cle ?? "—"} meta={compagnie ? `${nombre(compagnie.nombre)} dossier(s)` : undefined} icon="users" tone="amber" />
    </section>
    <section className="chart-grid">
      <ZonesChoc s={s} versDossiers={versDossiers} />
      <Compagnies s={s} versDossiers={versDossiers} />
      <article className="card chart-card"><div className="card-head"><div><h2>Dossiers par mois</h2><p>12 derniers mois (date du rapport)</p></div></div>
        <BarChart unite="dossiers" barres={s.mensuel.map((m) => ({ mois: m.mois, valeur: m.nombre }))} /></article>
    </section>
    <RepartitionStatuts s={s} versDossiers={versDossiers} />
  </>;
}

function RepartitionStatuts({ s, versDossiers }: Pick<Vue, "s" | "versDossiers">) {
  const statuts = ORDRE_STATUTS.map((st) => [st, s.parStatut[st] ?? 0] as const).filter(([st, n]) => n > 0 || st !== "EN_COURS");
  const total = statuts.reduce((t, [, n]) => t + n, 0);
  return <article className="card status-overview">
    <div className="card-head"><div><h2>Dossiers par statut</h2><p>Répartition des {nombre(total)} dossiers</p></div><Button variant="quiet" onClick={() => versDossiers()}>Voir les dossiers <Icon name="arrow" size={14} /></Button></div>
    <div className="segmented">{statuts.filter(([, n]) => n > 0).map(([st, n]) => <button key={st} className={CLASSES_STATUT[st]} style={{ flexGrow: n }} onClick={() => versDossiers(`?statut=${st}`)} title={`${LIBELLES_STATUT[st]} : ${n}`} aria-label={`${LIBELLES_STATUT[st]} : ${n}`} />)}</div>
    <div className="segment-legend">{statuts.map(([st, n]) => <button key={st} onClick={() => versDossiers(`?statut=${st}`)}><i className={CLASSES_STATUT[st]} /><span>{LIBELLES_STATUT[st]}</span><b>{n}</b></button>)}</div>
  </article>;
}

function ZonesChoc({ s, versDossiers }: Pick<Vue, "s" | "versDossiers">) {
  const zones = s.parZone.map((z) => [`${z.cle} · ${ZONES_CHOC[Number(z.cle)] ?? "?"}`, z.nombre] as const);
  return <article className="card chart-card compact"><div className="card-head"><div><h2>Sinistres par zone de choc</h2><p>Nombre de dossiers</p></div></div>
    <HorizontalBars data={zones} onSelect={(i) => versDossiers(`?zone=${s.parZone[i].cle}`)} /></article>;
}

function Compagnies({ s, versDossiers }: Pick<Vue, "s" | "versDossiers">) {
  const compagnies = s.parCompagnie.map((c) => [c.cle, c.nombre] as const);
  return <article className="card chart-card compact"><div className="card-head"><div><h2>Dossiers par compagnie d’assurance</h2><p>Les 5 premières compagnies</p></div></div>
    <HorizontalBars data={compagnies} onSelect={(i) => versDossiers(`?compagnie=${encodeURIComponent(s.parCompagnie[i].cle)}`)} /></article>;
}

function Alertes({ s, role, versDossiers }: Vue) {
  const navigate = useNavigate();
  return <article className="card alerts-card"><div className="card-head"><div><h2>Alertes récentes</h2><p>Contrôles nécessitant votre attention</p></div><span className="alert-count">{s.dossiersEnAnomalie}</span></div>
    <div className="alert-list">{s.alertes.length === 0 ? <p className="etat-chargement">Aucune anomalie.</p> : s.alertes.map((a) =>
      <button key={a.numeroRapport} onClick={() => navigate(adresseDossier(a.numeroRapport, "A_VERIFIER", role))}>
        <span className={a.incoherences > 0 ? "error" : "warn"}><Icon name="warning" size={15} /></span>
        <div><strong>Rapport {a.numeroRapport}</strong><p>{a.incoherences > 0 ? `${a.premierControle ?? "Contrôle en échec"}${a.incoherences > 1 ? ` (+${a.incoherences - 1})` : ""}` : `${a.champsManquants} champ${a.champsManquants > 1 ? "s" : ""} à compléter`}</p></div>
        <Icon name="chevron" size={14} />
      </button>)}</div>
    <Button variant="secondary" onClick={() => versDossiers("?anomalies=true")}>Voir toutes les alertes</Button></article>;
}

function DerniersDossiers({ s, role, avecMontant = false, titre, sousTitre }: Pick<Vue, "s" | "role"> & { avecMontant?: boolean; titre: string; sousTitre: string }) {
  const navigate = useNavigate();
  const entetes = ["N° RAPPORT", "IMPORTÉ LE", "COMPAGNIE", ...(avecMontant ? ["MONTANT"] : []), "STATUT", ""];
  return <article className="card latest-card"><div className="card-head"><div><h2>{titre}</h2><p>{sousTitre}</p></div>{peutAcceder(role, "imports") && <Button variant="quiet" onClick={() => navigate(ROUTES.imports)}>Importer <Icon name="arrow" size={14} /></Button>}</div>
    {s.derniersDossiers.length === 0 ? <p className="etat-chargement">Aucun dossier pour l’instant.</p>
      : <DataTable headers={entetes}>{s.derniersDossiers.map((d) => <tr key={d.numeroRapport}>
        <td><strong>{d.numeroRapport}</strong></td><td>{date(d.importeLe)}</td><td>{d.compagnie ?? "—"}</td>{avecMontant && <td className="money">{montant(d.montantIndemnisation)}</td>}
        <td><StatusBadge statut={d.statut} /></td><td><Button variant="quiet" onClick={() => navigate(adresseDossier(d.numeroRapport, d.statut, role))}>Ouvrir</Button></td></tr>)}</DataTable>}
  </article>;
}
