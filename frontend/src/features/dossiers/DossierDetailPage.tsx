import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { annulerImport, archiverDossier, cles, dossier as chargerDossier, historique as chargerHistorique } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterDossier, telechargerRapportPdf } from "@/api/exports";
import { peutAcceder, ROUTES } from "@/app/routes";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { Button } from "@/components/ui/Button";
import { Info, InfoCard } from "@/components/ui/InfoCard";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { date, dateHeure, montant } from "@/domain/format";
import { STATUTS_ANNULABLES, STATUTS_CLOS, ZONES_CHOC } from "@/domain/referentiels";
import type { DossierDetail } from "@/domain/dossiers";
import { useUtilisateur } from "@/features/auth/AuthContext";
import { DamageTable } from "./components/DamageTable";
import { DetailPieces } from "./components/DetailPieces";
import { usePdfOriginal } from "./components/PdfViewer";
import { ValeursCommentaire } from "./components/ValeursCommentaire";
import { VehicleDiagram } from "./components/VehicleDiagram";
import { CONTRATS, ENERGIES, EVENEMENTS, libelle, libelleAction, NATURES, PROCEDURES } from "./libelles";

/** Fiche complète d'un dossier (F8), PDF original (P1) et historique des actions (P7). */
export default function DossierDetailPage() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const numero = useParams().numero!;
  // Un import annulé s'ouvre par son id : le bon PDF a pu être réimporté sous le même n°
  const idParam = useSearchParams()[0].get("id");
  const id = idParam ? Number(idParam) : undefined;
  const { role } = useUtilisateur();
  const requete = useQuery({ queryKey: cles.dossier(numero, id), queryFn: () => chargerDossier(numero, id) });
  const evenements = useQuery({ queryKey: cles.historique(numero, id), queryFn: () => chargerHistorique(numero, id) });
  const pdf = usePdfOriginal(requete.data?.importId ?? null);
  const [motif, setMotif] = useState<string | null>(null);

  function appliquer(d: DossierDetail) {
    void queryClient.invalidateQueries({ queryKey: ["dossiers"] });
    void queryClient.invalidateQueries({ queryKey: ["statistiques"] });
    void queryClient.invalidateQueries({ queryKey: ["recherche"] });
    // Après une annulation, la fiche reste celle de cet import, adressée par son id
    if (d.statut === "ANNULE" && id == null) navigate(`${ROUTES.dossier(d.numeroRapport)}?id=${d.id}`, { replace: true });
  }
  const archivage = useMutation({ mutationFn: () => archiverDossier(numero), onSuccess: appliquer });
  const annulation = useMutation({
    mutationFn: (texte: string) => annulerImport(numero, texte),
    onSuccess: (d) => { setMotif(null); appliquer(d); },
  });

  if (requete.isPending) return <main className="main detail-page"><PageHead title={`Dossier ${numero}`} /><p className="etat-chargement">Chargement du dossier…</p></main>;
  if (requete.isError) return <main className="main detail-page"><PageHead title={`Dossier ${numero}`} /><p className="import-erreur" role="alert">{messageErreur(requete.error, "Dossier introuvable.")}</p><Button variant="secondary" onClick={() => navigate(ROUTES.dossiers)}>Retour aux dossiers</Button></main>;

  const d = requete.data;
  const tvaTexte = d.tvaRecuperable === null ? "TVA : non renseignée" : d.tvaRecuperable ? "Base HT · TVA récupérable" : "TVA incluse · TVA non récupérable";
  const sousTitre = d.statut === "ANNULE" ? `Import annulé le ${dateHeure(d.annuleLe)} par ${d.annulePar}`
    : d.statut === "VALIDE" || d.statut === "ARCHIVE" ? `Validé le ${dateHeure(d.valideLe)} par ${d.validePar}` : `Importé le ${dateHeure(d.creeLe)}`;
  const clos = STATUTS_CLOS.includes(d.statut);
  const valide = d.statut === "VALIDE" || d.statut === "ARCHIVE";
  const peutArchiver = d.statut === "VALIDE" && peutAcceder(role, "verification");
  const peutAnnuler = role === "ADMINISTRATEUR" && STATUTS_ANNULABLES.includes(d.statut);
  const erreurAction = archivage.error ?? annulation.error;
  return <main className="main detail-page">
    <PageHead title={`Dossier ${d.numeroRapport}`} subtitle={sousTitre} actions={<>
      <StatusBadge statut={d.statut} />
      <Button variant="secondary" icon="eye" disabled={!pdf.adresse} onClick={() => pdf.adresse && window.open(pdf.adresse, "_blank", "noopener")}>Voir le PDF original</Button>
      {d.statut !== "ANNULE" && <BoutonTelechargement icon="file" action={() => telechargerRapportPdf(d.numeroRapport)} disabled={!valide}
        title={!valide ? "Le rapport PDF se génère à partir d’un dossier validé (P10)" : "Rapport PDF fidèle au modèle d’origine"}
        apres={() => void queryClient.invalidateQueries({ queryKey: ["rapports"] })}>Générer le rapport PDF</BoutonTelechargement>}
      {d.statut !== "ANNULE" && <BoutonTelechargement action={() => exporterDossier(d.numeroRapport)}>Exporter en Excel</BoutonTelechargement>}
      {peutArchiver && <Button variant="secondary" disabled={archivage.isPending} title="Clôt le dossier validé : il reste consultable mais ne se modifie plus"
        onClick={() => archivage.mutate()}>{archivage.isPending ? "Archivage…" : "Archiver"}</Button>}
      {peutAnnuler && motif === null && <Button variant="secondary" title="Écarte un import erroné ; le PDF et l’historique sont conservés"
        onClick={() => setMotif("")}>Annuler l’import</Button>}
      {!clos && peutAcceder(role, "verification") && <Button onClick={() => navigate(ROUTES.verification(d.numeroRapport))}>{d.statut === "VALIDE" ? "Modifier" : "Vérifier"}</Button>}
    </>} />
    {motif !== null && <form className="card bulk-bar" onSubmit={(e) => { e.preventDefault(); if (motif.trim()) annulation.mutate(motif.trim()); }}>
      <label className="filter" style={{ flex: 1 }}><span>Motif de l’annulation (obligatoire)</span>
        <input autoFocus maxLength={500} value={motif} onChange={(e) => setMotif(e.target.value)} placeholder="Ex. : mauvais fichier déposé, rapport d’un autre dossier…" /></label>
      <span>Le dossier sort des listes et des statistiques. Le PDF original et l’historique sont conservés, et le bon PDF pourra être réimporté.</span>
      <Button type="submit" variant="danger" disabled={!motif.trim() || annulation.isPending}>{annulation.isPending ? "Annulation…" : "Confirmer l’annulation"}</Button>
      <Button type="button" variant="secondary" onClick={() => setMotif(null)}>Retour</Button>
    </form>}
    {erreurAction && <p className="import-erreur" role="alert">{messageErreur(erreurAction, "Action impossible.")}</p>}
    {d.statut === "ANNULE" && <p className="import-erreur" role="status">Import annulé le {dateHeure(d.annuleLe)} par {d.annulePar} : {d.motifAnnulation}</p>}
    <div className="detail-layout"><div className="detail-main">
      <div className="amount-banner"><span>Montant d’indemnisation{valide ? " validé" : ""}</span><strong>{montant(d.montantIndemnisation)}</strong><small>{tvaTexte}</small></div>
      <InfoCard title="Sinistre"><div className="info-grid">
        <Info label="N° de rapport" value={d.numeroRapport} /><Info label="Nature" value={NATURES[d.natureRapport]} /><Info label="Procédure" value={libelle(PROCEDURES, d.typeProcedure)} />
        <Info label="Réf. sinistre" value={d.refSinistre} /><Info label="Date du sinistre" value={date(d.dateSinistre)} /><Info label="Date de mission" value={date(d.dateMission)} />
        <Info label="Compagnie" value={d.compagnie} /><Info label="Contrat" value={libelle(CONTRATS, d.natureContrat)} /><Info label="Événement" value={libelle(EVENEMENTS, d.natureEvenement)} />
        <Info label="Cabinet" value={d.cabinet} /><Info label="Expert" value={d.expert} /><Info label="Réparateur" value={d.reparateur} />
        <Info label="Expertise du véhicule" value={date(d.dateExpertise)} /><Info label="Après travaux" value={date(d.dateApresTravaux)} /><Info label="Date du rapport" value={date(d.dateRapport)} />
      </div></InfoCard>
      <InfoCard title="Parties"><div className="party-summary">
        <section><h3>Assuré</h3><Info label="Nom" value={d.assure?.nomPrenom} /><Info label="Véhicule" value={[d.assure?.vehicule, d.assure?.immatriculation].filter(Boolean).join(" · ")} /><Info label="Police" value={d.assure?.numeroPolice} /></section>
        <section><h3>Adversaire</h3>{d.adversaire ? <><Info label="Nom" value={d.adversaire.nomPrenom} /><Info label="Véhicule" value={[d.adversaire.vehicule, d.adversaire.immatriculation].filter(Boolean).join(" · ")} /><Info label="Compagnie" value={d.adversaire.compagnie} /></> : <p className="etat-chargement">Pas d’adversaire.</p>}</section>
      </div></InfoCard>
      <InfoCard title="Véhicule"><div className="vehicle-summary"><div className="info-grid">
        <Info label="Marque / Modèle" value={[d.vehicule?.marque, d.vehicule?.modele].filter(Boolean).join(" ")} />
        <Info label="Énergie" value={libelle(ENERGIES, d.vehicule?.energie)} />
        <Info label="Kilométrage" value={d.vehicule?.kilometrageNonEstimable ? "Non estimable" : d.vehicule?.kilometrage != null ? `${d.vehicule.kilometrage.toLocaleString("fr-FR")} km` : null} />
        <Info label="N° de série" value={d.vehicule?.numeroSerie} />
        <Info label={d.zonesChoc.length > 1 ? "Zones de choc" : "Zone de choc"} value={d.zonesChoc.map((z) => `${z} · ${ZONES_CHOC[z]}`).join(", ") || null} />
      </div><VehicleDiagram zones={d.zonesChoc} /></div></InfoCard>
      <InfoCard title="Évaluation des dommages"><DamageTable dossier={d} /></InfoCard>
      <InfoCard title="Détail des pièces"><DetailPieces lignes={d.lignesDetail} /></InfoCard>
      <InfoCard title="Conclusion"><div className="info-grid">
        <Info label={d.vetusteHt !== null ? "Vétusté HT" : "Vétusté TTC"} value={montant(d.vetusteHt ?? d.vetusteTtc)} /><Info label="Dépannage" value={montant(d.depannage)} />
        <Info label="Valeur à neuf" value={montant(d.valeurNeuf)} /><Info label="Valeur vénale" value={montant(d.valeurVenale)} /><Info label="Valeur épave" value={montant(d.valeurEpave)} />
        <Info label="Réforme" value={d.typeReforme === "ECONOMIQUE" ? "Économique" : d.typeReforme === "TECHNIQUE" ? "Technique" : "Non"} />
      </div>{d.montantLettres && <div className="indemnity"><span>Montant en lettres</span><strong>{montant(d.montantIndemnisation)}</strong><p>{d.montantLettres}</p></div>}
        {d.commentaire && <p className="lecture-seule">{d.commentaire}</p>}</InfoCard>
      {d.commentaire && <InfoCard title="Valeurs clés du commentaire"><ValeursCommentaire valeurs={d.valeursCommentaire} /></InfoCard>}
    </div>
      <aside className="history card"><h2>Historique</h2><p>Traçabilité des actions</p><div className="timeline">
        {evenements.isPending && <span className="vide">Chargement…</span>}
        {evenements.data?.length === 0 && <span className="vide">Aucune action enregistrée.</span>}
        {evenements.data?.map((e, i) => <div key={i} className={["VALIDATION", "ANNULATION", "ARCHIVAGE"].includes(e.action) ? "important" : ""}><i />
          <strong>{libelleAction(e.action)}{e.champ ? ` : ${e.champ}` : ""}</strong>
          <span>{e.champ ? `${e.ancienneValeur ?? "vide"} → ${e.nouvelleValeur ?? "vide"}` : `par ${e.utilisateur ?? "système"}`}</span>
          <small>{dateHeure(e.horodatage)}{e.champ && e.utilisateur ? ` · ${e.utilisateur}` : ""}</small></div>)}
      </div></aside>
    </div>
  </main>;
}
