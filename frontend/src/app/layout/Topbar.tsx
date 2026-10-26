import { useEffect, useRef, useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router";
import { cles, compteursDossiers } from "@/api/dossiers";
import { Icon } from "@/components/ui/Icon";
import { ROUTES } from "@/app/routes";
import { LIBELLES_ROLE, LIBELLES_STATUT, type StatutDossier } from "@/domain/referentiels";
import { useAuth, useUtilisateur } from "@/features/auth/AuthContext";

/** Statuts qui demandent une action, signalés par la cloche. */
const STATUTS_A_TRAITER: StatutDossier[] = ["A_VERIFIER", "A_COMPLETER", "PRET_A_VALIDER"];

export function Topbar() {
  const navigate = useNavigate();
  const { deconnecter } = useAuth();
  const utilisateur = useUtilisateur();
  const [menuOuvert, setMenuOuvert] = useState(false);
  const [alertesOuvertes, setAlertesOuvertes] = useState(false);
  const [recherche, setRecherche] = useState("");
  const champRecherche = useRef<HTMLInputElement>(null);
  const compteurs = useQuery({ queryKey: cles.compteurs, queryFn: compteursDossiers, refetchInterval: 60_000 });
  const aTraiter = STATUTS_A_TRAITER.map((s) => [s, compteurs.data?.[s] ?? 0] as const).filter(([, n]) => n > 0);
  const totalATraiter = aTraiter.reduce((t, [, n]) => t + n, 0);
  const initiales = `${utilisateur.prenom.charAt(0)}${utilisateur.nom.charAt(0)}`.toUpperCase();

  // ⌘K / Ctrl+K place le curseur dans la recherche globale
  useEffect(() => {
    const raccourci = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        champRecherche.current?.focus();
      }
    };
    window.addEventListener("keydown", raccourci);
    return () => window.removeEventListener("keydown", raccourci);
  }, []);

  function rechercher(e: FormEvent) {
    e.preventDefault();
    const q = recherche.trim();
    navigate(q ? `${ROUTES.dossiers}?q=${encodeURIComponent(q)}` : ROUTES.dossiers);
    setRecherche("");
    champRecherche.current?.blur();
  }

  async function seDeconnecter() {
    setMenuOuvert(false);
    await deconnecter();
    navigate(ROUTES.connexion, { replace: true });
  }

  return <header className="topbar">
    <form className="search" role="search" onSubmit={rechercher}><Icon name="search" size={18} /><input ref={champRecherche} aria-label="Recherche globale" value={recherche} onChange={(e) => setRecherche(e.target.value)} placeholder="Rechercher un rapport, une immatriculation, un nom…" /><kbd>⌘ K</kbd></form>
    <div className="user-menu notification-menu">
      <button className="notification" aria-label={`Notifications : ${totalATraiter} dossier${totalATraiter > 1 ? "s" : ""} à traiter`} aria-expanded={alertesOuvertes} onClick={() => { setAlertesOuvertes(!alertesOuvertes); setMenuOuvert(false); }}>
        <Icon name="bell" size={20} />{totalATraiter > 0 && <i />}</button>
      {alertesOuvertes && <div className="user-menu-panel" role="menu">
        {aTraiter.length
          ? aTraiter.map(([s, n]) => <button role="menuitem" key={s} onClick={() => { setAlertesOuvertes(false); navigate(`${ROUTES.dossiers}?statut=${s}`); }}><Icon name="folder" size={15} />{n} dossier{n > 1 ? "s" : ""} · {LIBELLES_STATUT[s]}</button>)
          : <span className="user-menu-identifiant">Aucun dossier à traiter</span>}
      </div>}
    </div>
    <div className="user-menu">
      <button className="user" onClick={() => { setMenuOuvert(!menuOuvert); setAlertesOuvertes(false); }} aria-haspopup="menu" aria-expanded={menuOuvert}>
        <span className="avatar">{initiales}</span>
        <div><strong>{utilisateur.prenom} {utilisateur.nom}</strong><small>{LIBELLES_ROLE[utilisateur.role]}</small></div>
        <Icon name="chevron" size={15} />
      </button>
      {menuOuvert && <div className="user-menu-panel" role="menu">
        <span className="user-menu-identifiant">{utilisateur.identifiant}</span>
        <button role="menuitem" onClick={seDeconnecter}><Icon name="lock" size={15} />Se déconnecter</button>
      </div>}
    </div>
  </header>;
}
