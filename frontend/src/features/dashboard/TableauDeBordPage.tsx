import { useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { Kpi } from "@/components/ui/Kpi";
import { PageHead } from "@/components/ui/PageHead";
import { CLASSES_STATUT, StatusBadge } from "@/components/ui/StatusBadge";
import { BarChart } from "@/components/charts/BarChart";
import { HorizontalBars } from "@/components/charts/HorizontalBars";
import { LIBELLES_STATUT } from "@/domain/referentiels";
import { alertes, derniersImports, dossiersParCompagnie, dossiersParZone, repartitionStatuts } from "@/mocks/donnees";

/** Tableau de bord (F10). Toutes les valeurs sont encore celles de la maquette. */
export default function TableauDeBordPage() {
  const navigate = useNavigate();
  const versDossiers = () => navigate(ROUTES.dossiers);
  return <main className="main dashboard-page">
    <PageHead title="Tableau de bord" subtitle="Vue d’ensemble de l’activité du cabinet" actions={<button className="select-period"><Icon name="clock" size={15} />Octobre 2026 <span>⌄</span></button>} />
    <section className="kpi-grid"><Kpi label="Dossiers traités" value="148" meta="+12 % par rapport au mois dernier" icon="folder" /><Kpi label="Montant total indemnisé" value="356 400 DH" icon="chart" tone="green" /><Kpi label="Coût moyen par sinistre" value="2 408 DH" icon="car" tone="blue" /><Kpi label="En attente de validation" value="9" icon="clock" tone="amber" /></section>
    <article className="card status-overview"><div className="card-head"><div><h2>Dossiers par statut</h2><p>Répartition des 148 dossiers traités</p></div><Button variant="quiet" onClick={versDossiers}>Voir les dossiers <Icon name="arrow" size={14} /></Button></div><div className="segmented">{repartitionStatuts.map(([s, v]) => <button key={s} className={CLASSES_STATUT[s]} style={{ flexGrow: Math.max(v, 2) }} onClick={versDossiers} title={`${LIBELLES_STATUT[s]} : ${v}`} />)}</div><div className="segment-legend">{repartitionStatuts.map(([s, v]) => <button key={s} onClick={versDossiers}><i className={CLASSES_STATUT[s]} /><span>{LIBELLES_STATUT[s]}</span><b>{v}</b></button>)}</div></article>
    <section className="chart-grid">
      <article className="card chart-card"><div className="card-head"><div><h2>Montants indemnisés par mois</h2><p>En milliers de dirhams (DH)</p></div><span className="chart-total">356,4 kDH</span></div><BarChart /></article>
      <article className="card chart-card"><div className="card-head"><div><h2>Répartition main d’œuvre / fournitures</h2><p>Comparatif mensuel en DH</p></div><div className="mini-legend"><i />Main d’œuvre <i />Fournitures</div></div><BarChart stacked /></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Sinistres par zone de choc</h2><p>Nombre de dossiers</p></div></div><HorizontalBars data={dossiersParZone} /></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Dossiers par compagnie d’assurance</h2><p>Top 5 des compagnies</p></div></div><HorizontalBars data={dossiersParCompagnie} /></article>
    </section>
    <section className="bottom-grid">
      <article className="card alerts-card"><div className="card-head"><div><h2>Alertes récentes</h2><p>Contrôles nécessitant votre attention</p></div><span className="alert-count">6</span></div><div className="alert-list">{alertes.map(([tone, ref, text]) => <button key={ref} onClick={() => navigate(ROUTES.verification(ref))}><span className={tone}><Icon name="warning" size={15} /></span><div><strong>Rapport {ref}</strong><p>{text}</p></div><Icon name="chevron" size={14} /></button>)}</div><Button variant="secondary" onClick={versDossiers}>Voir toutes les alertes</Button></article>
      <article className="card latest-card"><div className="card-head"><div><h2>Derniers imports</h2><p>5 rapports ajoutés récemment</p></div><Button variant="quiet" onClick={() => navigate(ROUTES.imports)}>Importer <Icon name="arrow" size={14} /></Button></div><DataTable headers={["N° RAPPORT", "DATE", "COMPAGNIE", "MONTANT", "STATUT", ""]}>{derniersImports.map(r => <tr key={r[0]}><td><strong>{r[0]}</strong></td><td>{r[1]}</td><td>{r[2]}</td><td className="money">{r[3]}</td><td><StatusBadge statut={r[4]} /></td><td><Button variant="quiet" onClick={() => navigate(r[4] === "VALIDE" ? ROUTES.dossier(r[0]) : ROUTES.verification(r[0]))}>Ouvrir</Button></td></tr>)}</DataTable></article>
    </section>
  </main>;
}
