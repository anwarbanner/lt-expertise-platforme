import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router";
import { cles, dossier as chargerDossier, modifierDossier, validerDossier } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { ChampSaisie } from "@/components/ui/Field";
import { Icon } from "@/components/ui/Icon";
import { StatusBadge } from "@/components/ui/StatusBadge";
import type { Controle, DossierDetail, DossierModification, Partie, Vehicule } from "@/domain/dossiers";
import { dateHeure } from "@/domain/format";
import { ZONES_CHOC } from "@/domain/referentiels";
import { DamageTable } from "./components/DamageTable";
import { DetailPieces } from "./components/DetailPieces";
import { PdfViewer } from "./components/PdfViewer";
import { ValeursCommentaire } from "./components/ValeursCommentaire";
import { VehicleDiagram } from "./components/VehicleDiagram";
import { CONTRATS, ENERGIES, EVENEMENTS, NATURES, OUI_NON, PROCEDURES } from "./libelles";

const ONGLETS = ["Sinistre", "Parties", "Véhicule", "Évaluation des dommages", "Détail des pièces", "Conclusion"] as const;
const PARTIE_VIDE: Partie = { nomPrenom: null, vehicule: null, immatriculation: null, compagnie: null, numeroPolice: null, intermediaire: null };
const VEHICULE_VIDE: Vehicule = {
  marque: null, modele: null, typeMine: null, immatriculation: null, numeroSerie: null, energie: null, kilometrage: null,
  kilometrageNonEstimable: false, puissanceFiscale: null, dateMiseCirculation: null, etatGeneral: null,
  usurePneuAvg: null, usurePneuAvd: null, usurePneuArg: null, usurePneuArd: null,
};

/** Ordre d'affichage des contrôles : bloquants en échec, avertissements, puis conformes. */
const rang = (c: Controle) => (c.conforme ? 2 : c.bloquant ? 0 : 1);

/** Reprend les champs modifiables d'un dossier, tels qu'envoyés à PUT /api/dossiers/{numero}. */
function brouillonDe(d: DossierDetail): DossierModification {
  return {
    version: d.version, typeProcedure: d.typeProcedure, cabinet: d.cabinet, expert: d.expert, compagnie: d.compagnie,
    refSinistre: d.refSinistre, dateSinistre: d.dateSinistre, dateMission: d.dateMission, dateExpertise: d.dateExpertise,
    dateAvantReparation: d.dateAvantReparation, dateEnCoursTravaux: d.dateEnCoursTravaux, dateApresTravaux: d.dateApresTravaux,
    dateRapport: d.dateRapport, lieuRapport: d.lieuRapport, natureContrat: d.natureContrat, natureEvenement: d.natureEvenement,
    zonesChoc: [...d.zonesChoc], reparateur: d.reparateur, accordReparateur: d.accordReparateur,
    assure: d.assure ?? PARTIE_VIDE, adversaire: d.adversaire ?? PARTIE_VIDE, vehicule: d.vehicule ?? VEHICULE_VIDE,
    vetusteTtc: d.vetusteTtc, depannage: d.depannage, valeurNeuf: d.valeurNeuf, valeurAssuree: d.valeurAssuree,
    valeurVenale: d.valeurVenale, valeurEpave: d.valeurEpave, tvaRecuperable: d.tvaRecuperable,
    montantIndemnisation: d.montantIndemnisation, montantLettres: d.montantLettres, commentaire: d.commentaire,
    prixLignes: d.lignesDetail.filter((l) => !l.prixCoherent).map((l) => ({ ordre: l.ordre, prixUnitaire: l.prixUnitaire })),
  };
}

/** Écran de vérification (F5) : PDF original à gauche, données modifiables à droite, contrôles de cohérence (P4). */
export default function VerificationPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const numero = useParams().numero!;
  const [onglet, setOnglet] = useState(0);
  const [controlesOuverts, setControlesOuverts] = useState(true);
  const requete = useQuery({ queryKey: cles.dossier(numero), queryFn: () => chargerDossier(numero) });
  const [brouillon, setBrouillon] = useState<DossierModification | null>(null);
  const [enregistreLe, setEnregistreLe] = useState<string | null>(null);

  useEffect(() => {
    if (requete.data) setBrouillon(brouillonDe(requete.data));
  }, [requete.data]);

  function appliquer(d: DossierDetail) {
    queryClient.setQueryData(cles.dossier(numero), d);
    void queryClient.invalidateQueries({ queryKey: ["dossiers", "liste"] });
    void queryClient.invalidateQueries({ queryKey: cles.compteurs });
    void queryClient.invalidateQueries({ queryKey: cles.historique(numero) });
    void queryClient.invalidateQueries({ queryKey: ["statistiques"] });
  }

  const enregistrement = useMutation({
    mutationFn: (m: DossierModification) => modifierDossier(numero, m),
    onSuccess: (d) => { appliquer(d); setEnregistreLe(new Date().toISOString()); },
  });
  const validation = useMutation({
    mutationFn: () => validerDossier(numero),
    onSuccess: (d) => { appliquer(d); navigate(ROUTES.dossier(numero)); },
  });

  const d = requete.data;
  const modifie = useMemo(() => !!d && !!brouillon && JSON.stringify(brouillon) !== JSON.stringify(brouillonDe(d)), [d, brouillon]);

  if (requete.isPending || (d && !brouillon)) return <main className="verify-page"><p className="etat-chargement">Chargement du dossier…</p></main>;
  if (requete.isError || !d || !brouillon) return <main className="verify-page"><p className="import-erreur" role="alert">{messageErreur(requete.error, "Dossier introuvable.")}</p><Button variant="secondary" onClick={() => navigate(ROUTES.dossiers)}>Retour aux dossiers</Button></main>;

  const coherence = d.controles.filter((c) => c.type === "COHERENCE");
  const conformes = coherence.filter((c) => c.conforme).length;
  // Seuls les échecs bloquants empêchent la validation ; les autres sont des avertissements
  const bloquants = coherence.filter((c) => !c.conforme && c.bloquant);
  const essentielsManquants = d.controles.filter((c) => c.type === "CHAMP_MANQUANT" && c.bloquant).map((c) => c.champ!);
  const champsEnErreur = new Set([...bloquants.filter((c) => c.champ).map((c) => c.champ!), ...essentielsManquants]);
  const facultatifs = d.champsASaisir.filter((c) => !essentielsManquants.includes(c));
  const peutValider = d.statut === "PRET_A_VALIDER" && !modifie;
  const raisonBlocage = modifie ? "Enregistrez d’abord vos modifications" : d.statut === "VALIDE" ? "Dossier déjà validé"
    : d.statut !== "PRET_A_VALIDER" ? [bloquants.length ? `${bloquants.length} contrôle(s) bloquant(s) en échec` : "",
      essentielsManquants.length ? `donnée essentielle manquante : ${essentielsManquants.join(", ")}` : ""].filter(Boolean).join(" ; ") : undefined;

  /** Valeur du brouillon au chemin « champ » ou « bloc.champ », en texte pour le formulaire. */
  function valeur(chemin: string): string {
    const [bloc, champ] = chemin.split(".");
    const source = (champ ? (brouillon as unknown as Record<string, Record<string, unknown>>)[bloc] : brouillon) as unknown as Record<string, unknown>;
    const v = source?.[champ ?? bloc];
    return v === null || v === undefined ? "" : String(v);
  }

  function changer(chemin: string, texte: string, sorte: "texte" | "nombre" | "booleen" = "texte") {
    const v = texte === "" ? null : sorte === "nombre" ? Number(texte) : sorte === "booleen" ? texte === "true" : texte;
    const [bloc, champ] = chemin.split(".");
    setBrouillon((b) => {
      if (!b) return b;
      if (!champ) return { ...b, [bloc]: v };
      const sousBloc = (b as unknown as Record<string, object>)[bloc];
      return { ...b, [bloc]: { ...sousBloc, [champ]: v } };
    });
  }

  function changerZones(zones: number[]) {
    setBrouillon((b) => b && { ...b, zonesChoc: zones });
  }

  function changerPrix(ordre: number, prix: number | null) {
    setBrouillon((b) => b && { ...b, prixLignes: b.prixLignes.map((p) => (p.ordre === ordre ? { ...p, prixUnitaire: prix } : p)) });
  }

  function etat(chemin: string): "warning" | "error" | undefined {
    if (champsEnErreur.has(chemin)) return "error";
    return d!.champsASaisir.includes(chemin) && valeur(chemin) === "" ? "warning" : undefined;
  }

  const texte = (label: string, chemin: string, type: "text" | "date" | "textarea" = "text") =>
    <ChampSaisie key={chemin} label={label} value={valeur(chemin)} type={type} state={etat(chemin)} onChange={(t) => changer(chemin, t)} />;
  const nombreChamp = (label: string, chemin: string) =>
    <ChampSaisie key={chemin} label={label} value={valeur(chemin)} type="number" state={etat(chemin)} onChange={(t) => changer(chemin, t, "nombre")} />;
  const liste = (label: string, chemin: string, options: readonly (readonly [string, string])[], sorte: "texte" | "nombre" | "booleen" = "texte") =>
    <ChampSaisie key={chemin} label={label} value={valeur(chemin)} options={options} state={etat(chemin)} onChange={(t) => changer(chemin, t, sorte)} />;
  const partie = (bloc: "assure" | "adversaire") => <>{texte("Nom complet", `${bloc}.nomPrenom`)}{texte("Véhicule", `${bloc}.vehicule`)}
    {texte("Immatriculation", `${bloc}.immatriculation`)}{texte("Compagnie", `${bloc}.compagnie`)}{texte("N° de police", `${bloc}.numeroPolice`)}{texte("Intermédiaire", `${bloc}.intermediaire`)}</>;

  const contenu = [
    <div className="fields-grid" key="sinistre">
      <ChampSaisie label="N° de rapport" value={d.numeroRapport} onChange={() => {}} disabled aide={`Nature : ${NATURES[d.natureRapport]}`} />
      {texte("Réf. sinistre", "refSinistre")}{texte("Date du sinistre", "dateSinistre", "date")}{texte("Date de mission", "dateMission", "date")}
      {texte("Compagnie", "compagnie")}{liste("Nature du contrat", "natureContrat", CONTRATS)}{liste("Procédure", "typeProcedure", PROCEDURES)}
      {texte("Cabinet", "cabinet")}{texte("Expert", "expert")}{texte("Réparateur", "reparateur")}
      {liste("Accord du réparateur", "accordReparateur", OUI_NON, "booleen")}{liste("Nature de l’événement", "natureEvenement", EVENEMENTS)}
      <fieldset className="data-field zones-choc"><legend>Zones de choc</legend><div>{Object.entries(ZONES_CHOC).map(([code, libelle]) => {
        const n = Number(code);
        const coche = brouillon.zonesChoc.includes(n);
        return <label key={code}><input type="checkbox" checked={coche} onChange={() => changerZones(coche ? brouillon.zonesChoc.filter((z) => z !== n) : [...brouillon.zonesChoc, n].sort((a, b) => a - b))} />{code} · {libelle}</label>;
      })}</div></fieldset>
      {texte("Expertise du véhicule", "dateExpertise", "date")}{texte("Avant réparation", "dateAvantReparation", "date")}{texte("En cours de travaux", "dateEnCoursTravaux", "date")}
      {texte("Après travaux", "dateApresTravaux", "date")}{texte("Date du rapport", "dateRapport", "date")}{texte("Lieu du rapport", "lieuRapport")}
    </div>,
    <div className="party-grid" key="parties"><section><h3>Assuré</h3>{partie("assure")}</section>
      <section><h3>Adversaire</h3>{partie("adversaire")}<p className="etat-chargement">Laisser vide s’il n’y a pas d’adversaire.</p></section></div>,
    <div className="vehicle-tab" key="vehicule"><div className="fields-grid">
      {texte("Marque", "vehicule.marque")}{texte("Modèle", "vehicule.modele")}{texte("Type mine", "vehicule.typeMine")}
      {texte("Immatriculation", "vehicule.immatriculation")}{texte("N° de série", "vehicule.numeroSerie")}{liste("Énergie", "vehicule.energie", ENERGIES)}
      {nombreChamp("Kilométrage", "vehicule.kilometrage")}{liste("Kilométrage non estimable", "vehicule.kilometrageNonEstimable", OUI_NON, "booleen")}{nombreChamp("Puissance fiscale", "vehicule.puissanceFiscale")}
      {texte("Mise en circulation", "vehicule.dateMiseCirculation", "date")}{texte("État général", "vehicule.etatGeneral")}
      {nombreChamp("Pneu AVG (%)", "vehicule.usurePneuAvg")}{nombreChamp("Pneu AVD (%)", "vehicule.usurePneuAvd")}{nombreChamp("Pneu ARG (%)", "vehicule.usurePneuArg")}{nombreChamp("Pneu ARD (%)", "vehicule.usurePneuArd")}
    </div><VehicleDiagram zones={brouillon.zonesChoc} changer={changerZones} /></div>,
    <div key="evaluation"><p className="lecture-seule">Montants lus dans le rapport : ils ne se modifient pas ici, les contrôles les recoupent.</p><DamageTable dossier={d} /></div>,
    <div key="pieces">{d.lignesDetail.some((l) => !l.prixCoherent)
      ? <p className="verify-erreur" role="note">Les lignes en rouge ont un prix unitaire incohérent avec leur total (erreur du PDF) : corrigez le prix (le bouton → propose le prix déduit du total), puis enregistrez. La validation reste bloquée tant qu’une ligne est en rouge.</p>
      : <p className="lecture-seule">Lignes lues en page 2 du rapport ; tous les prix unitaires sont cohérents avec les totaux.</p>}
      <DetailPieces lignes={d.lignesDetail} prix={Object.fromEntries(brouillon.prixLignes.map((p) => [p.ordre, p.prixUnitaire]))} changerPrix={changerPrix} /></div>,
    <div className="conclusion" key="conclusion"><div className="fields-grid">
      {nombreChamp(d.vetusteHt !== null ? "Vétusté TTC (le rapport indique une vétusté HT)" : "Vétusté TTC", "vetusteTtc")}{nombreChamp("Dépannage", "depannage")}
      {liste("TVA récupérable", "tvaRecuperable", OUI_NON, "booleen")}{nombreChamp("Valeur à neuf", "valeurNeuf")}{nombreChamp("Valeur assurée", "valeurAssuree")}
      {nombreChamp("Valeur vénale", "valeurVenale")}{nombreChamp("Valeur épave", "valeurEpave")}{nombreChamp("Montant de l’indemnisation", "montantIndemnisation")}
      {texte("Montant en lettres", "montantLettres", "textarea")}{texte("Commentaire de l’expert", "commentaire", "textarea")}
    </div>{d.commentaire && <><h3 className="sous-titre">Valeurs clés du commentaire</h3><ValeursCommentaire valeurs={d.valeursCommentaire} /></>}</div>,
  ];
  const ongletAvecAlerte = (i: number) => {
    const prefixes = [["refSinistre", "date", "compagnie", "natureContrat", "cabinet", "expert", "reparateur", "lieuRapport"], ["assure.", "adversaire."], ["vehicule."], [], ["lignesDetail"], ["montant", "commentaire", "vetuste", "valeur"]][i];
    return [...d.champsASaisir, ...champsEnErreur].some((c) => prefixes.some((p) => c.startsWith(p)));
  };

  return <main className="verify-page">
    <div className="verify-head"><div><button onClick={() => navigate(ROUTES.dossiers)}>‹ Dossiers</button>
      <div><h1>Vérification du dossier {d.numeroRapport}</h1><StatusBadge statut={d.statut} /></div>
      <p>Importé le {dateHeure(d.creeLe)}{d.nomFichier ? ` · ${d.nomFichier}` : ""}</p></div>
      <div className="head-actions">
        <Button variant="secondary" onClick={() => navigate(ROUTES.dossier(d.numeroRapport))}>Fiche du dossier</Button>
        <Button variant="secondary" disabled={!modifie || enregistrement.isPending} onClick={() => enregistrement.mutate(brouillon)}>{enregistrement.isPending ? "Enregistrement…" : "Enregistrer"}</Button>
        <Button disabled={!peutValider || validation.isPending} title={raisonBlocage} onClick={() => validation.mutate()}>{validation.isPending ? "Validation…" : "Valider le dossier"}</Button>
      </div></div>
    <div className="brand-rule"><i /><i /></div>
    {(enregistrement.isError || validation.isError) && <p className="verify-erreur" role="alert">{messageErreur(enregistrement.error ?? validation.error, "Opération impossible.")}</p>}
    <div className="verify-split"><PdfViewer importId={d.importId} nomFichier={d.nomFichier} />
      <section className="form-panel"><div className="form-tabs">{ONGLETS.map((t, i) => <button className={onglet === i ? "active" : ""} key={t} onClick={() => setOnglet(i)}>{t}{ongletAvecAlerte(i) && <i />}</button>)}</div>
        <div className="form-content"><div className="tab-heading"><div><span>ÉTAPE {onglet + 1} SUR {ONGLETS.length}</span><h2>{ONGLETS[onglet]}</h2></div>
          {modifie ? <span className="saved non-enregistre">Modifications non enregistrées</span>
            : enregistreLe ? <span className="saved"><Icon name="check" size={13} /> Enregistré à {new Date(enregistreLe).toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" })}</span> : null}</div>
          {contenu[onglet]}</div></section></div>
    <aside className={`checks-panel ${controlesOuverts ? "open" : ""}`}>
      <button className="checks-head" onClick={() => setControlesOuverts(!controlesOuverts)}><span><Icon name="check" size={17} /><strong>Contrôles de cohérence</strong><b>{conformes}/{coherence.length}</b></span><span>{controlesOuverts ? "⌄" : "⌃"}</span></button>
      {controlesOuverts && <div className="check-list">
        {essentielsManquants.length > 0 && <div className="bad"><i>!</i><span>Donnée essentielle manquante<small>{essentielsManquants.join(", ")} : nécessaire pour valider</small></span></div>}
        {[...coherence].sort((a, b) => rang(a) - rang(b)).map((c) =>
          <div key={c.code} className={c.conforme ? "ok" : c.bloquant ? "bad" : "warn"}><i>{c.conforme ? "✓" : c.bloquant ? "×" : "!"}</i><span>{c.libelle}
            {!c.conforme && (c.code === "PAGE2_LIGNES_PRIX"
              ? <small>{(c.attendu ?? 0) - (c.obtenu ?? 0)} ligne(s) au prix incohérent sur {c.attendu ?? 0} <button className="link-button" onClick={() => setOnglet(4)}>Corriger les prix</button></small>
              : <small>Attendu {c.attendu ?? "—"}, trouvé {c.obtenu ?? "—"}</small>)}</span></div>)}
        {facultatifs.length > 0 && <div className="warn"><i>!</i><span>{facultatifs.length} champ(s) absent(s) du PDF<small>Saisie facultative : {facultatifs.slice(0, 6).join(", ")}{facultatifs.length > 6 ? "…" : ""}</small></span></div>}
      </div>}
    </aside>
  </main>;
}
