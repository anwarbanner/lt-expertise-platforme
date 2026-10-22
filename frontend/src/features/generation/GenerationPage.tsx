import { useState } from "react";
import { useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { DOSSIER_DEMO, rapportsGeneres } from "@/mocks/donnees";

/**
 * Génération de rapports (F9). Le cahier des charges prévoit un PDF fidèle au rapport d'origine (P10) généré
 * depuis un dossier validé ; les options Word, arabe et choix des sections vont au-delà (voir la roadmap).
 */
export default function GenerationPage() {
  const navigate = useNavigate();
  const [template, setTemplate] = useState("Rapport d’expertise complet");
  const [reference, setReference] = useState(DOSSIER_DEMO);
  const [generated, setGenerated] = useState(false);
  const sections = ["Informations du sinistre", "Parties et véhicules", "Évaluation des dommages", "Détail des pièces", "Conclusion et indemnisation", "Historique des contrôles"];
  return <main className="main generation-page">
    <PageHead title="Génération des rapports" subtitle="Créez des documents normalisés à partir des dossiers validés" actions={<Button variant="secondary" icon="folder" onClick={() => navigate(ROUTES.dossiers)}>Consulter les dossiers</Button>} />
    <div className="generation-layout">
      <div className="generation-settings">
        <article className="card generation-card">
          <div className="generation-card-head"><span>1</span><div><h2>Dossier source</h2><p>Sélectionnez le dossier validé à utiliser</p></div><b>OBLIGATOIRE</b></div>
          <label className="generation-field"><span>N° de dossier ou assuré</span><div><Icon name="search" size={16} /><input value={reference} onChange={e => { setReference(e.target.value); setGenerated(false); }} placeholder="Rechercher un dossier validé…" /></div></label>
          <div className="selected-folder"><span><Icon name="check" size={14} /></span><div><strong>Dossier {reference || DOSSIER_DEMO}</strong><small>Hassan Mansouri · Renault Clio V · 32148-B-6</small></div><StatusBadge statut="VALIDE" /></div>
        </article>
        <article className="card generation-card">
          <div className="generation-card-head"><span>2</span><div><h2>Modèle et contenu</h2><p>Définissez la structure du document</p></div></div>
          <label className="generation-field"><span>Modèle de rapport</span><select value={template} onChange={e => { setTemplate(e.target.value); setGenerated(false); }}><option>Rapport d’expertise complet</option><option>Synthèse d’indemnisation</option><option>Rapport contradictoire</option></select></label>
          <div className="report-sections"><span>Sections à inclure</span>{sections.map((section, i) => <label key={section}><input type="checkbox" defaultChecked={i < 5} /><span><strong>{section}</strong><small>{i === 5 ? "Journal des modifications et validations" : "Données extraites et validées du dossier"}</small></span></label>)}</div>
        </article>
        <article className="card generation-card">
          <div className="generation-card-head"><span>3</span><div><h2>Paramètres de sortie</h2><p>Finalisez les options d’export</p></div></div>
          <div className="output-grid"><label className="generation-field"><span>Format</span><select><option>PDF sécurisé</option><option>Document Word (.docx)</option></select></label><label className="generation-field"><span>Langue</span><select><option>Français</option><option>Arabe</option></select></label></div>
          <div className="output-options"><label><input type="checkbox" defaultChecked />Ajouter le logo et l’en-tête officiel</label><label><input type="checkbox" defaultChecked />Inclure la signature de l’expert</label><label><input type="checkbox" defaultChecked />Numéroter toutes les pages</label></div>
        </article>
        <div className={`generation-feedback ${generated ? "visible" : ""}`}><Icon name="check" size={16} /><div><strong>Rapport généré avec succès</strong><span>RAP-2026-0284.pdf est prêt au téléchargement.</span></div><Button variant="quiet" icon="download">Télécharger</Button></div>
        <div className="generation-actions"><Button variant="secondary">Enregistrer comme brouillon</Button><Button icon="file" onClick={() => setGenerated(true)}>Générer le rapport</Button></div>
      </div>
      <aside className="report-preview card">
        <div className="preview-toolbar"><div><strong>Aperçu du rapport</strong><span>Mis à jour automatiquement</span></div><button aria-label="Agrandir l’aperçu"><Icon name="eye" size={16} /></button></div>
        <div className="preview-canvas"><div className="report-paper">
          <header><img src="/logo-lt-expertise.png" alt="LT Expertise" /><div><span>Cabinet d’expertise automobile</span><small>Rapport confidentiel</small></div></header>
          <div className="report-paper-title"><small>RÉFÉRENCE {reference || DOSSIER_DEMO}</small><h2>{template}</h2><p>Établi le 28 octobre 2026</p></div>
          <section><h3>Informations générales</h3><div className="paper-grid"><span>Assuré<strong>Hassan Mansouri</strong></span><span>Compagnie<strong>Wafa Assurance</strong></span><span>Véhicule<strong>Renault Clio V</strong></span><span>Immatriculation<strong>32148-B-6</strong></span></div></section>
          <section><h3>Évaluation des dommages</h3><div className="paper-table"><span>Désignation</span><span>Montant HT</span><span>Tôlerie et main d’œuvre</span><b>1 440,00 DH</b><span>Pièces et fournitures</span><b>560,00 DH</b></div></section>
          <div className="paper-total"><span>Montant d’indemnisation</span><strong>2 400,00 DH</strong><small>Deux mille quatre cents dirhams</small></div>
          <footer><span>LT Expertise · Document généré automatiquement</span><b>1 / 3</b></footer>
        </div></div>
      </aside>
    </div>
    <article className="card generated-list"><div className="section-head"><div><h2>Rapports récemment générés</h2><p>Historique des derniers documents créés</p></div><Button variant="quiet">Voir tout l’historique <Icon name="arrow" size={14} /></Button></div><DataTable headers={["RÉFÉRENCE", "TYPE DE RAPPORT", "DOSSIER", "ASSURÉ", "DATE DE GÉNÉRATION", "FORMAT", "ACTIONS"]}>{rapportsGeneres.map(report => <tr key={report[0]}><td><strong>{report[0]}</strong></td><td>{report[1]}</td><td>{report[2]}</td><td>{report[3]}</td><td>{report[4]}</td><td><span className="format-badge"><Icon name="file" size={12} />{report[5]}</span></td><td><div className="report-actions"><button onClick={() => navigate(ROUTES.dossier(report[2]))}>Consulter</button><button><Icon name="download" size={15} /></button><button><Icon name="more" size={16} /></button></div></td></tr>)}</DataTable></article>
  </main>;
}
