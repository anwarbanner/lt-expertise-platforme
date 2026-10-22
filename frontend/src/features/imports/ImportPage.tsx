import { useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { TAILLE_MAX_IMPORT_MO } from "@/domain/imports";
import { lignesImport } from "@/mocks/donnees";

/** Import de PDF (F1). Le backend est prêt : POST /api/imports renvoie un RecapitulatifImport. */
export default function ImportPage() {
  const navigate = useNavigate();
  return <main className="main"><PageHead title="Importer des rapports" subtitle="Import et extraction automatique de rapports d’expertise PDF" actions={<Button variant="secondary" icon="folder" onClick={() => navigate(ROUTES.dossiers)}>Voir les dossiers</Button>} />
    <article className="dropzone"><div className="drop-icon"><Icon name="upload" size={25} /></div><h2>Déposez vos rapports d’expertise PDF ici</h2><p>ou cliquez pour parcourir vos fichiers</p><span>Un ou plusieurs fichiers PDF · {TAILLE_MAX_IMPORT_MO} Mo maximum chacun</span><Button icon="plus">Sélectionner des fichiers</Button></article>
    <article className="card process-card"><div className="section-head"><div><h2>Extraction en cours</h2><p>2 fichiers en traitement</p></div><span className="live-dot">TRAITEMENT ACTIF</span></div>
      <div className="process-list">{([["rapport_268202-0316.pdf", 72, "Extraction des tableaux"], ["expertise_vehicule_0317.pdf", 38, "Extraction des champs"]] as const).map(([n, p, s]) => <div className="process-item" key={n}><span className="file-icon"><Icon name="file" size={19} /></span><div><strong>{n}</strong><div className="progress"><i style={{ width: `${p}%` }} /></div><small>{s} · {p}%</small></div><button><Icon name="more" size={18} /></button></div>)}</div>
      <div className="steps">{["Lecture du PDF", "Extraction des champs", "Extraction des tableaux", "Contrôles de cohérence"].map((s, i) => <span className={i < 2 ? "done" : i === 2 ? "current" : ""} key={s}><i>{i < 2 ? <Icon name="check" size={11} /> : i + 1}</i>{s}</span>)}</div>
    </article>
    <article className="card result-card"><div className="section-head"><div><h2>Résultat de l’import</h2><p>Session du 28 octobre 2026 à 10:42</p></div><Button variant="secondary" icon="download">Exporter le rapport</Button></div>
      <div className="result-counts">{[["Importés", "12", "navy"], ["Prêts à valider", "7", "blue"], ["À compléter", "3", "amber"], ["À vérifier", "1", "red"], ["Refusés", "1", "gray"]].map(([l, v, t]) => <div key={l}><i className={t} /><span>{l}</span><strong>{v}</strong></div>)}</div>
      <DataTable headers={["FICHIER", "N° RAPPORT", "INDEMNISATION", "CONTRÔLES", "CHAMPS", "STATUT", "ACTION"]}>{lignesImport.map(r => <tr key={r[0]} className={r[5] === "REFUSE" ? "rejected" : ""}><td><div className="filename"><Icon name="file" size={16} /><div><strong>{r[0]}</strong>{r[6] && <small>{r[6]}</small>}</div></div></td><td>{r[1]}</td><td className="money">{r[2]}</td><td>{r[3]}</td><td>{r[4]}</td><td><StatusBadge statut={r[5]} /></td><td>{r[5] !== "REFUSE" && <Button variant="secondary" onClick={() => navigate(ROUTES.verification(r[1]))}>Vérifier</Button>}</td></tr>)}</DataTable>
    </article>
  </main>;
}
