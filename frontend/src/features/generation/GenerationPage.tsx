import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router";
import { cles, dossier, rechercherDossiers } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterDossier, historiqueRapports, telechargerRapportPdf } from "@/api/exports";
import { ROUTES } from "@/app/routes";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { date, dateHeure, montant } from "@/domain/format";

/**
 * Génération de rapports (F9) au périmètre retenu (D5) : un PDF fidèle au rapport d'origine (P10), uniquement
 * depuis un dossier validé, et l'historique des rapports générés (traces EXPORT_PDF du journal).
 */
export default function GenerationPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const historique = useQuery({ queryKey: ["rapports", "historique"], queryFn: () => historiqueRapports(20) });
  const [params, setParams] = useSearchParams();
  const numero = params.get("dossier") ?? "";
  const [saisie, setSaisie] = useState("");
  const [q, setQ] = useState("");
  const candidats = useQuery({ queryKey: cles.dossiers({ statut: "VALIDE", q }), queryFn: () => rechercherDossiers({ statut: "VALIDE", q }, 8), enabled: !numero });
  const choisi = useQuery({ queryKey: cles.dossier(numero), queryFn: () => dossier(numero), enabled: !!numero });

  useEffect(() => {
    const minuterie = setTimeout(() => setQ(saisie.trim()), 300);
    return () => clearTimeout(minuterie);
  }, [saisie]);

  const choisir = (n: string | null) => setParams(n ? { dossier: n } : {});
  const d = choisi.data;
  const vehicule = d?.vehicule ? [d.vehicule.marque, d.vehicule.modele].filter(Boolean).join(" ") : null;
  const valide = d?.statut === "VALIDE";

  return <main className="main generation-page">
    <PageHead title="Génération des rapports" subtitle="Produisez le rapport PDF d’un dossier validé" actions={<Button variant="secondary" icon="folder" onClick={() => navigate(ROUTES.dossiers)}>Consulter les dossiers</Button>} />
    <div className="generation-layout">
      <div className="generation-settings">
        <article className="card generation-card">
          <div className="generation-card-head"><span>1</span><div><h2>Dossier source</h2><p>Seul un dossier validé peut être généré</p></div><b>OBLIGATOIRE</b></div>
          {numero
            ? <>
              {choisi.isPending && <p className="etat-chargement">Chargement du dossier…</p>}
              {choisi.isError && <p className="import-erreur" role="alert">{messageErreur(choisi.error, "Dossier introuvable.")}</p>}
              {d && <div className="selected-folder"><span><Icon name="check" size={14} /></span><div><strong>Dossier {d.numeroRapport}</strong><small>{[d.assure?.nomPrenom, vehicule, d.vehicule?.immatriculation].filter(Boolean).join(" · ") || "—"}</small></div><StatusBadge statut={d.statut} /></div>}
              {d && !valide && <p className="import-erreur" role="alert">Ce dossier n’est pas validé : la génération n’est possible qu’après validation par un gestionnaire.</p>}
              <Button variant="quiet" onClick={() => choisir(null)}>Choisir un autre dossier</Button>
            </>
            : <>
              <label className="generation-field"><span>N° de rapport, assuré ou immatriculation</span><div><Icon name="search" size={16} /><input value={saisie} onChange={(e) => setSaisie(e.target.value)} placeholder="Rechercher un dossier validé…" /></div></label>
              {candidats.isError && <p className="import-erreur" role="alert">{messageErreur(candidats.error, "Recherche indisponible.")}</p>}
              {candidats.data && (candidats.data.content.length
                ? <ul className="generation-candidats">{candidats.data.content.map((c) => <li key={c.id}><button type="button" onClick={() => choisir(c.numeroRapport)}>
                  <strong>{c.numeroRapport}</strong><small>{[c.assure, c.immatriculation, c.compagnie].filter(Boolean).join(" · ")}</small><span>{montant(c.montantIndemnisation)}</span></button></li>)}</ul>
                : <p className="etat-chargement">Aucun dossier validé ne correspond.</p>)}
            </>}
        </article>
        <article className="card generation-card">
          <div className="generation-card-head"><span>2</span><div><h2>Document produit</h2><p>Rapport d’expertise au format PDF, fidèle au gabarit du cabinet (P10)</p></div></div>
          <p className="etat-chargement">Mêmes rubriques et même ordre que le rapport de l’expert, avec les données validées en base (corrections comprises). Chaque génération est tracée au journal.</p>
        </article>
        <div className="generation-actions">
          {d && <BoutonTelechargement variant="secondary" action={() => exporterDossier(d.numeroRapport)}>Exporter en Excel</BoutonTelechargement>}
          <BoutonTelechargement variant="primary" icon="file" disabled={!valide} action={() => telechargerRapportPdf(numero)}
            title={valide ? undefined : "Sélectionnez un dossier validé"} apres={() => void queryClient.invalidateQueries({ queryKey: ["rapports"] })}>Générer le rapport PDF</BoutonTelechargement>
        </div>
      </div>
      <aside className="report-preview card">
        <div className="preview-toolbar"><div><strong>Aperçu du rapport</strong><span>{d ? "Données du dossier" : "Sélectionnez un dossier"}</span></div>{d && <button aria-label="Ouvrir la fiche du dossier" onClick={() => navigate(ROUTES.dossier(d.numeroRapport))}><Icon name="eye" size={16} /></button>}</div>
        <div className="preview-canvas"><div className="report-paper">
          <header><img src="/logo-lt-expertise.png" alt="LT Expertise" /><div><span>{d?.cabinet ?? "Cabinet d’expertise automobile"}</span><small>Rapport confidentiel</small></div></header>
          <div className="report-paper-title"><small>RÉFÉRENCE {d?.numeroRapport ?? "—"}</small><h2>Rapport d’expertise</h2><p>{d?.dateRapport ? `Établi le ${date(d.dateRapport)}${d.lieuRapport ? ` à ${d.lieuRapport}` : ""}` : "—"}</p></div>
          <section><h3>Informations générales</h3><div className="paper-grid"><span>Assuré<strong>{d?.assure?.nomPrenom ?? "—"}</strong></span><span>Compagnie<strong>{d?.compagnie ?? "—"}</strong></span><span>Véhicule<strong>{vehicule || "—"}</strong></span><span>Immatriculation<strong>{d?.vehicule?.immatriculation ?? "—"}</strong></span></div></section>
          <section><h3>Évaluation des dommages</h3><div className="paper-table"><span>Désignation</span><span>Montant TTC</span><span>Main d’œuvre</span><b>{montant(d?.totalMainOeuvre?.ttc)}</b><span>Fournitures</span><b>{montant(d?.totalFournitures?.ttc)}</b></div></section>
          <div className="paper-total"><span>Montant d’indemnisation</span><strong>{montant(d?.montantIndemnisation)}</strong>{d?.montantLettres && <small>{d.montantLettres}</small>}</div>
          <footer><span>LT Expertise · Document généré automatiquement</span></footer>
        </div></div>
      </aside>
    </div>
    <article className="card generated-list"><div className="section-head"><div><h2>Rapports récemment générés</h2><p>D’après le journal d’audit (génération PDF)</p></div></div>
      {historique.isError && <p className="import-erreur" role="alert">{messageErreur(historique.error, "Historique indisponible.")}</p>}
      {historique.data?.length === 0 && <p className="etat-chargement">Aucun rapport généré pour l’instant.</p>}
      {!!historique.data?.length && <DataTable headers={["DOSSIER", "ASSURÉ", "COMPAGNIE", "GÉNÉRÉ LE", "PAR", "ACTIONS"]}>{historique.data.map((g, i) =>
        <tr key={`${g.numeroRapport}-${g.genereLe}-${i}`}><td><strong>{g.numeroRapport}</strong></td><td>{g.assure ?? "—"}</td><td>{g.compagnie ?? "—"}</td>
          <td>{dateHeure(g.genereLe)}</td><td>{g.generePar ?? "—"}</td>
          <td><div className="report-actions"><button onClick={() => navigate(ROUTES.dossier(g.numeroRapport))}>Consulter</button>
            <BoutonTelechargement variant="quiet" icon="download" action={() => telechargerRapportPdf(g.numeroRapport)}
              apres={() => void queryClient.invalidateQueries({ queryKey: ["rapports"] })}>Régénérer</BoutonTelechargement></div></td></tr>)}</DataTable>}
    </article>
  </main>;
}
