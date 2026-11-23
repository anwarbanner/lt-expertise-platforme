import { useEffect, useState, type ReactNode } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  cles, creerUtilisateur, journal, modifierUtilisateur, reinitialiserMotDePasse, utilisateurs,
} from "@/api/dossiers";
import { messageErreur } from "@/api/client";
import { exporterJournal } from "@/api/exports";
import { BoutonTelechargement } from "@/components/ui/BoutonTelechargement";
import { Button } from "@/components/ui/Button";
import { DataTable } from "@/components/ui/DataTable";
import { Icon } from "@/components/ui/Icon";
import { PageHead } from "@/components/ui/PageHead";
import type { CompteUtilisateur, EntreeJournal } from "@/domain/dossiers";
import { dateHeure } from "@/domain/format";
import { LIBELLES_ROLE, type Role } from "@/domain/referentiels";
import { useUtilisateur } from "@/features/auth/AuthContext";
import { libelleAction } from "@/features/dossiers/libelles";

const ROLES: readonly (readonly [Role, string])[] = [
  ["ADMINISTRATEUR", "Accès complet, comptes utilisateurs et journal d’audit"],
  ["GESTIONNAIRE", "Import, vérification, modification et validation"],
  ["CONSULTATION", "Consultation et export uniquement"],
];

const ACTIONS_AUDIT = ["CONNEXION", "ECHEC_CONNEXION", "IMPORT", "IMPORT_REFUSE", "CREATION", "MODIFICATION",
  "CHANGEMENT_STATUT", "VALIDATION", "ARCHIVAGE", "ANNULATION", "EXPORT_PDF", "EXPORT_EXCEL", "GESTION_UTILISATEUR"] as const;

/** Catégorie affichée (classe CSS, libellé) d'une action du journal. */
function categorie(action: string): readonly [string, string] {
  if (action === "CONNEXION") return ["connexion", "Connexion"];
  if (action === "ECHEC_CONNEXION" || action === "GESTION_UTILISATEUR") return ["securite", "Sécurité"];
  if (action.startsWith("EXPORT")) return ["export", "Export"];
  if (action === "IMPORT" || action === "IMPORT_REFUSE" || action === "CREATION") return ["creation", "Création"];
  return ["modification", "Modification"];
}

function detail(e: EntreeJournal): string {
  const morceaux: string[] = [];
  if (e.numeroRapport) morceaux.push(`Dossier ${e.numeroRapport}`);
  if (e.champ) morceaux.push(`${e.champ} : ${e.ancienneValeur ?? "—"} → ${e.nouvelleValeur ?? "—"}`);
  else if (e.details?.motif) morceaux.push([e.details.nomFichier, e.details.motif].filter(Boolean).map(String).join(" : "));
  else if (e.details) {
    // L'empreinte SHA-256 n'aide pas la lecture ; elle reste dans la base
    const lisibles = Object.entries(e.details).filter(([k]) => k !== "hashSha256" && k !== "issue");
    morceaux.push(lisibles.map(([k, v]) => `${k} : ${typeof v === "object" ? JSON.stringify(v) : String(v)}`).join(" · "));
  }
  return morceaux.join(" — ") || "—";
}

const initiales = (texte: string) => texte.split(/\s+/).filter(Boolean).map((m) => m[0]!.toUpperCase()).slice(0, 2).join("");

/** Comptes et rôles (F11, P9) et journal d'audit (P7). Réservé à l'administrateur. */
export default function UtilisateursPage() {
  const [vue, setVue] = useState<"comptes" | "journal">("comptes");
  const [panneau, setPanneau] = useState<{ mode: "creation" } | { mode: "edition" | "motDePasse"; compte: CompteUtilisateur } | null>(null);
  const comptes = useQuery({ queryKey: cles.utilisateurs, queryFn: utilisateurs });
  const [saisieJournal, setSaisieJournal] = useState("");
  const [filtresJournal, setFiltresJournal] = useState({ q: "", action: "", page: 0 });
  const evenements = useQuery({ queryKey: cles.journal(filtresJournal), queryFn: () => journal(filtresJournal), placeholderData: keepPreviousData });

  useEffect(() => {
    const minuterie = setTimeout(() => setFiltresJournal((f) => f.q === saisieJournal.trim() ? f : { ...f, q: saisieJournal.trim(), page: 0 }), 300);
    return () => clearTimeout(minuterie);
  }, [saisieJournal]);

  return <main className="main users-page"><PageHead title="Utilisateurs" subtitle="Administration des accès, des rôles et de la traçabilité" actions={vue === "comptes"
    ? <Button icon="plus" onClick={() => setPanneau({ mode: "creation" })}>Ajouter un utilisateur</Button>
    : <BoutonTelechargement action={() => exporterJournal({ q: filtresJournal.q, action: filtresJournal.action })} title="Entrées répondant aux filtres, les plus récentes d’abord">Exporter le journal</BoutonTelechargement>} />
    <div className="users-tabs" role="tablist" aria-label="Administration des utilisateurs">
      <button className={vue === "comptes" ? "active" : ""} onClick={() => setVue("comptes")} role="tab" aria-selected={vue === "comptes"}>Comptes utilisateurs <b>{comptes.data?.length ?? "…"}</b></button>
      <button className={vue === "journal" ? "active" : ""} onClick={() => setVue("journal")} role="tab" aria-selected={vue === "journal"}>Journal d’audit <b>{evenements.data?.page.totalElements ?? "…"}</b></button>
    </div>
    {vue === "comptes"
      ? <Comptes requete={comptes} ouvrir={setPanneau} />
      : <>
        <article className="card audit-filters">
          <div className="audit-search"><Icon name="search" size={16} /><input aria-label="Rechercher dans le journal" value={saisieJournal} onChange={(e) => setSaisieJournal(e.target.value)} placeholder="Rechercher un utilisateur, un dossier ou une adresse IP…" /></div>
          <label className="filter"><span>Type d’action</span><select aria-label="Type d’action" value={filtresJournal.action} onChange={(e) => setFiltresJournal((f) => ({ ...f, action: e.target.value, page: 0 }))}>
            <option value="">Toutes les actions</option>{ACTIONS_AUDIT.map((a) => <option key={a} value={a}>{libelleAction(a)}</option>)}</select></label>
          <Button variant="quiet" onClick={() => { setSaisieJournal(""); setFiltresJournal({ q: "", action: "", page: 0 }); }}>Réinitialiser</Button>
        </article>
        <article className="card audit-card"><div className="section-head"><div><h2>Activité des utilisateurs</h2><p>Traçabilité des connexions, des modifications et des actions d’administration</p></div></div>
          {evenements.isPending && <p className="etat-chargement">Chargement du journal…</p>}
          {evenements.isError && <p className="import-erreur" role="alert">{messageErreur(evenements.error, "Journal indisponible.")}</p>}
          {evenements.data && <>
            <DataTable headers={["DATE ET HEURE", "UTILISATEUR", "ACTION", "DÉTAIL", "CATÉGORIE", "ADRESSE IP"]}>{evenements.data.content.map((e) => {
              const [classe, libelleCategorie] = categorie(e.action);
              return <tr key={e.id}>
                <td><strong>{dateHeure(e.horodatage)}</strong></td>
                <td><div className="user-cell"><span>{initiales(e.utilisateur ?? "?")}</span><strong>{e.utilisateur ?? "—"}</strong></div></td>
                <td><strong className="audit-action">{libelleAction(e.action)}</strong></td>
                <td>{detail(e)}</td>
                <td><span className={`audit-category ${classe}`}>{libelleCategorie}</span></td>
                <td>{e.adresseIp ? <code className="audit-ip">{e.adresseIp}</code> : "—"}</td>
              </tr>;
            })}</DataTable>
            {evenements.data.content.length === 0 && <div className="audit-empty"><Icon name="search" size={20} /><strong>Aucun événement trouvé</strong><span>Modifiez votre recherche pour afficher d’autres activités.</span></div>}
            <Pagination page={evenements.data.page} changer={(page) => setFiltresJournal((f) => ({ ...f, page }))} />
          </>}
        </article>
      </>}
    {panneau && (panneau.mode === "motDePasse"
      ? <PanneauMotDePasse compte={panneau.compte} fermer={() => setPanneau(null)} />
      : <PanneauCompte compte={panneau.mode === "edition" ? panneau.compte : null} fermer={() => setPanneau(null)} />)}
  </main>;
}

function Pagination({ page, changer }: { page: { number: number; size: number; totalElements: number; totalPages: number }; changer: (page: number) => void }) {
  const debut = page.totalElements ? page.number * page.size + 1 : 0;
  const fin = Math.min((page.number + 1) * page.size, page.totalElements);
  return <div className="pagination"><span>{debut}–{fin} sur {page.totalElements} événements</span><div>
    <button aria-label="Page précédente" disabled={page.number === 0} onClick={() => changer(page.number - 1)}>‹</button>
    <button className="active">{page.number + 1}</button>
    <button aria-label="Page suivante" disabled={page.number + 1 >= page.totalPages} onClick={() => changer(page.number + 1)}>›</button>
  </div></div>;
}

function Comptes({ requete, ouvrir }: {
  requete: ReturnType<typeof useQuery<CompteUtilisateur[]>>;
  ouvrir: (panneau: { mode: "edition" | "motDePasse"; compte: CompteUtilisateur }) => void;
}) {
  const moi = useUtilisateur();
  const queryClient = useQueryClient();
  const [recherche, setRecherche] = useState("");
  const activation = useMutation({
    mutationFn: (u: CompteUtilisateur) => modifierUtilisateur(u.id, { nom: u.nom, prenom: u.prenom, email: u.email, role: u.role, actif: !u.actif }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cles.utilisateurs });
      void queryClient.invalidateQueries({ queryKey: ["journal"] });
    },
  });

  if (requete.isPending) return <article className="card users-card"><p className="etat-chargement">Chargement des comptes…</p></article>;
  if (requete.isError) return <article className="card users-card"><p className="import-erreur" role="alert">{messageErreur(requete.error, "Comptes indisponibles.")}</p></article>;

  const liste = requete.data;
  const filtre = recherche.trim().toLowerCase();
  const visibles = liste.filter((u) => `${u.prenom} ${u.nom} ${u.identifiant} ${u.email ?? ""}`.toLowerCase().includes(filtre));
  const actifs = liste.filter((u) => u.actif).length;
  const verrouilles = liste.filter((u) => u.verrouille).length;

  return <article className="card users-card"><div className="section-head"><div><h2>Comptes utilisateurs</h2>
    <p>{liste.length} utilisateur{liste.length > 1 ? "s" : ""} · {actifs} actif{actifs > 1 ? "s" : ""} · {verrouilles} verrouillé{verrouilles > 1 ? "s" : ""}</p></div>
    <div className="small-search"><Icon name="search" size={15} /><input aria-label="Rechercher un utilisateur" value={recherche} onChange={(e) => setRecherche(e.target.value)} placeholder="Rechercher un utilisateur…" /></div></div>
    {activation.isError && <p className="import-erreur" role="alert">{messageErreur(activation.error, "Modification refusée.")}</p>}
    <DataTable headers={["NOM", "IDENTIFIANT", "RÔLE", "STATUT", "DERNIÈRE CONNEXION", "ACTIONS"]}>{visibles.map((u) => {
      const statut = !u.actif ? "Désactivé" : u.verrouille ? "Verrouillé" : "Actif";
      return <tr key={u.id}>
        <td><div className="user-cell"><span>{initiales(`${u.prenom} ${u.nom}`)}</span><strong>{u.prenom} {u.nom}</strong></div></td>
        <td>{u.identifiant}</td>
        <td><span className={`role ${u.role.toLowerCase()}`}>{LIBELLES_ROLE[u.role]}</span></td>
        <td><span className={`account-status ${statut === "Actif" ? "active" : "locked"}`}><i />{statut}</span></td>
        <td>{u.derniereConnexion ? dateHeure(u.derniereConnexion) : "Jamais"}</td>
        <td><div className="user-actions">
          <button onClick={() => ouvrir({ mode: "edition", compte: u })}>Modifier</button>
          <button onClick={() => ouvrir({ mode: "motDePasse", compte: u })}>Réinitialiser le mot de passe</button>
          {u.identifiant !== moi.identifiant && <button className="danger" disabled={activation.isPending} onClick={() => activation.mutate(u)}>{u.actif ? "Désactiver" : "Réactiver"}</button>}
        </div></td>
      </tr>;
    })}</DataTable>
    {visibles.length === 0 && <div className="audit-empty"><Icon name="search" size={20} /><strong>Aucun utilisateur trouvé</strong></div>}
  </article>;
}

function Champ({ label, erreur, children }: { label: string; erreur?: string; children: ReactNode }) {
  return <label className={`data-field ${erreur ? "error" : ""}`}><span>{label}</span>{children}{erreur && <small>{erreur}</small>}</label>;
}

function Panneau({ titre, sousTitre, fermer, children, actions }: { titre: string; sousTitre: string; fermer: () => void; children: ReactNode; actions: ReactNode }) {
  return <><button className="drawer-overlay" onClick={fermer} aria-label="Fermer" />
    <aside className="user-drawer" role="dialog" aria-label={titre}><div className="drawer-head"><div><h2>{titre}</h2><p>{sousTitre}</p></div><button onClick={fermer} aria-label="Fermer le panneau">×</button></div>
      {children}<div className="drawer-actions">{actions}</div></aside></>;
}

const IDENTIFIANT = /^[a-zA-Z0-9._-]{3,50}$/;

const schemaCompte = (creation: boolean) => z.object({
  prenom: z.string().trim().min(1, "Saisissez le prénom.").max(80),
  nom: z.string().trim().min(1, "Saisissez le nom.").max(80),
  identifiant: z.string().trim(),
  email: z.union([z.literal(""), z.string().trim().max(150).email("Adresse e-mail invalide.")]),
  role: z.enum(["ADMINISTRATEUR", "GESTIONNAIRE", "CONSULTATION"]),
  motDePasse: z.string(),
}).superRefine((v, ctx) => {
  if (!creation) return;
  if (!IDENTIFIANT.test(v.identifiant)) ctx.addIssue({ code: "custom", path: ["identifiant"], message: "3 à 50 caractères : lettres, chiffres, point, tiret." });
  if (v.motDePasse.length < 10) ctx.addIssue({ code: "custom", path: ["motDePasse"], message: "10 caractères au moins." });
});
type FormulaireCompte = z.infer<ReturnType<typeof schemaCompte>>;

/** Création d'un compte, ou modification (nom, e-mail, rôle) d'un compte existant. */
function PanneauCompte({ compte, fermer }: { compte: CompteUtilisateur | null; fermer: () => void }) {
  const queryClient = useQueryClient();
  const creation = compte === null;
  const { register, handleSubmit, watch, formState: { errors } } = useForm<FormulaireCompte>({
    resolver: zodResolver(schemaCompte(creation)),
    defaultValues: { prenom: compte?.prenom ?? "", nom: compte?.nom ?? "", identifiant: compte?.identifiant ?? "", email: compte?.email ?? "", role: compte?.role ?? "GESTIONNAIRE", motDePasse: "" },
  });
  const enregistrement = useMutation({
    mutationFn: (v: FormulaireCompte) => {
      const email = v.email || null;
      return compte
        ? modifierUtilisateur(compte.id, { nom: v.nom, prenom: v.prenom, email, role: v.role, actif: compte.actif })
        : creerUtilisateur({ identifiant: v.identifiant, nom: v.nom, prenom: v.prenom, email, role: v.role, motDePasse: v.motDePasse });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cles.utilisateurs });
      void queryClient.invalidateQueries({ queryKey: ["journal"] });
      fermer();
    },
  });
  const role = watch("role");

  return <Panneau titre={creation ? "Ajouter un utilisateur" : "Modifier l’utilisateur"} sousTitre={creation ? "Créez un nouvel accès à la plateforme" : compte.identifiant} fermer={fermer}
    actions={<><Button variant="secondary" onClick={fermer}>Annuler</Button><Button type="submit" form="formulaire-compte" disabled={enregistrement.isPending}>{creation ? "Créer l’utilisateur" : "Enregistrer"}</Button></>}>
    <form id="formulaire-compte" className="drawer-body" noValidate onSubmit={handleSubmit((v) => enregistrement.mutate(v))}>
      {enregistrement.isError && <p className="import-erreur" role="alert">{messageErreur(enregistrement.error, "Enregistrement refusé.")}</p>}
      <Champ label="Prénom" erreur={errors.prenom?.message}><input {...register("prenom")} /></Champ>
      <Champ label="Nom" erreur={errors.nom?.message}><input {...register("nom")} /></Champ>
      <Champ label="Identifiant" erreur={errors.identifiant?.message}><input {...register("identifiant")} readOnly={!creation} autoComplete="off" /></Champ>
      <Champ label="Adresse e-mail" erreur={errors.email?.message}><input type="email" {...register("email")} /></Champ>
      {creation && <Champ label="Mot de passe initial" erreur={errors.motDePasse?.message}><input type="password" autoComplete="new-password" {...register("motDePasse")} /></Champ>}
      <h3>Rôle et droits</h3>
      {ROLES.map(([r, description]) => <label className={`role-choice ${role === r ? "selected" : ""}`} key={r}><input type="radio" value={r} {...register("role")} /><div><strong>{LIBELLES_ROLE[r]}</strong><span>{description}</span></div></label>)}
    </form>
  </Panneau>;
}

const schemaMotDePasse = z.object({
  motDePasse: z.string().min(10, "10 caractères au moins.").max(200),
  confirmation: z.string(),
}).refine((v) => v.motDePasse === v.confirmation, { path: ["confirmation"], message: "Les deux mots de passe diffèrent." });
type FormulaireMotDePasse = z.infer<typeof schemaMotDePasse>;

/** Nouveau mot de passe choisi par l'administrateur (pas d'e-mail, P11) : déverrouille le compte et ferme ses sessions. */
function PanneauMotDePasse({ compte, fermer }: { compte: CompteUtilisateur; fermer: () => void }) {
  const queryClient = useQueryClient();
  const { register, handleSubmit, formState: { errors } } = useForm<FormulaireMotDePasse>({ resolver: zodResolver(schemaMotDePasse), defaultValues: { motDePasse: "", confirmation: "" } });
  const reinitialisation = useMutation({
    mutationFn: (v: FormulaireMotDePasse) => reinitialiserMotDePasse(compte.id, v.motDePasse),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: cles.utilisateurs });
      void queryClient.invalidateQueries({ queryKey: ["journal"] });
      fermer();
    },
  });
  return <Panneau titre="Réinitialiser le mot de passe" sousTitre={`${compte.prenom} ${compte.nom} (${compte.identifiant})`} fermer={fermer}
    actions={<><Button variant="secondary" onClick={fermer}>Annuler</Button><Button type="submit" form="formulaire-mot-de-passe" disabled={reinitialisation.isPending}>Réinitialiser</Button></>}>
    <form id="formulaire-mot-de-passe" className="drawer-body" noValidate onSubmit={handleSubmit((v) => reinitialisation.mutate(v))}>
      {reinitialisation.isError && <p className="import-erreur" role="alert">{messageErreur(reinitialisation.error, "Réinitialisation refusée.")}</p>}
      <p>Le compte sera déverrouillé et ses sessions ouvertes fermées. Communiquez le nouveau mot de passe à l’utilisateur.</p>
      <Champ label="Nouveau mot de passe" erreur={errors.motDePasse?.message}><input type="password" autoComplete="new-password" {...register("motDePasse")} /></Champ>
      <Champ label="Confirmation" erreur={errors.confirmation?.message}><input type="password" autoComplete="new-password" {...register("confirmation")} /></Champ>
    </form>
  </Panneau>;
}
