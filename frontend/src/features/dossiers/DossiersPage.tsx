import { pagesAffichees } from "@/components/ui/Pagination";
import { useEffect, useState } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router";
import { cles, compagnies, compteursDossiers, rechercherDossiers, validerEnLot, type FiltresDossiers } from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterListe } from "@/api/exports";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { adresseDossier, peutAcceder } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { date, montant, nombre } from "@/domain/format";
import { LIBELLES_STATUT, ZONES_CHOC, type StatutDossier } from "@/domain/referentiels";
import { useUtilisateur } from "@/features/auth/AuthContext";

const TRANCHES: Record<string, [string, string, string]> = {
  "moins-5000": ["Moins de 5 000 DH", "", "4999.99"],
  "5000-20000": ["5 000 à 20 000 DH", "5000", "20000"],
  "20000-100000": ["20 000 à 100 000 DH", "20000", "100000"],
  "plus-100000": ["Plus de 100 000 DH", "100000", ""],
};
const ONGLETS: [string, StatutDossier | ""][] = [["Tous", ""], ["À compléter", "A_COMPLETER"], ["À vérifier", "A_VERIFIER"], ["Prêts à valider", "PRET_A_VALIDER"], ["Validés", "VALIDE"], ["Archivés", "ARCHIVE"], ["Annulés", "ANNULE"]];
const FILTRES = ["q", "statut", "compagnie", "zone", "du", "au", "tranche", "anomalies"] as const;

/** Liste, recherche et filtres des dossiers (F7, F8). Les filtres vivent dans l'adresse (partageable, retour arrière). */
export default function DossiersPage() {
  const navigate = useNavigate();
  const { role } = useUtilisateur();
  const [params, setParams] = useSearchParams();
  const [saisie, setSaisie] = useState(params.get("q") ?? "");
  // Recherche venue d'ailleurs (barre du haut) alors que la page est déjà affichée
  const qUrl = params.get("q") ?? "";
  const [qVu, setQVu] = useState(qUrl);
  if (qUrl !== qVu) {
    setQVu(qUrl);
    if (qUrl !== saisie.trim()) setSaisie(qUrl);
  }
  const tranche = TRANCHES[params.get("tranche") ?? ""];
  const filtres: FiltresDossiers = {
    q: params.get("q") ?? undefined, statut: params.get("statut") ?? undefined, compagnie: params.get("compagnie") ?? undefined,
    zone: params.get("zone") ?? undefined, du: params.get("du") ?? undefined, au: params.get("au") ?? undefined,
    montantMin: tranche?.[1], montantMax: tranche?.[2], anomalies: params.get("anomalies") ?? undefined,
    page: Number(params.get("page") ?? 0), sort: params.get("sort") ?? undefined,
  };
  const liste = useQuery({ queryKey: cles.dossiers(filtres), queryFn: () => rechercherDossiers(filtres), placeholderData: keepPreviousData });
  const compteurs = useQuery({ queryKey: cles.compteurs, queryFn: compteursDossiers });
  const listeCompagnies = useQuery({ queryKey: cles.compagnies, queryFn: compagnies });

  // Validation en lot (gestionnaire) : dossiers cochés, ou tous les prêts à valider répondant aux filtres
  const gestionnaire = peutAcceder(role, "verification");
  const queryClient = useQueryClient();
  const [selection, setSelection] = useState<Set<string>>(new Set());
  const [confirmation, setConfirmation] = useState<"selection" | "tous" | null>(null);
  const filtresLot = { q: filtres.q, compagnie: filtres.compagnie, zone: filtres.zone, du: filtres.du, au: filtres.au, montantMin: filtres.montantMin, montantMax: filtres.montantMax };
  const prets = useQuery({ queryKey: ["dossiers", "prets", filtresLot], queryFn: () => rechercherDossiers({ ...filtresLot, statut: "PRET_A_VALIDER" }, 1), enabled: gestionnaire });
  const lot = useMutation({
    mutationFn: (mode: "selection" | "tous") => validerEnLot(mode === "selection" ? { numeros: [...selection] } : { tousLesPrets: true, ...filtresLot }),
    onSuccess: () => {
      setSelection(new Set());
      setConfirmation(null);
      void queryClient.invalidateQueries({ queryKey: ["dossiers"] });
      void queryClient.invalidateQueries({ queryKey: ["statistiques"] });
    },
  });
  const totalPrets = prets.data?.page.totalElements ?? 0;
  const nombreLot = confirmation === "selection" ? selection.size : totalPrets;
  const pretsDeLaPage = liste.data?.content.filter((d) => d.statut === "PRET_A_VALIDER").map((d) => d.numeroRapport) ?? [];
  const toutCoche = pretsDeLaPage.length > 0 && pretsDeLaPage.every((n) => selection.has(n));
  const basculer = (numeros: string[], cocher: boolean) => setSelection((s) => {
    const suivante = new Set(s);
    numeros.forEach((n) => (cocher ? suivante.add(n) : suivante.delete(n)));
    return suivante;
  });

  // Recherche lancée 300 ms après la dernière frappe
  useEffect(() => {
    const minuterie = setTimeout(() => { if ((params.get("q") ?? "") !== saisie.trim()) changer({ q: saisie.trim() }); }, 300);
    return () => clearTimeout(minuterie);
  }, [saisie]); // eslint-disable-line react-hooks/exhaustive-deps

  function changer(valeurs: Record<string, string>) {
    const suivant = new URLSearchParams(params);
    Object.entries(valeurs).forEach(([cle, valeur]) => (valeur ? suivant.set(cle, valeur) : suivant.delete(cle)));
    if (!("page" in valeurs)) suivant.delete("page");
    setParams(suivant);
  }

  function trier(colonne: string) {
    const [actuelle, sens] = (params.get("sort") ?? "").split(",");
    changer({ sort: `${colonne},${actuelle === colonne && sens === "desc" ? "asc" : "desc"}`, page: params.get("page") ?? "" });
  }

  function enTete(libelle: string, colonne: string) {
    const [actuelle, sens] = (params.get("sort") ?? "").split(",");
    return <button className="tri" onClick={() => trier(colonne)}>{libelle} {actuelle === colonne ? (sens === "asc" ? "↑" : "↓") : "↕"}</button>;
  }

  // « Tous » n'inclut pas les imports annulés, comme la liste sans filtre de statut
  const total = Object.entries(compteurs.data ?? {}).reduce((t, [s, n]) => (s === "ANNULE" ? t : t + (n ?? 0)), 0);
  const page = liste.data?.page;
  const filtresActifs = FILTRES.some((f) => params.get(f));

  return <main className="main folders-page">
    <PageHead title="Dossiers" subtitle="Consultez et gérez l’ensemble des rapports d’expertise" actions={<BoutonTelechargement action={() => exporterListe(filtres)} title="Toute la liste répondant aux filtres, pas seulement la page affichée">Exporter en Excel</BoutonTelechargement>} />
    <article className="card filter-card">
      <div className="filter-search"><Icon name="search" size={16} /><input aria-label="Recherche" value={saisie} onChange={(e) => setSaisie(e.target.value)} placeholder="N° rapport, réf. sinistre, immatriculation, nom, réparateur…" /></div>
      <label className="filter"><span>Du</span><input type="date" value={params.get("du") ?? ""} onChange={(e) => changer({ du: e.target.value })} /></label>
      <label className="filter"><span>Au</span><input type="date" value={params.get("au") ?? ""} onChange={(e) => changer({ au: e.target.value })} /></label>
      <label className="filter"><span>Compagnie</span><select value={params.get("compagnie") ?? ""} onChange={(e) => changer({ compagnie: e.target.value })}>
        <option value="">Toutes</option>{(listeCompagnies.data ?? []).map((c) => <option key={c}>{c}</option>)}</select></label>
      <label className="filter"><span>Statut</span><select value={params.get("statut") ?? ""} onChange={(e) => changer({ statut: e.target.value })}>
        <option value="">Tous les statuts</option>{(Object.keys(LIBELLES_STATUT) as (keyof typeof LIBELLES_STATUT)[]).filter((s) => s !== "REFUSE").map((s) => <option key={s} value={s}>{LIBELLES_STATUT[s]}</option>)}</select></label>
      <label className="filter"><span>Zone de choc</span><select value={params.get("zone") ?? ""} onChange={(e) => changer({ zone: e.target.value })}>
        <option value="">Toutes</option>{Object.entries(ZONES_CHOC).map(([code, libelle]) => <option key={code} value={code}>{code} · {libelle}</option>)}</select></label>
      <label className="filter"><span>Montant</span><select value={params.get("tranche") ?? ""} onChange={(e) => changer({ tranche: e.target.value })}>
        <option value="">Tous</option>{Object.entries(TRANCHES).map(([cle, [libelle]]) => <option key={cle} value={cle}>{libelle}</option>)}</select></label>
      <Button variant="quiet" disabled={!filtresActifs} onClick={() => { setSaisie(""); setParams(new URLSearchParams()); }}>Réinitialiser</Button>
    </article>
    <div className="quick-tabs">{ONGLETS.map(([libelle, statut]) =>
      <button key={libelle} className={(params.get("statut") ?? "") === statut && !params.get("anomalies") ? "active" : ""} onClick={() => changer({ statut, anomalies: "" })}>
        {libelle}<b>{statut ? (compteurs.data?.[statut] ?? 0) : total}</b></button>)}
      {params.get("anomalies") && <button className="active" onClick={() => changer({ anomalies: "" })}>En anomalie ×</button>}
    </div>
    {gestionnaire && <article className="card bulk-bar">
      {confirmation
        ? <span className="bulk-confirm">Valider {nombre(nombreLot)} dossier(s) ? Chaque validation est tracée à votre nom.
          <Button onClick={() => lot.mutate(confirmation)} disabled={lot.isPending || nombreLot === 0}>{lot.isPending ? "Validation…" : "Confirmer"}</Button>
          <Button variant="secondary" onClick={() => setConfirmation(null)}>Annuler</Button></span>
        : <><span>{selection.size} dossier(s) sélectionné(s)</span>
          <Button disabled={selection.size === 0} onClick={() => setConfirmation("selection")}>Valider la sélection</Button>
          <Button variant="secondary" disabled={totalPrets === 0} onClick={() => setConfirmation("tous")}>
            Valider tous les prêts à valider{filtresActifs ? " (selon les filtres)" : ""} ({nombre(totalPrets)})</Button></>}
    </article>}
    {lot.isError && <p className="import-erreur" role="alert">{messageErreur(lot.error, "Validation en lot impossible.")}</p>}
    {lot.data && <article className="card bulk-resultat" role="status"><strong>{lot.data.valides} dossier(s) validé(s)</strong>{lot.data.paquets > 1 ? ` en ${lot.data.paquets} paquets de 500` : ""}
      {lot.data.ignores.length > 0 && <> · {lot.data.ignores.length} ignoré(s)<ul>{lot.data.ignores.slice(0, 10).map((i) => <li key={i.numeroRapport}>{i.numeroRapport} : {i.motif}</li>)}</ul></>}</article>}
    <article className="card folder-table">
      {liste.isError && <p className="import-erreur" role="alert">{messageErreur(liste.error, "Liste indisponible.")}</p>}
      {liste.isPending && <p className="etat-chargement">Chargement…</p>}
      {liste.data?.content.length === 0 && <p className="etat-chargement">Aucun dossier ne correspond à ces critères.</p>}
      {liste.data && liste.data.content.length > 0 &&
        <div className="table-scroll"><table><thead><tr>
          {gestionnaire && <th className="coche"><input type="checkbox" aria-label="Cocher les dossiers prêts de la page" checked={toutCoche}
            disabled={pretsDeLaPage.length === 0} onChange={() => basculer(pretsDeLaPage, !toutCoche)} /></th>}
          <th>{enTete("N° RAPPORT", "numeroRapport")}</th><th>{enTete("DATE SINISTRE", "dateSinistre")}</th><th>ASSURÉ</th><th>IMMATRICULATION</th>
          <th>COMPAGNIE</th><th>ZONE DE CHOC</th><th>{enTete("INDEMNISATION (DH)", "montantIndemnisation")}</th><th>STATUT</th><th></th>
        </tr></thead><tbody>{liste.data.content.map((d) =>
          <tr key={d.id} onDoubleClick={() => navigate(adresseDossier(d.numeroRapport, d.statut, role, d.id))}>
            {gestionnaire && <td className="coche"><input type="checkbox" aria-label={`Cocher ${d.numeroRapport}`} disabled={d.statut !== "PRET_A_VALIDER"}
              title={d.statut !== "PRET_A_VALIDER" ? "Seul un dossier prêt à valider peut être validé" : undefined}
              checked={selection.has(d.numeroRapport)} onChange={(e) => basculer([d.numeroRapport], e.target.checked)} /></td>}
            <td><strong>{d.numeroRapport}</strong></td><td>{date(d.dateSinistre)}</td><td>{d.assure ?? "—"}</td>
            <td>{d.immatriculation ? <span className="plate">{d.immatriculation}</span> : "—"}</td><td>{d.compagnie ?? "—"}</td>
            <td>{d.zonesChoc.map((z) => ZONES_CHOC[z]).join(", ") || "—"}</td><td className="money">{montant(d.montantIndemnisation, "")}</td>
            <td><StatusBadge statut={d.statut} />{d.anomalies > 0 && <span className="anomalies" title={`${d.anomalies} contrôle(s) ou champ(s) à revoir`}>⚠ {d.anomalies}</span>}</td>
            <td><div className="action-menu"><Button variant="quiet" onClick={() => navigate(adresseDossier(d.numeroRapport, d.statut, role, d.id))}>Ouvrir</Button></div></td>
          </tr>)}</tbody></table></div>}
      {page && page.totalElements > 0 && <div className="pagination">
        <span>{nombre(page.number * page.size + 1)}–{nombre(Math.min((page.number + 1) * page.size, page.totalElements))} sur {nombre(page.totalElements)}</span>
        <div>
          <button disabled={page.number === 0} onClick={() => changer({ page: String(page.number - 1) })} aria-label="Page précédente">‹</button>
          {pagesAffichees(page.number, page.totalPages).map((p, i) => p === null ? <span key={`e${i}`}>…</span>
            : <button key={p} className={p === page.number ? "active" : ""} onClick={() => changer({ page: String(p) })}>{p + 1}</button>)}
          <button disabled={page.number + 1 >= page.totalPages} onClick={() => changer({ page: String(page.number + 1) })} aria-label="Page suivante">›</button>
        </div>
      </div>}
    </article>
  </main>;
}
