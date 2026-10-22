import { useState } from "react";
import { useNavigate, useParams } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { DOSSIER_DEMO } from "@/mocks/donnees";
import { ONGLETS, OngletDossier } from "./components/OngletsDossier";
import { PdfViewer } from "./components/PdfViewer";

/** Écran de vérification (F5) : PDF original à gauche, formulaire à droite, contrôles de cohérence (P4). */
export default function VerificationPage() {
  const navigate = useNavigate();
  const numero = useParams().numero ?? DOSSIER_DEMO;
  const [onglet, setOnglet] = useState(0);
  const [controlesOuverts, setControlesOuverts] = useState(true);
  return <main className="verify-page"><div className="verify-head"><div><button onClick={() => navigate(ROUTES.dossiers)}>‹ Dossiers</button><div><h1>Vérification du dossier {numero}</h1><StatusBadge statut="A_VERIFIER" /></div><p>Importé le 20/10/2026 à 09:14 · rapport_{numero}.pdf</p></div><div className="head-actions"><Button variant="secondary">Enregistrer</Button><Button disabled title="1 contrôle en échec">Valider le dossier</Button></div></div><div className="brand-rule"><i /><i /></div>
    <div className="verify-split"><PdfViewer numero={numero} /><section className="form-panel"><div className="form-tabs">{ONGLETS.map((t, i) => <button className={onglet === i ? "active" : ""} key={t} onClick={() => setOnglet(i)}>{t}{i === 1 && <i />}</button>)}</div><div className="form-content"><div className="tab-heading"><div><span>ÉTAPE {onglet + 1} SUR {ONGLETS.length}</span><h2>{ONGLETS[onglet]}</h2></div><span className="saved"><Icon name="check" size={13} /> Modifications enregistrées</span></div><OngletDossier onglet={onglet} /></div></section></div>
    <aside className={`checks-panel ${controlesOuverts ? "open" : ""}`}><button className="checks-head" onClick={() => setControlesOuverts(!controlesOuverts)}><span><Icon name="check" size={17} /><strong>Contrôles de cohérence</strong><b>26/27</b></span><span>{controlesOuverts ? "⌄" : "⌃"}</span></button>{controlesOuverts && <div className="check-list">{[["ok", "Somme MO = Total main d’œuvre"], ["ok", "Total général TTC − vétusté = indemnisation"], ["ok", "Montant en lettres = montant en chiffres"], ["bad", "Page 2 = page 1 (fournitures)"]].map(([s, t]) => <div key={t} className={s}><i>{s === "ok" ? "✓" : "×"}</i><span>{t}{s === "bad" && <small>Attendu 840,00, trouvé 800,00</small>}</span></div>)}</div>}</aside>
  </main>;
}
