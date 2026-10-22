import { useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Filter } from "@/components/ui/Field";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import type { StatutAffiche } from "@/domain/referentiels";
import { dossiers } from "@/mocks/donnees";

/** Liste, recherche et filtres des dossiers (F7, F8). */
export default function DossiersPage() {
  const navigate = useNavigate();
  const ouvrir = (numero: string, statut: StatutAffiche) => navigate(statut === "VALIDE" ? ROUTES.dossier(numero) : ROUTES.verification(numero));
  return <main className="main folders-page"><PageHead title="Dossiers" subtitle="Consultez et gérez l’ensemble des rapports d’expertise" actions={<Button variant="secondary" icon="download">Exporter en Excel</Button>} />
    <article className="card filter-card"><div className="filter-search"><Icon name="search" size={16} /><input placeholder="N° rapport, réf. sinistre, immatriculation, nom, réparateur…" /></div><Filter label="Du" value="01/10/2026" /><Filter label="Au" value="31/10/2026" /><Filter label="Compagnie" value="Toutes" /><Filter label="Statut" value="Tous les statuts" /><Filter label="Zone de choc" value="Toutes" /><Filter label="Montant" value="Toutes" /><Button variant="quiet">Réinitialiser</Button></article>
    <div className="quick-tabs">{([["Tous", 148], ["À compléter", 8], ["À vérifier", 6], ["Prêts à valider", 9], ["Validés", 121]] as const).map(([l, v], i) => <button className={i === 0 ? "active" : ""} key={l}>{l}<b>{v}</b></button>)}</div>
    <article className="card folder-table"><DataTable headers={["N° RAPPORT ↕", "DATE SINISTRE ↕", "ASSURÉ", "IMMATRICULATION", "COMPAGNIE", "ZONE DE CHOC", "INDEMNISATION (DH) ↕", "STATUT", ""]}>{dossiers.map(r => <tr key={r[0]} onDoubleClick={() => ouvrir(r[0], r[7])}><td><strong>{r[0]}</strong></td><td>{r[1]}</td><td>{r[2]}</td><td><span className="plate">{r[3]}</span></td><td>{r[4]}</td><td>{r[5]}</td><td className="money">{r[6]}</td><td><StatusBadge statut={r[7]} /></td><td><div className="action-menu"><Button variant="quiet" onClick={() => ouvrir(r[0], r[7])}>Ouvrir</Button><button><Icon name="more" size={17} /></button></div></td></tr>)}</DataTable><div className="pagination"><span>1–10 sur 148</span><div><button disabled>‹</button><button className="active">1</button><button>2</button><button>3</button><span>…</span><button>15</button><button>›</button></div></div></article>
  </main>;
}
