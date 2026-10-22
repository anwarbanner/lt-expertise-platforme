import { useState, type ReactNode } from "react";

type Screen = "dashboard" | "import" | "folders" | "pricing" | "generate" | "verify" | "detail" | "users" | "login";
type IconName = "grid" | "upload" | "folder" | "bell" | "chart" | "users" | "search" | "chevron" | "plus" | "clock" | "check" | "car" | "file" | "more" | "filter" | "arrow" | "download" | "eye" | "lock" | "warning";

const paths: Record<IconName, ReactNode> = {
  grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
  upload: <><path d="M12 16V4m-5 5 5-5 5 5M5 20h14" /></>,
  folder: <path d="M3 8a3 3 0 0 1 3-3h4l2 2h6a3 3 0 0 1 3 3v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z" />,
  bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9m-11 13h4" /></>,
  chart: <><path d="M4 20V10m6 10V4m6 16v-7m6 7H2" /></>,
  users: <><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  chevron: <path d="m9 18 6-6-6-6" />, plus: <path d="M12 5v14M5 12h14" />, arrow: <path d="M5 12h14m-4-4 4 4-4 4" />,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  check: <path d="m5 12 4 4L19 6" />, warning: <><path d="M12 3 2.5 20h19Z" /><path d="M12 9v4m0 3h.01" /></>,
  car: <><path d="m5 16-1 2v2h2l1-2h10l1 2h2v-2l-1-2-2-6H7Zm0-2h14" /><path d="M8 17h.01M16 17h.01" /></>,
  file: <><path d="M6 2h8l4 4v16H6Z" /><path d="M14 2v5h5M9 13h6M9 17h5" /></>,
  more: <><circle cx="5" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="12" cy="12" r="1" fill="currentColor" stroke="none" /><circle cx="19" cy="12" r="1" fill="currentColor" stroke="none" /></>,
  filter: <path d="M4 5h16M7 12h10M10 19h4" />, download: <path d="M12 3v12m-5-5 5 5 5-5M4 20h16" />,
  eye: <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6S2 12 2 12" /><circle cx="12" cy="12" r="2.5" /></>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
};

function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  return <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name]}</svg>;
}

function Button({ children, variant = "primary", icon, onClick, disabled, title }: { children: ReactNode; variant?: "primary" | "secondary" | "danger" | "quiet"; icon?: IconName; onClick?: () => void; disabled?: boolean; title?: string }) {
  return <button className={`button ${variant}`} onClick={onClick} disabled={disabled} title={title}>{icon && <Icon name={icon} size={15} />}{children}</button>;
}

const statusClass: Record<string, string> = { "Importé": "imported", "À compléter": "incomplete", "À vérifier": "verify", "Prêt à valider": "ready", "Validé": "validated", "Archivé": "archived", "Refusé": "refused" };
function Status({ children }: { children: string }) { return <span className={`status ${statusClass[children] || "imported"}`}><i />{children}</span>; }

const menu: [string, IconName, Screen][] = [
  ["Tableau de bord", "grid", "dashboard"], ["Importer des rapports", "upload", "import"], ["Dossiers", "folder", "folders"],
  ["Analyse des prix", "chart", "pricing"],
  ["Génération des rapports", "file", "generate"],
  ["Utilisateurs", "users", "users"],
];

function Sidebar({ screen, go, collapsed, toggle }: { screen: Screen; go: (s: Screen) => void; collapsed: boolean; toggle: () => void }) {
  return <aside className="sidebar">
    <div className="logo-wrap"><img src="/logo-lt-expertise.png" alt="LT Expertise" className="official-logo" /></div>
    <button className="sidebar-toggle" onClick={toggle} aria-label={collapsed?"Développer la navigation":"Réduire la navigation"} title={collapsed?"Développer":"Réduire"}><Icon name="chevron" size={15}/></button>
    <nav className="nav"><span className="nav-label">ESPACE DE TRAVAIL</span>{menu.map(([label, icon, target]) =>
      <button key={label} className={`nav-item ${label === "Utilisateurs" ? "users-item" : ""} ${screen === target ? "active" : ""}`} onClick={() => go(target)} title={collapsed?label:undefined}><Icon name={icon} size={19} /><span>{label}</span></button>
    )}</nav>
    <div className="sidebar-help"><span><Icon name="file" size={17} /></span><div><strong>Centre d’aide</strong><small>Guides et assistance</small></div><Icon name="chevron" size={15} /></div>
    <p className="version">LT Expertise • v2.4.0</p>
  </aside>;
}

function Header({ go }: { go: (s: Screen) => void }) {
  return <header className="topbar">
    <div className="search"><Icon name="search" size={18} /><input aria-label="Recherche globale" placeholder="Rechercher un rapport, une immatriculation, un nom…" /><kbd>⌘ K</kbd></div>
    <button className="notification" aria-label="Notifications"><Icon name="bell" size={20} /><i /></button>
    <button className="user" onClick={() => go("login")}><span className="avatar">SB</span><div><strong>Samira Benali</strong><small>Gestionnaire</small></div><Icon name="chevron" size={15} /></button>
  </header>;
}

function Layout({ screen, go, children }: { screen: Screen; go: (s: Screen) => void; children: ReactNode }) {
  const [collapsed,setCollapsed]=useState(false);
  return <div className={`app-shell ${collapsed?"sidebar-collapsed":""}`}><Sidebar screen={screen} go={go} collapsed={collapsed} toggle={()=>setCollapsed(!collapsed)} /><div className="workspace"><Header go={go} />{children}</div></div>;
}

function PageHead({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return <><div className="page-heading"><div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div><div className="head-actions">{actions}</div></div><div className="brand-rule"><i /><i /></div></>;
}

function Kpi({ label, value, meta, icon, tone = "navy" }: { label: string; value: string; meta?: string; icon: IconName; tone?: string }) {
  return <article className="kpi"><div className={`kpi-icon ${tone}`}><Icon name={icon} size={20} /></div><div className="kpi-copy"><span>{label}</span><strong>{value}</strong>{meta && <small>{meta}</small>}</div></article>;
}

const statusData = [["Validé",121],["Prêt à valider",9],["À vérifier",6],["À compléter",8],["Importé",4],["Archivé",0]] as const;
const recentImports = [
  ["268202-0314","28/10/2026","Wafa Assurance","3 840,00 DH","Prêt à valider"],
  ["268202-0313","28/10/2026","Sanlam Maroc","5 120,00 DH","À compléter"],
  ["268202-0312","27/10/2026","RMA Assurance","2 760,00 DH","Validé"],
  ["268202-0311","27/10/2026","AtlantaSanad","6 400,00 DH","À vérifier"],
  ["268202-0310","26/10/2026","AXA Assurance","1 980,00 DH","Importé"],
];

function BarChart({ stacked = false }: { stacked?: boolean }) {
  const vals = [48,57,46,69,61,78];
  return <div className="bar-chart"><div className="chart-y"><span>80k</span><span>60k</span><span>40k</span><span>20k</span><span>0</span></div><div className="bars">{vals.map((v,i)=><div className="bar-col" key={i}><div className="bar-slot">{stacked ? <><i className="bar-light" style={{height:`${v*.45}%`}}/><i className="bar-main stacked" style={{height:`${v*.55}%`}}/></> : <i className={`bar-main ${i===5?"accent":""}`} style={{height:`${v}%`}}/>}</div><span>{["Mai","Juin","Juil.","Août","Sept.","Oct."][i]}</span></div>)}</div></div>;
}

function HorizontalBars({ type }: { type: "zones" | "insurance" }) {
  const data = type === "zones" ? [["Avant",84],["Arrière",66],["Latéral droit",42],["Latéral gauche",35],["Central",22],["Toit",12]] : [["Wafa Assurance",78],["RMA Assurance",62],["Sanlam Maroc",54],["AXA Assurance",43],["AtlantaSanad",31]];
  return <div className="hbars">{data.map(([label,val],i)=><div className="hbar" key={label}><span>{label}</span><div><i style={{width:`${val}%`}} className={i===0?"highlight":""}/></div><b>{val}</b></div>)}</div>;
}

function Dashboard({ go }: { go: (s: Screen) => void }) {
  const alerts = [
    ["error","268202-0291","Total TTC incohérent (attendu 2 400,00, trouvé 2 640,00)"],
    ["warn","268202-0302","Taux horaire 75 DH au lieu de 60 DH"],
    ["warn","268202-0288","6 champs à compléter"],
    ["error","268202-0305","Montant en lettres différent du montant en chiffres"],
  ];
  return <main className="main dashboard-page">
    <PageHead title="Tableau de bord" subtitle="Vue d’ensemble de l’activité du cabinet" actions={<button className="select-period"><Icon name="clock" size={15}/>Octobre 2026 <span>⌄</span></button>} />
    <section className="kpi-grid"><Kpi label="Dossiers traités" value="148" meta="+12 % par rapport au mois dernier" icon="folder"/><Kpi label="Montant total indemnisé" value="356 400 DH" icon="chart" tone="green"/><Kpi label="Coût moyen par sinistre" value="2 408 DH" icon="car" tone="blue"/><Kpi label="En attente de validation" value="9" icon="clock" tone="amber"/></section>
    <article className="card status-overview"><div className="card-head"><div><h2>Dossiers par statut</h2><p>Répartition des 148 dossiers traités</p></div><Button variant="quiet" onClick={()=>go("folders")}>Voir les dossiers <Icon name="arrow" size={14}/></Button></div><div className="segmented">{statusData.map(([s,v])=><button key={s} className={statusClass[s]} style={{flexGrow:Math.max(v,2)}} onClick={()=>go("folders")} title={`${s} : ${v}`}/>)}</div><div className="segment-legend">{statusData.map(([s,v])=><button key={s} onClick={()=>go("folders")}><i className={statusClass[s]}/><span>{s}</span><b>{v}</b></button>)}</div></article>
    <section className="chart-grid">
      <article className="card chart-card"><div className="card-head"><div><h2>Montants indemnisés par mois</h2><p>En milliers de dirhams (DH)</p></div><span className="chart-total">356,4 kDH</span></div><BarChart/></article>
      <article className="card chart-card"><div className="card-head"><div><h2>Répartition main d’œuvre / fournitures</h2><p>Comparatif mensuel en DH</p></div><div className="mini-legend"><i/>Main d’œuvre <i/>Fournitures</div></div><BarChart stacked/></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Sinistres par zone de choc</h2><p>Nombre de dossiers</p></div></div><HorizontalBars type="zones"/></article>
      <article className="card chart-card compact"><div className="card-head"><div><h2>Dossiers par compagnie d’assurance</h2><p>Top 5 des compagnies</p></div></div><HorizontalBars type="insurance"/></article>
    </section>
    <section className="bottom-grid">
      <article className="card alerts-card"><div className="card-head"><div><h2>Alertes récentes</h2><p>Contrôles nécessitant votre attention</p></div><span className="alert-count">6</span></div><div className="alert-list">{alerts.map(([tone,ref,text])=><button key={ref} onClick={()=>go("verify")}><span className={tone}><Icon name="warning" size={15}/></span><div><strong>Rapport {ref}</strong><p>{text}</p></div><Icon name="chevron" size={14}/></button>)}</div><Button variant="secondary" onClick={()=>go("verify")}>Voir toutes les alertes</Button></article>
      <article className="card latest-card"><div className="card-head"><div><h2>Derniers imports</h2><p>5 rapports ajoutés récemment</p></div><Button variant="quiet" onClick={()=>go("import")}>Importer <Icon name="arrow" size={14}/></Button></div><DataTable headers={["N° RAPPORT","DATE","COMPAGNIE","MONTANT","STATUT",""]}>{recentImports.map(r=><tr key={r[0]}><td><strong>{r[0]}</strong></td><td>{r[1]}</td><td>{r[2]}</td><td className="money">{r[3]}</td><td><Status>{r[4]}</Status></td><td><Button variant="quiet" onClick={()=>go(r[4]==="Validé"?"detail":"verify")}>Ouvrir</Button></td></tr>)}</DataTable></article>
    </section>
  </main>;
}

function DataTable({ headers, children }: { headers: string[]; children: ReactNode }) {
  return <div className="table-scroll"><table><thead><tr>{headers.map((h,i)=><th key={i}>{h}</th>)}</tr></thead><tbody>{children}</tbody></table></div>;
}

const importRows = [
  ["rapport_268202-0314.pdf","268202-0314","3 840,00 DH","27/27","0","Prêt à valider",""],
  ["expertise_0313.pdf","268202-0313","5 120,00 DH","24/27","3","À compléter",""],
  ["rapport_268202-0312.pdf","268202-0312","2 760,00 DH","27/27","0","Prêt à valider",""],
  ["scan_expertise_0309.pdf","268202-0309","—","—","—","Refusé","Modèle de PDF non reconnu"],
  ["rapport_0284_copie.pdf","268202-0284","2 400,00 DH","27/27","0","Refusé","Doublon : rapport 268202-0284 déjà importé"],
];

function ImportScreen({ go }: { go: (s: Screen)=>void }) {
  return <main className="main"><PageHead title="Importer des rapports" subtitle="Import et extraction automatique de rapports d’expertise PDF" actions={<Button variant="secondary" icon="folder" onClick={()=>go("folders")}>Voir les dossiers</Button>}/>
    <article className="dropzone"><div className="drop-icon"><Icon name="upload" size={25}/></div><h2>Déposez vos rapports d’expertise PDF ici</h2><p>ou cliquez pour parcourir vos fichiers</p><span>Un ou plusieurs fichiers PDF · 10 Mo maximum chacun</span><Button icon="plus">Sélectionner des fichiers</Button></article>
    <article className="card process-card"><div className="section-head"><div><h2>Extraction en cours</h2><p>2 fichiers en traitement</p></div><span className="live-dot">TRAITEMENT ACTIF</span></div>
      <div className="process-list">{[["rapport_268202-0316.pdf",72,"Extraction des tableaux"],["expertise_vehicule_0317.pdf",38,"Extraction des champs"]].map(([n,p,s])=><div className="process-item" key={String(n)}><span className="file-icon"><Icon name="file" size={19}/></span><div><strong>{n}</strong><div className="progress"><i style={{width:`${p}%`}}/></div><small>{s} · {p}%</small></div><button><Icon name="more" size={18}/></button></div>)}</div>
      <div className="steps">{["Lecture du PDF","Extraction des champs","Extraction des tableaux","Contrôles de cohérence"].map((s,i)=><span className={i<2?"done":i===2?"current":""} key={s}><i>{i<2?<Icon name="check" size={11}/>:i+1}</i>{s}</span>)}</div>
    </article>
    <article className="card result-card"><div className="section-head"><div><h2>Résultat de l’import</h2><p>Session du 28 octobre 2026 à 10:42</p></div><Button variant="secondary" icon="download">Exporter le rapport</Button></div>
      <div className="result-counts">{[["Importés","12","navy"],["Prêts à valider","7","blue"],["À compléter","3","amber"],["À vérifier","1","red"],["Refusés","1","gray"]].map(([l,v,t])=><div key={l}><i className={t}/><span>{l}</span><strong>{v}</strong></div>)}</div>
      <DataTable headers={["FICHIER","N° RAPPORT","INDEMNISATION","CONTRÔLES","CHAMPS","STATUT","ACTION"]}>{importRows.map(r=><tr key={r[0]} className={r[5]==="Refusé"?"rejected":""}><td><div className="filename"><Icon name="file" size={16}/><div><strong>{r[0]}</strong>{r[6]&&<small>{r[6]}</small>}</div></div></td><td>{r[1]}</td><td className="money">{r[2]}</td><td>{r[3]}</td><td>{r[4]}</td><td><Status>{r[5]}</Status></td><td>{r[5]!=="Refusé"&&<Button variant="secondary" onClick={()=>go("verify")}>Vérifier</Button>}</td></tr>)}</DataTable>
    </article>
  </main>;
}

const dossiers = [
  ["268202-0314","26/10/2026","Nadia Alaoui","42819-A-6","Wafa Assurance","Avant","3 840,00","Prêt à valider"],
  ["268202-0313","25/10/2026","Youssef Amrani","15274-B-8","Sanlam Maroc","Arrière","5 120,00","À compléter"],
  ["268202-0312","25/10/2026","Salma Idrissi","78421-A-1","RMA Assurance","Latéral droit","2 760,00","Validé"],
  ["268202-0311","24/10/2026","Omar Chraïbi","9567-D-6","AtlantaSanad","Avant","6 400,00","À vérifier"],
  ["268202-0310","23/10/2026","Khadija Bennani","36482-A-3","AXA Assurance","Arrière","1 980,00","Importé"],
  ["268202-0309","22/10/2026","Mehdi El Fassi","85214-B-7","Wafa Assurance","Central","7 250,00","Validé"],
  ["268202-0308","22/10/2026","Imane Tazi","19548-A-2","RMA Assurance","Latéral gauche","4 300,00","Validé"],
  ["268202-0307","21/10/2026","Karim Berrada","62149-D-5","Sanlam Maroc","Avant","8 940,00","À compléter"],
  ["268202-0306","21/10/2026","Sara Lahlou","47510-A-9","AXA Assurance","Toit","3 125,00","Validé"],
  ["268202-0284","20/10/2026","Hassan Mansouri","32148-B-6","Wafa Assurance","Arrière","2 400,00","Validé"],
];

function Folders({ go }: { go:(s:Screen)=>void }) {
  return <main className="main folders-page"><PageHead title="Dossiers" subtitle="Consultez et gérez l’ensemble des rapports d’expertise" actions={<Button variant="secondary" icon="download">Exporter en Excel</Button>}/>
    <article className="card filter-card"><div className="filter-search"><Icon name="search" size={16}/><input placeholder="N° rapport, réf. sinistre, immatriculation, nom, réparateur…"/></div><Filter label="Du" value="01/10/2026"/><Filter label="Au" value="31/10/2026"/><Filter label="Compagnie" value="Toutes"/><Filter label="Statut" value="Tous les statuts"/><Filter label="Zone de choc" value="Toutes"/><Filter label="Montant" value="Toutes"/><Button variant="quiet">Réinitialiser</Button></article>
    <div className="quick-tabs">{[["Tous",148],["À compléter",8],["À vérifier",6],["Prêts à valider",9],["Validés",121]].map(([l,v],i)=><button className={i===0?"active":""} key={l}>{l}<b>{v}</b></button>)}</div>
    <article className="card folder-table"><DataTable headers={["N° RAPPORT ↕","DATE SINISTRE ↕","ASSURÉ","IMMATRICULATION","COMPAGNIE","ZONE DE CHOC","INDEMNISATION (DH) ↕","STATUT",""]}>{dossiers.map(r=><tr key={r[0]} onDoubleClick={()=>go(r[7]==="Validé"?"detail":"verify")}><td><strong>{r[0]}</strong></td><td>{r[1]}</td><td>{r[2]}</td><td><span className="plate">{r[3]}</span></td><td>{r[4]}</td><td>{r[5]}</td><td className="money">{r[6]}</td><td><Status>{r[7]}</Status></td><td><div className="action-menu"><Button variant="quiet" onClick={()=>go(r[7]==="Validé"?"detail":"verify")}>Ouvrir</Button><button><Icon name="more" size={17}/></button></div></td></tr>)}</DataTable><div className="pagination"><span>1–10 sur 148</span><div><button disabled>‹</button><button className="active">1</button><button>2</button><button>3</button><span>…</span><button>15</button><button>›</button></div></div></article>
  </main>;
}
function Filter({label,value}:{label:string;value:string}){return <label className="filter"><span>{label}</span><button>{value}<span>⌄</span></button></label>}

const tabs = ["Sinistre","Parties","Véhicule","Évaluation des dommages","Détail des pièces","Conclusion"];
function Field({ label, value, state }: { label:string; value:string; state?:"warning"|"error" }) { return <label className={`data-field ${state||""}`}><span>{label}</span><input value={value} placeholder={state==="warning"?"À saisir":""} readOnly/>{state==="warning"&&<small>À saisir</small>}{state==="error"&&<small>Valeur incohérente</small>}</label>; }

function VehicleDiagram(){return <div className="vehicle-diagram"><div className="car-body"><div className="windshield"/><div className="cabin"/>{[1,2,3,4,5,6,7,8].map((n,i)=><span key={n} className={`zone z${n} ${n===7?"selected":""}`}>{n}</span>)}</div><div className="diagram-caption"><strong>Zone 7 · Arrière</strong><span>Zone de choc principale</span></div></div>}

function DamageTable({editable=true}:{editable?:boolean}) { const rows=[["MO 1 Tôlerie","20","60,00","1 200,00","20%","1 440,00"],["MO 2 Tôlerie","4","60,00","240,00","20%","288,00"],["MO 3 Tôlerie","—","60,00","0,00","20%","0,00"],["MO Peinture","8","60,00","480,00","20%","576,00"],["MO 1 Mécanique","2","60,00","120,00","20%","144,00"],["MO 2 Mécanique","—","60,00","0,00","20%","0,00"],["MO 3 Mécanique","—","60,00","0,00","20%","0,00"]]; return <><DataTable headers={["POSTE","NBR H","PRIX UNIT.","TOTAL HT","TVA","TOTAL TTC"]}>{rows.map(r=><tr key={r[0]}>{r.map((v,i)=><td key={i} className={i>0?"money":""}>{editable&&i>0?<input value={v} readOnly/>:i===0?<strong>{v}</strong>:v}</td>)}</tr>)}</DataTable><div className="damage-totals"><div><span>Pièces</span><b>400,00 DH</b></div><div><span>Ingrédient peinture</span><b>200,00 DH</b></div><div><span>Petite fourniture</span><b>80,00 DH</b></div><div className="grand"><span>Total général</span><b>2 400,00 DH</b></div></div></> }

function TabContent({tab}:{tab:number}) {
  if(tab===0)return <div className="fields-grid">{[["N° de rapport","268202-0284"],["Réf. sinistre","SIN-2026-10842"],["Date du sinistre","18/10/2026"],["Date de mission","19/10/2026"],["Compagnie","Wafa Assurance"],["Nature du contrat","RC"],["Procédure","Contradictoire"],["Cabinet","LT Expertise"],["Expert","M. Amine El Khatib"]].map(x=><Field key={x[0]} label={x[0]} value={x[1]}/>)}</div>;
  if(tab===1)return <div className="party-grid"><section><h3>Assuré</h3><Field label="Nom complet" value="Hassan Mansouri"/><Field label="Véhicule" value="Renault Clio V"/><Field label="Immatriculation" value="32148-B-6"/><Field label="Compagnie" value="Wafa Assurance"/><Field label="N° de police" value="POL-984217"/><Field label="Intermédiaire" value="" state="warning"/></section><section><h3>Adversaire</h3><Field label="Nom complet" value="Rachid El Gharbi"/><Field label="Véhicule" value="Peugeot 208"/><Field label="Immatriculation" value="78410-A-2"/><Field label="Compagnie" value="RMA Assurance"/><Field label="N° de police" value="RMA-450821"/><Field label="Intermédiaire" value="Agence Al Massira"/></section></div>;
  if(tab===2)return <div className="vehicle-tab"><div className="fields-grid">{[["Marque","Renault"],["Modèle","Clio V"],["N° de série","VF1RJA00X65482147"],["Énergie","Diesel"],["Kilométrage","84 000 km"],["Pneu AVG","40 %"],["Pneu AVD","45 %"],["Pneu ARG","60 %"],["Pneu ARD","60 %"]].map(x=><Field key={x[0]} label={x[0]} value={x[1]}/>)}</div><VehicleDiagram/></div>;
  if(tab===3)return <DamageTable/>;
  if(tab===4)return <div className="parts-table"><div className="group-label">ZONE : ARRIÈRE <span>7</span></div><div className="subgroup">Tôlerie</div><DataTable headers={["QTÉ","DÉSIGNATION","TYPE","PRIX","VÉTUSTÉ","TVA","TOTAL","OPÉRATION","HEURES","MONTANT"]}><tr><td>1</td><td><strong>PARE CHOC AR</strong></td><td>R</td><td>400,00</td><td>0%</td><td>20%</td><td>480,00</td><td>Réparation</td><td>4 h</td><td>240,00</td></tr><tr><td>1</td><td><strong>TRAVERSE ARRIÈRE</strong></td><td>O</td><td>—</td><td>—</td><td>20%</td><td>—</td><td>Redressage</td><td>8 h</td><td>480,00</td></tr></DataTable><div className="subgroup">Peinture</div><DataTable headers={["QTÉ","DÉSIGNATION","TYPE","PRIX","VÉTUSTÉ","TVA","TOTAL","OPÉRATION","HEURES","MONTANT"]}><tr><td>1</td><td><strong>PARE CHOC AR</strong></td><td>R</td><td>—</td><td>0%</td><td>20%</td><td>—</td><td>Peinture</td><td>5 h</td><td>300,00</td></tr></DataTable></div>;
  return <div className="conclusion"><div className="fields-grid"><Field label="Vétusté" value="0,00 DH"/><Field label="Dépannage" value="0,00 DH"/><Field label="Valeur à neuf" value="185 000,00 DH"/><Field label="Valeur vénale" value="142 000,00 DH"/><Field label="Valeur épave" value="—"/><Field label="TVA récupérable" value="Non"/></div><div className="indemnity"><span>Montant d’indemnisation</span><strong>2 400,00 DH</strong><p>Deux mille quatre cents dirhams et zéro centime</p></div></div>;
}

function PdfViewer(){return <section className="pdf-panel"><div className="pdf-toolbar"><span>Rapport_268202-0284.pdf</span><div><button>−</button><b>90%</b><button>+</button><i/><button>‹</button><b>1 / 2</b><button>›</button></div></div><div className="pdf-canvas"><div className="pdf-page"><img src="/logo-lt-expertise.png" alt="LT Expertise"/><h3>RAPPORT D’EXPERTISE AUTOMOBILE</h3><div className="pdf-ref">N° 268202-0284</div><div className="fake-lines">{Array.from({length:12}).map((_,i)=><i key={i} style={{width:`${55+(i*17)%42}%`}}/>)}</div><div className="pdf-highlight"><span>Montant de l’indemnisation</span><strong>2 400,00 DH</strong></div><div className="fake-table">{Array.from({length:20}).map((_,i)=><i key={i}/>)}</div></div></div><div className="pdf-footer"><Icon name="eye" size={14}/> Le champ sélectionné est surligné dans le document source</div></section>}

function Verify({go}:{go:(s:Screen)=>void}) {
  const [tab,setTab]=useState(0); const [checks,setChecks]=useState(true);
  return <main className="verify-page"><div className="verify-head"><div><button onClick={()=>go("folders")}>‹ Dossiers</button><div><h1>Vérification du dossier 268202-0284</h1><Status>À vérifier</Status></div><p>Importé le 20/10/2026 à 09:14 · rapport_268202-0284.pdf</p></div><div className="head-actions"><Button variant="secondary">Enregistrer</Button><Button disabled title="1 contrôle en échec">Valider le dossier</Button></div></div><div className="brand-rule"><i/><i/></div>
    <div className="verify-split"><PdfViewer/><section className="form-panel"><div className="form-tabs">{tabs.map((t,i)=><button className={tab===i?"active":""} key={t} onClick={()=>setTab(i)}>{t}{i===1&&<i/>}</button>)}</div><div className="form-content"><div className="tab-heading"><div><span>ÉTAPE {tab+1} SUR 6</span><h2>{tabs[tab]}</h2></div><span className="saved"><Icon name="check" size={13}/> Modifications enregistrées</span></div><TabContent tab={tab}/></div></section></div>
    <aside className={`checks-panel ${checks?"open":""}`}><button className="checks-head" onClick={()=>setChecks(!checks)}><span><Icon name="check" size={17}/><strong>Contrôles de cohérence</strong><b>26/27</b></span><span>{checks?"⌄":"⌃"}</span></button>{checks&&<div className="check-list">{[["ok","Somme MO = Total main d’œuvre"],["ok","Total général TTC − vétusté = indemnisation"],["ok","Montant en lettres = montant en chiffres"],["bad","Page 2 = page 1 (fournitures)"]].map(([s,t])=><div key={t} className={s}><i>{s==="ok"?"✓":"×"}</i><span>{t}{s==="bad"&&<small>Attendu 840,00, trouvé 800,00</small>}</span></div>)}</div>}</aside>
  </main>;
}

function Detail({go}:{go:(s:Screen)=>void}) {
  return <main className="main detail-page"><PageHead title="Dossier 268202-0284" subtitle="Rapport validé le 20 octobre 2026 à 09:31" actions={<><Status>Validé</Status><Button variant="secondary" icon="eye">Voir le PDF original</Button><Button variant="secondary" icon="file">Générer le rapport PDF</Button><Button variant="secondary" icon="download">Exporter en Excel</Button><Button onClick={()=>go("verify")}>Modifier</Button></>}/>
    <div className="detail-layout"><div className="detail-main"><div className="amount-banner"><span>Montant d’indemnisation validé</span><strong>2 400,00 DH</strong><small>TVA incluse · TVA non récupérable</small></div>
      <InfoCard title="Sinistre"><div className="info-grid">{[["N° de rapport","268202-0284"],["Réf. sinistre","SIN-2026-10842"],["Date du sinistre","18/10/2026"],["Compagnie","Wafa Assurance"],["Contrat","RC"],["Expert","M. Amine El Khatib"]].map(x=><Info key={x[0]} label={x[0]} value={x[1]}/>)}</div></InfoCard>
      <InfoCard title="Parties"><div className="party-summary"><section><h3>Assuré</h3><Info label="Nom" value="Hassan Mansouri"/><Info label="Véhicule" value="Renault Clio V · 32148-B-6"/><Info label="Police" value="POL-984217"/></section><section><h3>Adversaire</h3><Info label="Nom" value="Rachid El Gharbi"/><Info label="Véhicule" value="Peugeot 208 · 78410-A-2"/><Info label="Compagnie" value="RMA Assurance"/></section></div></InfoCard>
      <InfoCard title="Véhicule"><div className="vehicle-summary"><div className="info-grid"><Info label="Marque / Modèle" value="Renault Clio V"/><Info label="Énergie" value="Diesel"/><Info label="Kilométrage" value="84 000 km"/><Info label="N° de série" value="VF1RJA00X65482147"/></div><VehicleDiagram/></div></InfoCard>
      <InfoCard title="Évaluation des dommages"><DamageTable editable={false}/></InfoCard>
      <InfoCard title="Détail des pièces"><TabContent tab={4}/></InfoCard>
      <InfoCard title="Conclusion"><TabContent tab={5}/></InfoCard>
    </div><aside className="history card"><h2>Historique</h2><p>Traçabilité des actions</p><div className="timeline">{[["Importé","par S. Benali","20/10/2026 09:14"],["Kilométrage modifié","vide → 84 000","20/10/2026 09:20"],["Contrôle relancé","26/27 conformes","20/10/2026 09:25"],["Validé","par S. Benali","20/10/2026 09:31"],["Rapport PDF généré","par S. Benali","20/10/2026 09:33"]].map((e,i)=><div key={e[0]} className={i===3?"important":""}><i/><strong>{e[0]}</strong><span>{e[1]}</span><small>{e[2]}</small></div>)}</div></aside></div>
  </main>;
}
function InfoCard({title,children}:{title:string;children:ReactNode}){return <article className="card info-card"><div className="info-head"><h2>{title}</h2><button>Afficher les détails <span>⌄</span></button></div>{children}</article>}
function Info({label,value}:{label:string;value:string}){return <div className="info-item"><span>{label}</span><strong>{value}</strong></div>}

const priceRows = [
  { name:"Pare-chocs arrière", type:"Pièce", reference:"PC-CL5-AR", client:"Wafa Assurance", vehicle:"Renault Clio V", supplier:"Auto Parts Maroc", price:1680, date:"24/10/2026" },
  { name:"Pare-chocs arrière", type:"Pièce", reference:"PC-CL5-AR", client:"RMA Assurance", vehicle:"Renault Clio V", supplier:"Carrosserie Plus", price:1920, date:"19/10/2026" },
  { name:"Pare-chocs arrière", type:"Pièce", reference:"PC-208-AR", client:"Sanlam Maroc", vehicle:"Peugeot 208", supplier:"Atlas Pièces", price:2140, date:"16/10/2026" },
  { name:"Phare avant droit", type:"Pièce", reference:"PH-DAC-D", client:"AXA Assurance", vehicle:"Dacia Sandero", supplier:"Auto Parts Maroc", price:1260, date:"12/10/2026" },
  { name:"Aile avant gauche", type:"Pièce", reference:"AI-C3-G", client:"Wafa Assurance", vehicle:"Citroën C3", supplier:"Carrosserie Plus", price:980, date:"08/10/2026" },
  { name:"Peinture métallisée", type:"Fourniture", reference:"FOU-PEINT-M", client:"AtlantaSanad", vehicle:"Renault Clio V", supplier:"Color Auto", price:520, date:"25/10/2026" },
  { name:"Ingrédient peinture", type:"Fourniture", reference:"FOU-ING-01", client:"RMA Assurance", vehicle:"Peugeot 208", supplier:"Color Auto", price:310, date:"21/10/2026" },
  { name:"Kit de fixation", type:"Fourniture", reference:"FOU-FIX-12", client:"Sanlam Maroc", vehicle:"Dacia Sandero", supplier:"Atlas Pièces", price:145, date:"15/10/2026" },
];

function PriceAnalysis() {
  const [type,setType]=useState("Tous");
  const [client,setClient]=useState("Tous les clients");
  const [vehicle,setVehicle]=useState("Tous les véhicules");
  const [query,setQuery]=useState("");
  const filtered = priceRows.filter(row=>(type==="Tous"||row.type===type)&&(client==="Tous les clients"||row.client===client)&&(vehicle==="Tous les véhicules"||row.vehicle===vehicle)&&(row.name.toLowerCase().includes(query.toLowerCase())||row.reference.toLowerCase().includes(query.toLowerCase())));
  const prices = filtered.map(row=>row.price);
  const minimum = prices.length?Math.min(...prices):0;
  const maximum = prices.length?Math.max(...prices):0;
  const average = prices.length?Math.round(prices.reduce((sum,value)=>sum+value,0)/prices.length):0;
  const formatPrice = (value:number)=>`${value.toLocaleString("fr-FR")} DH`;
  const reset = ()=>{setType("Tous");setClient("Tous les clients");setVehicle("Tous les véhicules");setQuery("")};
  return <main className="main pricing-page">
    <PageHead title="Analyse des prix" subtitle="Comparez les tarifs des pièces et fournitures par client et par véhicule" actions={<Button variant="secondary" icon="download">Exporter l’analyse</Button>}/>
    <article className="card pricing-filters">
      <div className="pricing-search"><Icon name="search" size={17}/><input value={query} onChange={e=>setQuery(e.target.value)} placeholder="Rechercher une pièce, une fourniture ou une référence…"/></div>
      <label><span>Catégorie</span><select value={type} onChange={e=>setType(e.target.value)}><option>Tous</option><option>Pièce</option><option>Fourniture</option></select></label>
      <label><span>Client / compagnie</span><select value={client} onChange={e=>setClient(e.target.value)}><option>Tous les clients</option>{[...new Set(priceRows.map(row=>row.client))].map(value=><option key={value}>{value}</option>)}</select></label>
      <label><span>Marque / modèle</span><select value={vehicle} onChange={e=>setVehicle(e.target.value)}><option>Tous les véhicules</option>{[...new Set(priceRows.map(row=>row.vehicle))].map(value=><option key={value}>{value}</option>)}</select></label>
      <Button variant="quiet" onClick={reset}>Réinitialiser</Button>
    </article>
    <section className="price-kpis">
      <article className="card price-kpi minimum"><span><Icon name="arrow" size={18}/></span><div><small>Prix minimum</small><strong>{formatPrice(minimum)}</strong><p>Tarif le plus bas observé</p></div></article>
      <article className="card price-kpi average"><span><Icon name="chart" size={18}/></span><div><small>Prix moyen</small><strong>{formatPrice(average)}</strong><p>Moyenne des résultats filtrés</p></div></article>
      <article className="card price-kpi maximum"><span><Icon name="arrow" size={18}/></span><div><small>Prix maximum</small><strong>{formatPrice(maximum)}</strong><p>Tarif le plus élevé observé</p></div></article>
      <article className="card price-kpi records"><span><Icon name="file" size={18}/></span><div><small>Tarifs analysés</small><strong>{filtered.length}</strong><p>Occurrences correspondantes</p></div></article>
    </section>
    <section className="pricing-insights">
      <article className="card price-range-card">
        <div className="section-head"><div><h2>Fourchette tarifaire</h2><p>Positionnement des tarifs sélectionnés</p></div><span className="price-spread">Écart : {formatPrice(maximum-minimum)}</span></div>
        <div className="range-visual"><div className="range-labels"><span>Minimum<b>{formatPrice(minimum)}</b></span><span>Moyenne<b>{formatPrice(average)}</b></span><span>Maximum<b>{formatPrice(maximum)}</b></span></div><div className="range-track"><i/><i/><i/></div></div>
        <div className="price-advice"><Icon name="check" size={16}/><div><strong>Indicateur de référence</strong><p>Un tarif proche de la moyenne facilite une estimation cohérente. Vérifiez les montants éloignés de plus de 20 %.</p></div></div>
      </article>
      <article className="card distribution-card"><div className="section-head"><div><h2>Répartition par catégorie</h2><p>Montant moyen par type</p></div></div><div className="distribution-bars"><div><span>Pièces</span><i><b className="parts-bar"/></i><strong>1 596 DH</strong></div><div><span>Fournitures</span><i><b className="supplies-bar"/></i><strong>325 DH</strong></div></div></article>
    </section>
    <article className="card pricing-results">
      <div className="section-head"><div><h2>Détail des tarifs</h2><p>{filtered.length} résultat{filtered.length!==1?"s":""} correspondant aux critères</p></div><span className="results-updated"><Icon name="check" size={13}/> Données actualisées</span></div>
      <DataTable headers={["DÉSIGNATION","TYPE","RÉFÉRENCE","CLIENT / COMPAGNIE","VÉHICULE","FOURNISSEUR","PRIX TTC","DATE"]}>{filtered.map((row,index)=><tr key={`${row.reference}-${index}`}><td><strong>{row.name}</strong></td><td><span className={`price-type ${row.type==="Pièce"?"part":"supply"}`}>{row.type}</span></td><td>{row.reference}</td><td>{row.client}</td><td>{row.vehicle}</td><td>{row.supplier}</td><td className="money"><strong>{formatPrice(row.price)}</strong></td><td>{row.date}</td></tr>)}</DataTable>
      {!filtered.length&&<div className="empty-prices"><Icon name="search" size={24}/><strong>Aucun tarif trouvé</strong><span>Modifiez ou réinitialisez vos critères de recherche.</span></div>}
    </article>
  </main>;
}

const generatedReports = [
  ["RAP-2026-0284", "Rapport d’expertise", "268202-0284", "Hassan Mansouri", "28/10/2026 à 10:42", "PDF"],
  ["SYN-2026-0312", "Synthèse d’indemnisation", "268202-0312", "Salma Idrissi", "27/10/2026 à 16:18", "PDF"],
  ["RAP-2026-0309", "Rapport d’expertise", "268202-0309", "Mehdi El Fassi", "26/10/2026 à 11:07", "PDF"],
];

function ReportGeneration({go}:{go:(s:Screen)=>void}) {
  const [template,setTemplate]=useState("Rapport d’expertise complet");
  const [reference,setReference]=useState("268202-0284");
  const [generated,setGenerated]=useState(false);
  const sections = ["Informations du sinistre","Parties et véhicules","Évaluation des dommages","Détail des pièces","Conclusion et indemnisation","Historique des contrôles"];
  return <main className="main generation-page">
    <PageHead title="Génération des rapports" subtitle="Créez des documents normalisés à partir des dossiers validés" actions={<Button variant="secondary" icon="folder" onClick={()=>go("folders")}>Consulter les dossiers</Button>}/>
    <div className="generation-layout">
      <div className="generation-settings">
        <article className="card generation-card">
          <div className="generation-card-head"><span>1</span><div><h2>Dossier source</h2><p>Sélectionnez le dossier validé à utiliser</p></div><b>OBLIGATOIRE</b></div>
          <label className="generation-field"><span>N° de dossier ou assuré</span><div><Icon name="search" size={16}/><input value={reference} onChange={e=>{setReference(e.target.value);setGenerated(false)}} placeholder="Rechercher un dossier validé…"/></div></label>
          <div className="selected-folder"><span><Icon name="check" size={14}/></span><div><strong>Dossier {reference || "268202-0284"}</strong><small>Hassan Mansouri · Renault Clio V · 32148-B-6</small></div><Status>Validé</Status></div>
        </article>
        <article className="card generation-card">
          <div className="generation-card-head"><span>2</span><div><h2>Modèle et contenu</h2><p>Définissez la structure du document</p></div></div>
          <label className="generation-field"><span>Modèle de rapport</span><select value={template} onChange={e=>{setTemplate(e.target.value);setGenerated(false)}}><option>Rapport d’expertise complet</option><option>Synthèse d’indemnisation</option><option>Rapport contradictoire</option></select></label>
          <div className="report-sections"><span>Sections à inclure</span>{sections.map((section,i)=><label key={section}><input type="checkbox" defaultChecked={i<5}/><span><strong>{section}</strong><small>{i===5?"Journal des modifications et validations":"Données extraites et validées du dossier"}</small></span></label>)}</div>
        </article>
        <article className="card generation-card">
          <div className="generation-card-head"><span>3</span><div><h2>Paramètres de sortie</h2><p>Finalisez les options d’export</p></div></div>
          <div className="output-grid"><label className="generation-field"><span>Format</span><select><option>PDF sécurisé</option><option>Document Word (.docx)</option></select></label><label className="generation-field"><span>Langue</span><select><option>Français</option><option>Arabe</option></select></label></div>
          <div className="output-options"><label><input type="checkbox" defaultChecked/>Ajouter le logo et l’en-tête officiel</label><label><input type="checkbox" defaultChecked/>Inclure la signature de l’expert</label><label><input type="checkbox" defaultChecked/>Numéroter toutes les pages</label></div>
        </article>
        <div className={`generation-feedback ${generated?"visible":""}`}><Icon name="check" size={16}/><div><strong>Rapport généré avec succès</strong><span>RAP-2026-0284.pdf est prêt au téléchargement.</span></div><Button variant="quiet" icon="download">Télécharger</Button></div>
        <div className="generation-actions"><Button variant="secondary">Enregistrer comme brouillon</Button><Button icon="file" onClick={()=>setGenerated(true)}>Générer le rapport</Button></div>
      </div>
      <aside className="report-preview card">
        <div className="preview-toolbar"><div><strong>Aperçu du rapport</strong><span>Mis à jour automatiquement</span></div><button aria-label="Agrandir l’aperçu"><Icon name="eye" size={16}/></button></div>
        <div className="preview-canvas"><div className="report-paper">
          <header><img src="/logo-lt-expertise.png" alt="LT Expertise"/><div><span>Cabinet d’expertise automobile</span><small>Rapport confidentiel</small></div></header>
          <div className="report-paper-title"><small>RÉFÉRENCE {reference || "268202-0284"}</small><h2>{template}</h2><p>Établi le 28 octobre 2026</p></div>
          <section><h3>Informations générales</h3><div className="paper-grid"><span>Assuré<strong>Hassan Mansouri</strong></span><span>Compagnie<strong>Wafa Assurance</strong></span><span>Véhicule<strong>Renault Clio V</strong></span><span>Immatriculation<strong>32148-B-6</strong></span></div></section>
          <section><h3>Évaluation des dommages</h3><div className="paper-table"><span>Désignation</span><span>Montant HT</span><span>Tôlerie et main d’œuvre</span><b>1 440,00 DH</b><span>Pièces et fournitures</span><b>560,00 DH</b></div></section>
          <div className="paper-total"><span>Montant d’indemnisation</span><strong>2 400,00 DH</strong><small>Deux mille quatre cents dirhams</small></div>
          <footer><span>LT Expertise · Document généré automatiquement</span><b>1 / 3</b></footer>
        </div></div>
      </aside>
    </div>
    <article className="card generated-list"><div className="section-head"><div><h2>Rapports récemment générés</h2><p>Historique des derniers documents créés</p></div><Button variant="quiet">Voir tout l’historique <Icon name="arrow" size={14}/></Button></div><DataTable headers={["RÉFÉRENCE","TYPE DE RAPPORT","DOSSIER","ASSURÉ","DATE DE GÉNÉRATION","FORMAT","ACTIONS"]}>{generatedReports.map(report=><tr key={report[0]}><td><strong>{report[0]}</strong></td><td>{report[1]}</td><td>{report[2]}</td><td>{report[3]}</td><td>{report[4]}</td><td><span className="format-badge"><Icon name="file" size={12}/>{report[5]}</span></td><td><div className="report-actions"><button onClick={()=>go("detail")}>Consulter</button><button><Icon name="download" size={15}/></button><button><Icon name="more" size={16}/></button></div></td></tr>)}</DataTable></article>
  </main>;
}

const users=[["Samira Benali","s.benali","Gestionnaire","Actif","Aujourd’hui à 10:42"],["Amine El Khatib","a.elkhatib","Administrateur","Actif","Aujourd’hui à 09:18"],["Nadia Alaoui","n.alaoui","Consultation","Actif","Hier à 16:32"],["Youssef Amrani","y.amrani","Gestionnaire","Verrouillé","24/10/2026 à 11:05"],["Salma Idrissi","s.idrissi","Consultation","Actif","21/10/2026 à 14:12"]];
const userAuditEvents=[
  { date:"28/10/2026", time:"10:42", user:"Samira Benali", initials:"SB", action:"Connexion réussie", detail:"Accès à la plateforme", category:"Connexion", ip:"196.200.176.42" },
  { date:"28/10/2026", time:"09:36", user:"Amine El Khatib", initials:"AE", action:"Rôle modifié", detail:"Nadia Alaoui · Consultation → Gestionnaire", category:"Modification", ip:"196.200.176.18" },
  { date:"28/10/2026", time:"09:18", user:"Amine El Khatib", initials:"AE", action:"Utilisateur créé", detail:"Création du compte de Salma Idrissi", category:"Création", ip:"196.200.176.18" },
  { date:"27/10/2026", time:"16:32", user:"Nadia Alaoui", initials:"NA", action:"Export de données", detail:"Export de la liste des dossiers au format Excel", category:"Export", ip:"105.158.94.61" },
  { date:"27/10/2026", time:"14:07", user:"Samira Benali", initials:"SB", action:"Mot de passe réinitialisé", detail:"Réinitialisation demandée pour Youssef Amrani", category:"Sécurité", ip:"196.200.176.42" },
  { date:"24/10/2026", time:"11:05", user:"Youssef Amrani", initials:"YA", action:"Compte verrouillé", detail:"5 tentatives de connexion échouées", category:"Sécurité", ip:"41.248.132.17" },
];
function Users(){
  const [drawer,setDrawer]=useState(false);
  const [view,setView]=useState<"accounts"|"audit">("accounts");
  const [auditQuery,setAuditQuery]=useState("");
  const visibleEvents=userAuditEvents.filter(event=>`${event.user} ${event.action} ${event.detail} ${event.category} ${event.ip}`.toLowerCase().includes(auditQuery.toLowerCase()));
  return <main className="main users-page"><PageHead title="Utilisateurs" subtitle="Administration des accès, des rôles et de la traçabilité" actions={view==="accounts"?<Button icon="plus" onClick={()=>setDrawer(true)}>Ajouter un utilisateur</Button>:<Button variant="secondary" icon="download">Exporter le journal</Button>}/>
    <div className="users-tabs" role="tablist" aria-label="Administration des utilisateurs"><button className={view==="accounts"?"active":""} onClick={()=>setView("accounts")} role="tab" aria-selected={view==="accounts"}>Comptes utilisateurs <b>5</b></button><button className={view==="audit"?"active":""} onClick={()=>setView("audit")} role="tab" aria-selected={view==="audit"}>Journal d’audit <b>{userAuditEvents.length}</b></button></div>
    {view==="accounts"?<article className="card users-card"><div className="section-head"><div><h2>Comptes utilisateurs</h2><p>5 utilisateurs · 4 actifs · 1 verrouillé</p></div><div className="small-search"><Icon name="search" size={15}/><input placeholder="Rechercher un utilisateur…"/></div></div><DataTable headers={["NOM","IDENTIFIANT","RÔLE","STATUT","DERNIÈRE CONNEXION","ACTIONS"]}>{users.map(u=><tr key={u[1]}><td><div className="user-cell"><span>{u[0].split(" ").map(x=>x[0]).join("")}</span><strong>{u[0]}</strong></div></td><td>{u[1]}</td><td><span className={`role ${u[2].toLowerCase()}`}>{u[2]}</span></td><td><span className={`account-status ${u[3]==="Actif"?"active":"locked"}`}><i/>{u[3]}</span></td><td>{u[4]}</td><td><div className="user-actions"><button>Modifier</button><button>Réinitialiser le mot de passe</button><button>Désactiver</button></div></td></tr>)}</DataTable></article>
    :<><article className="card audit-filters"><div className="audit-search"><Icon name="search" size={16}/><input value={auditQuery} onChange={event=>setAuditQuery(event.target.value)} placeholder="Rechercher une action, un utilisateur ou une adresse IP…"/></div><Filter label="Période" value="7 derniers jours"/><Filter label="Utilisateur" value="Tous les utilisateurs"/><Filter label="Type d’action" value="Toutes les actions"/><Button variant="quiet" onClick={()=>setAuditQuery("")}>Réinitialiser</Button></article>
      <article className="card audit-card"><div className="section-head"><div><h2>Activité des utilisateurs</h2><p>Traçabilité des connexions et des actions d’administration</p></div><span className="audit-retention"><Icon name="lock" size={13}/>Conservation : 12 mois</span></div><DataTable headers={["DATE ET HEURE","UTILISATEUR","ACTION","DÉTAIL","CATÉGORIE","ADRESSE IP"]}>{visibleEvents.map(event=><tr key={`${event.date}-${event.time}-${event.action}`}><td><div className="audit-date"><strong>{event.date}</strong><span>{event.time}</span></div></td><td><div className="user-cell"><span>{event.initials}</span><strong>{event.user}</strong></div></td><td><strong className="audit-action">{event.action}</strong></td><td>{event.detail}</td><td><span className={`audit-category ${event.category.toLowerCase().replace("é","e")}`}>{event.category}</span></td><td><code className="audit-ip">{event.ip}</code></td></tr>)}</DataTable>{visibleEvents.length===0&&<div className="audit-empty"><Icon name="search" size={20}/><strong>Aucun événement trouvé</strong><span>Modifiez votre recherche pour afficher d’autres activités.</span></div>}<div className="pagination"><span>1–{visibleEvents.length} sur {visibleEvents.length} événements</span><div><button disabled>‹</button><button className="active">1</button><button disabled>›</button></div></div></article></>}
    {drawer&&<><button className="drawer-overlay" onClick={()=>setDrawer(false)} aria-label="Fermer"/><aside className="user-drawer"><div className="drawer-head"><div><h2>Ajouter un utilisateur</h2><p>Créez un nouvel accès à la plateforme</p></div><button onClick={()=>setDrawer(false)}>×</button></div><div className="drawer-body"><Field label="Nom complet" value=""/><Field label="Identifiant" value=""/><Field label="Adresse e-mail" value=""/><h3>Rôle et droits</h3>{[["Administrateur","Accès complet, utilisateurs et paramétrage"],["Gestionnaire","Import, vérification, modification et validation"],["Consultation","Consultation et export uniquement"]].map((r,i)=><label className={`role-choice ${i===1?"selected":""}`} key={r[0]}><input type="radio" name="role" defaultChecked={i===1}/><div><strong>{r[0]}</strong><span>{r[1]}</span></div></label>)}</div><div className="drawer-actions"><Button variant="secondary" onClick={()=>setDrawer(false)}>Annuler</Button><Button>Créer l’utilisateur</Button></div></aside></>}
  </main>
}

function Login({go}:{go:(s:Screen)=>void}) {
  return <div className="login-page"><div className="login-decoration left"/><div className="login-decoration right"/><main className="login-card"><img src="/logo-lt-expertise.png" alt="LT Expertise"/><h1>Bienvenue</h1><p>Plateforme de gestion des rapports d’expertise</p><div className="login-error"><Icon name="warning" size={17}/><span><strong>Identifiant ou mot de passe incorrect</strong>Compte verrouillé après 5 tentatives.</span></div><label><span>Identifiant</span><div><Icon name="users" size={17}/><input placeholder="Votre identifiant"/></div></label><label><span>Mot de passe</span><div><Icon name="lock" size={17}/><input type="password" value="motdepasse" readOnly/><Icon name="eye" size={17}/></div></label><div className="login-meta"><label><input type="checkbox"/> Se souvenir de moi</label><button>Mot de passe oublié ?</button></div><Button onClick={()=>go("dashboard")}>Se connecter <Icon name="arrow" size={15}/></Button><small>Accès sécurisé réservé aux collaborateurs de LT Expertise</small></main><footer>© 2026 LT Expertise · Confidentialité · Assistance</footer></div>
}

export default function App(){
  const [screen,setScreen]=useState<Screen>("dashboard");
  if(screen==="login") return <Login go={setScreen}/>;
  let content:ReactNode;
  if(screen==="dashboard") content=<Dashboard go={setScreen}/>;
  else if(screen==="import") content=<ImportScreen go={setScreen}/>;
  else if(screen==="folders") content=<Folders go={setScreen}/>;
  else if(screen==="pricing") content=<PriceAnalysis/>;
  else if(screen==="generate") content=<ReportGeneration go={setScreen}/>;
  else if(screen==="verify") content=<Verify go={setScreen}/>;
  else if(screen==="detail") content=<Detail go={setScreen}/>;
  else content=<Users/>;
  return <Layout screen={screen} go={setScreen}>{content}</Layout>;
}
