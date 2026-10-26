import { NavLink } from "react-router";
import { Icon, type IconName } from "@/components/ui/Icon";
import { peutAcceder, ROLES_PAR_ECRAN, ROUTES } from "@/app/routes";
import { useUtilisateur } from "@/features/auth/AuthContext";

const menu: [string, IconName, string, keyof typeof ROLES_PAR_ECRAN][] = [
  ["Tableau de bord", "grid", ROUTES.tableauDeBord, "tableauDeBord"],
  ["Importer des rapports", "upload", ROUTES.imports, "imports"],
  ["Dossiers", "folder", ROUTES.dossiers, "dossiers"],
  ["Analyse des prix", "chart", ROUTES.analysePrix, "analysePrix"],
  ["Génération des rapports", "file", ROUTES.generation, "generation"],
  ["Utilisateurs", "users", ROUTES.utilisateurs, "utilisateurs"],
];

/** Navigation : seules les entrées autorisées pour le rôle de l'utilisateur sont proposées. */
export function Sidebar({ collapsed, toggle }: { collapsed: boolean; toggle: () => void }) {
  const { role } = useUtilisateur();
  return <aside className="sidebar">
    <div className="logo-wrap"><img src="/logo-lt-expertise.png" alt="LT Expertise" className="official-logo" /></div>
    <button className="sidebar-toggle" onClick={toggle} aria-label={collapsed ? "Développer la navigation" : "Réduire la navigation"} title={collapsed ? "Développer" : "Réduire"}><Icon name="chevron" size={15} /></button>
    <nav className="nav"><span className="nav-label">ESPACE DE TRAVAIL</span>{menu.filter(([, , , ecran]) => peutAcceder(role, ecran)).map(([label, icon, cible]) =>
      <NavLink key={label} to={cible} end={cible === ROUTES.tableauDeBord} className={({ isActive }) => `nav-item ${label === "Utilisateurs" ? "users-item" : ""} ${isActive ? "active" : ""}`} title={collapsed ? label : undefined}><Icon name={icon} size={19} /><span>{label}</span></NavLink>
    )}</nav>
    <div className="sidebar-help"><span><Icon name="file" size={17} /></span><div><strong>Centre d’aide</strong><small>Guides et assistance</small></div><Icon name="chevron" size={15} /></div>
    <p className="version">LT Expertise • v2.4.0</p>
  </aside>;
}
