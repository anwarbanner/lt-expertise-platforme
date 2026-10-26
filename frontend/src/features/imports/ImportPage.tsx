import { useEffect, useRef, useState, type DragEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { messageErreur } from "@/api/client";
import { adresseDossier, ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { LIBELLES_STATUT_IMPORT, TAILLE_MAX_IMPORT_MO, type RecapitulatifLot, type ResultatImport } from "@/domain/imports";
import { useUtilisateur } from "@/features/auth/AuthContext";
import { clesImports, historiqueImports, importerFichiers, recapitulatifLot } from "./api";

const TAILLE_MAX_OCTETS = TAILLE_MAX_IMPORT_MO * 1024 * 1024;

/**
 * Import de PDF (F1–F4) : dépôt, suivi du lot traité en arrière-plan, résultat fichier par fichier (statut calculé
 * après extraction et contrôles), historique des imports.
 */
export default function ImportPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { role } = useUtilisateur();
  const champFichiers = useRef<HTMLInputElement>(null);
  const [survol, setSurvol] = useState(false);
  const [ignores, setIgnores] = useState<string[]>([]);
  const [lot, setLot] = useState<string | null>(null);

  const depot = useMutation({
    mutationFn: importerFichiers,
    onSuccess: (recap) => {
      queryClient.setQueryData(clesImports.lot(recap.lot), recap);
      setLot(recap.lot);
    },
  });

  const suivi = useQuery({
    queryKey: clesImports.lot(lot),
    queryFn: () => recapitulatifLot(lot!),
    enabled: lot !== null,
    refetchInterval: (requete) => (requete.state.data?.statut === "EN_COURS" ? 1000 : false),
  });

  const historique = useQuery({ queryKey: clesImports.historique(false), queryFn: () => historiqueImports(false) });

  const recap = suivi.data;
  useEffect(() => {
    if (recap?.statut === "TERMINE") {
      void queryClient.invalidateQueries({ queryKey: clesImports.historique(false) });
    }
  }, [recap?.statut, queryClient]);

  function deposer(liste: FileList | null) {
    if (!liste || depot.isPending) return;
    const fichiers = Array.from(liste);
    const acceptes = fichiers.filter((f) => estPdf(f) && f.size <= TAILLE_MAX_OCTETS);
    setIgnores(fichiers.filter((f) => !acceptes.includes(f))
      .map((f) => `${f.name} : ${estPdf(f) ? `plus de ${TAILLE_MAX_IMPORT_MO} Mo` : "pas un PDF"}`));
    if (acceptes.length > 0) depot.mutate(acceptes);
  }

  function lacher(evenement: DragEvent) {
    evenement.preventDefault();
    setSurvol(false);
    deposer(evenement.dataTransfer.files);
  }

  return <main className="main"><PageHead title="Importer des rapports" subtitle="Import et extraction automatique de rapports d’expertise PDF" actions={<Button variant="secondary" icon="folder" onClick={() => navigate(ROUTES.dossiers)}>Voir les dossiers</Button>} />
    <article className={`dropzone ${survol ? "survol" : ""}`} onDragOver={(e) => { e.preventDefault(); setSurvol(true); }} onDragLeave={() => setSurvol(false)} onDrop={lacher}>
      <div className="drop-icon"><Icon name="upload" size={25} /></div>
      <h2>Déposez vos rapports d’expertise PDF ici</h2>
      <p>ou cliquez pour parcourir vos fichiers</p>
      <span>Un ou plusieurs fichiers PDF · {TAILLE_MAX_IMPORT_MO} Mo maximum chacun</span>
      <input ref={champFichiers} type="file" accept="application/pdf,.pdf" multiple hidden aria-label="Fichiers PDF à importer"
        onChange={(e) => { deposer(e.target.files); e.target.value = ""; }} />
      <Button icon="plus" disabled={depot.isPending} onClick={() => champFichiers.current?.click()}>
        {depot.isPending ? "Envoi en cours…" : "Sélectionner des fichiers"}
      </Button>
      {depot.isError && <p className="import-erreur" role="alert">{messageErreur(depot.error, "L’import a échoué.")}</p>}
      {ignores.length > 0 && <ul className="import-ignores">{ignores.map((m) => <li key={m}>Ignoré — {m}</li>)}</ul>}
    </article>

    {recap?.statut === "EN_COURS" && <article className="card process-card">
      <div className="section-head"><div><h2>Traitement en cours</h2><p>{recap.traites} fichier{recap.traites > 1 ? "s" : ""} traité{recap.traites > 1 ? "s" : ""} sur {recap.total}</p></div><span className="live-dot">TRAITEMENT ACTIF</span></div>
      <div className="progress import-progression"><i style={{ width: `${Math.round((recap.traites / recap.total) * 100)}%` }} /></div>
    </article>}

    {recap && <Resultat recap={recap} ouvrir={(r) => navigate(adresseDossier(r.numeroRapport!, r.statutDossier!, role))} />}

    <article className="card result-card">
      <div className="section-head"><div><h2>Derniers imports</h2><p>Historique des fichiers déposés, acceptés ou refusés</p></div></div>
      {historique.isPending && <p className="etat-chargement">Chargement…</p>}
      {historique.isError && <p className="import-erreur" role="alert">{messageErreur(historique.error, "Historique indisponible.")}</p>}
      {historique.data && (historique.data.content.length === 0
        ? <p className="etat-chargement">Aucun import pour l’instant.</p>
        : <DataTable headers={["FICHIER", "DÉPOSÉ LE", "PAR", "PAGES", "STATUT"]}>{historique.data.content.map((f) =>
          <tr key={f.id} className={f.statutImport !== "ACCEPTE" ? "rejected" : ""}>
            <td><div className="filename"><Icon name="file" size={16} /><div><strong>{f.nomFichier}</strong>{f.motifRefus && <small>{f.motifRefus}</small>}</div></div></td>
            <td>{new Date(f.importeLe).toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" })}</td>
            <td>{f.importePar}</td>
            <td>{f.nombrePages ?? "—"}</td>
            <td>{f.statutImport === "ACCEPTE" ? LIBELLES_STATUT_IMPORT.ACCEPTE : <StatusBadge statut="REFUSE" />}</td>
          </tr>)}</DataTable>)}
    </article>
  </main>;
}

function Resultat({ recap, ouvrir }: { recap: RecapitulatifLot; ouvrir: (r: ResultatImport) => void }) {
  const compteurs: [string, number, string][] = [
    ["Réussis", recap.reussis, "navy"], ["Prêts à valider", recap.pretsAValider, "blue"],
    ["À compléter", recap.aCompleter, "amber"], ["À vérifier", recap.aVerifier, "red"],
    ["Refusés ou en erreur", recap.refuses + recap.enErreur, "gray"]];
  return <article className="card result-card">
    <div className="section-head"><div><h2>Résultat de l’import</h2><p>Dépôt du {new Date(recap.creeLe).toLocaleString("fr-FR", { dateStyle: "long", timeStyle: "short" })} · {recap.traites}/{recap.total} traité{recap.total > 1 ? "s" : ""}</p></div></div>
    <div className="result-counts">{compteurs.map(([libelle, valeur, ton]) => <div key={libelle}><i className={ton} /><span>{libelle}</span><strong>{valeur}</strong></div>)}</div>
    <DataTable headers={["FICHIER", "N° RAPPORT", "CONTRÔLES", "CHAMPS À SAISIR", "STATUT", "ACTION"]}>{recap.resultats.map((r) => {
      const accepte = r.statutDossier !== null;
      return <tr key={`${r.nomFichier}-${r.importId ?? r.motif}`} className={accepte ? "" : "rejected"}>
        <td><div className="filename"><Icon name="file" size={16} /><div><strong>{r.nomFichier}</strong>{r.motif && <small>{r.motif}</small>}</div></div></td>
        <td>{r.numeroRapport ?? "—"}</td>
        <td>{accepte ? `${r.controlesConformes}/${r.controlesTotal}` : "—"}</td>
        <td>{accepte ? r.champsASaisir : "—"}</td>
        <td><StatusBadge statut={r.statutDossier ?? "REFUSE"} /></td>
        <td>{accepte && r.numeroRapport && <Button variant="secondary" onClick={() => ouvrir(r)}>Vérifier</Button>}</td>
      </tr>;
    })}</DataTable>
  </article>;
}

function estPdf(fichier: File) {
  return fichier.type === "application/pdf" || fichier.name.toLowerCase().endsWith(".pdf");
}
