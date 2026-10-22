import { DataTable } from "@/components/ui/DataTable";
import { Field } from "@/components/ui/Field";
import { DamageTable } from "./DamageTable";
import { VehicleDiagram } from "./VehicleDiagram";

export const ONGLETS = ["Sinistre", "Parties", "Véhicule", "Évaluation des dommages", "Détail des pièces", "Conclusion"] as const;

export const ONGLET_DETAIL_PIECES = 4;
export const ONGLET_CONCLUSION = 5;

/** Contenu d'un onglet du dossier ; chaque onglet correspond à un bloc du rapport et à une table du modèle. */
export function OngletDossier({ onglet }: { onglet: number }) {
  // Bloc en-tête → rapport_expertise
  if (onglet === 0) return <div className="fields-grid">{[["N° de rapport", "268202-0284"], ["Réf. sinistre", "SIN-2026-10842"], ["Date du sinistre", "18/10/2026"], ["Date de mission", "19/10/2026"], ["Compagnie", "Wafa Assurance"], ["Nature du contrat", "RC"], ["Procédure", "Procédure classique"], ["Cabinet", "LT Expertise"], ["Expert", "M. Amine El Khatib"]].map(x => <Field key={x[0]} label={x[0]} value={x[1]} />)}</div>;
  // Assuré / adversaire → partie
  if (onglet === 1) return <div className="party-grid"><section><h3>Assuré</h3><Field label="Nom complet" value="Hassan Mansouri" /><Field label="Véhicule" value="Renault Clio V" /><Field label="Immatriculation" value="32148-B-6" /><Field label="Compagnie" value="Wafa Assurance" /><Field label="N° de police" value="POL-984217" /><Field label="Intermédiaire" value="" state="warning" /></section><section><h3>Adversaire</h3><Field label="Nom complet" value="Rachid El Gharbi" /><Field label="Véhicule" value="Peugeot 208" /><Field label="Immatriculation" value="78410-A-2" /><Field label="Compagnie" value="RMA Assurance" /><Field label="N° de police" value="RMA-450821" /><Field label="Intermédiaire" value="Agence Al Massira" /></section></div>;
  // Caractéristiques techniques → vehicule
  if (onglet === 2) return <div className="vehicle-tab"><div className="fields-grid">{[["Marque", "Renault"], ["Modèle", "Clio V"], ["N° de série", "VF1RJA00X65482147"], ["Énergie", "Diesel"], ["Kilométrage", "84 000 km"], ["Pneu AVG", "40 %"], ["Pneu AVD", "45 %"], ["Pneu ARG", "60 %"], ["Pneu ARD", "60 %"]].map(x => <Field key={x[0]} label={x[0]} value={x[1]} />)}</div><VehicleDiagram /></div>;
  // Évaluation des dommages → evaluation_main_oeuvre + fourniture
  if (onglet === 3) return <DamageTable />;
  // Détail fournitures / travaux → ligne_detail (opérations possibles : Échange, Réparation)
  if (onglet === ONGLET_DETAIL_PIECES) return <div className="parts-table"><div className="group-label">ZONE : ARRIÈRE <span>7</span></div><div className="subgroup">Tôlerie</div><DataTable headers={["QTÉ", "DÉSIGNATION", "TYPE", "PRIX", "VÉTUSTÉ", "TVA", "TOTAL", "OPÉRATION", "HEURES", "MONTANT"]}><tr><td>1</td><td><strong>PARE CHOC AR</strong></td><td>R</td><td>400,00</td><td>0%</td><td>20%</td><td>480,00</td><td>Réparation</td><td>4 h</td><td>240,00</td></tr><tr><td>1</td><td><strong>TRAVERSE ARRIÈRE</strong></td><td>O</td><td>—</td><td>—</td><td>20%</td><td>—</td><td>Échange</td><td>8 h</td><td>480,00</td></tr></DataTable><div className="subgroup">Peinture</div><DataTable headers={["QTÉ", "DÉSIGNATION", "TYPE", "PRIX", "VÉTUSTÉ", "TVA", "TOTAL", "OPÉRATION", "HEURES", "MONTANT"]}><tr><td>1</td><td><strong>PARE CHOC AR</strong></td><td>R</td><td>—</td><td>0%</td><td>20%</td><td>—</td><td>Réparation</td><td>5 h</td><td>300,00</td></tr></DataTable></div>;
  // Conclusion → rapport_expertise (vétusté, valeurs, TVA, indemnisation)
  return <div className="conclusion"><div className="fields-grid"><Field label="Vétusté" value="0,00 DH" /><Field label="Dépannage" value="0,00 DH" /><Field label="Valeur à neuf" value="185 000,00 DH" /><Field label="Valeur vénale" value="142 000,00 DH" /><Field label="Valeur épave" value="—" /><Field label="TVA récupérable" value="Non" /></div><div className="indemnity"><span>Montant d’indemnisation</span><strong>2 400,00 DH</strong><p>Deux mille quatre cents dirhams et zéro centime</p></div></div>;
}
