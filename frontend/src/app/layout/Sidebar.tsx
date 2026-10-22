import { NavLink } from "react-router";
import { Icon, type IconName } from "@/components/ui/Icon";
import { ROUTES } from "@/app/routes";

const menu: [string, IconName, string][] = [
  ["Tableau de bord", "grid", ROUTES.tableauDeBord],
  ["Importer des rapports", "upload", ROUTES.imports],
  ["Dossiers", "folder", ROUTES.dossiers],
  ["Analyse des prix", "chart", ROUTES.analysePrix],
  ["Génération des rapports", "file", ROUTES.generation],
  ["Utilisateurs", "users", ROUTES.utilisateurs],
];

export function Sidebar({ collapsed, toggle }: { collapsed: boolean; toggle: () => void }) {
  return <aside className="sidebar">
    <div className="logo-wrap"><img src="/logo-lt-expertise.png" alt="LT Expertise" className="official-logo" /></div>
    <button className="sidebar-toggle" onClick={toggle} aria-label={collapsed ? "Développer la navigation" : "Réduire la navigation"} title={collapsed ? "Développer" : "Réduire"}><Icon name="chevron" size={15} /></button>
    <nav className="nav"><span className="nav-label">ESPACE DE TRAVAIL</span>{menu.map(([label, icon, cible]) =>
      <NavLink key={label} to={cible} end={cible === ROUTES.tableauDeBord} className={({ isActive }) => `nav-item ${label === "Utilisateurs" ? "users-item" : ""} ${isActive ? "active" : ""}`} title={collapsed ? label : undefined}><Icon name={icon} size={19} /><span>{label}</span></NavLink>
    )}</nav>
    <div className="sidebar-help"><span><Icon name="file" size={17} /></span><div><strong>Centre d’aide</strong><small>Guides et assistance</small></div><Icon name="chevron" size={15} /></div>
    <p className="version">LT Expertise • v2.4.0</p>
  </aside>;
}
