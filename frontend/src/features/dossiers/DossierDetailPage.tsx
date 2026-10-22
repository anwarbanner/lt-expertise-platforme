import { useNavigate, useParams } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { Info, InfoCard } from "@/components/ui/InfoCard";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { DOSSIER_DEMO } from "@/mocks/donnees";
import { DamageTable } from "./components/DamageTable";
import { ONGLET_CONCLUSION, ONGLET_DETAIL_PIECES, OngletDossier } from "./components/OngletsDossier";
import { VehicleDiagram } from "./components/VehicleDiagram";

/** Fiche d'un dossier validé (F8) avec son historique (P7). */
export default function DossierDetailPage() {
  const navigate = useNavigate();
  const numero = useParams().numero ?? DOSSIER_DEMO;
  return <main className="main detail-page"><PageHead title={`Dossier ${numero}`} subtitle="Rapport validé le 20 octobre 2026 à 09:31" actions={<><StatusBadge statut="VALIDE" /><Button variant="secondary" icon="eye">Voir le PDF original</Button><Button variant="secondary" icon="file">Générer le rapport PDF</Button><Button variant="secondary" icon="download">Exporter en Excel</Button><Button onClick={() => navigate(ROUTES.verification(numero))}>Modifier</Button></>} />
    <div className="detail-layout"><div className="detail-main"><div className="amount-banner"><span>Montant d’indemnisation validé</span><strong>2 400,00 DH</strong><small>TVA incluse · TVA non récupérable</small></div>
      <InfoCard title="Sinistre"><div className="info-grid">{[["N° de rapport", numero], ["Réf. sinistre", "SIN-2026-10842"], ["Date du sinistre", "18/10/2026"], ["Compagnie", "Wafa Assurance"], ["Contrat", "RC"], ["Expert", "M. Amine El Khatib"]].map(x => <Info key={x[0]} label={x[0]} value={x[1]} />)}</div></InfoCard>
      <InfoCard title="Parties"><div className="party-summary"><section><h3>Assuré</h3><Info label="Nom" value="Hassan Mansouri" /><Info label="Véhicule" value="Renault Clio V · 32148-B-6" /><Info label="Police" value="POL-984217" /></section><section><h3>Adversaire</h3><Info label="Nom" value="Rachid El Gharbi" /><Info label="Véhicule" value="Peugeot 208 · 78410-A-2" /><Info label="Compagnie" value="RMA Assurance" /></section></div></InfoCard>
      <InfoCard title="Véhicule"><div className="vehicle-summary"><div className="info-grid"><Info label="Marque / Modèle" value="Renault Clio V" /><Info label="Énergie" value="Diesel" /><Info label="Kilométrage" value="84 000 km" /><Info label="N° de série" value="VF1RJA00X65482147" /></div><VehicleDiagram /></div></InfoCard>
      <InfoCard title="Évaluation des dommages"><DamageTable editable={false} /></InfoCard>
      <InfoCard title="Détail des pièces"><OngletDossier onglet={ONGLET_DETAIL_PIECES} /></InfoCard>
      <InfoCard title="Conclusion"><OngletDossier onglet={ONGLET_CONCLUSION} /></InfoCard>
    </div><aside className="history card"><h2>Historique</h2><p>Traçabilité des actions</p><div className="timeline">{[["Importé", "par S. Benali", "20/10/2026 09:14"], ["Kilométrage modifié", "vide → 84 000", "20/10/2026 09:20"], ["Contrôle relancé", "26/27 conformes", "20/10/2026 09:25"], ["Validé", "par S. Benali", "20/10/2026 09:31"], ["Rapport PDF généré", "par S. Benali", "20/10/2026 09:33"]].map((e, i) => <div key={e[0]} className={i === 3 ? "important" : ""}><i /><strong>{e[0]}</strong><span>{e[1]}</span><small>{e[2]}</small></div>)}</div></aside></div>
  </main>;
}
