import { useNavigate } from "react-router";
import { Icon } from "@/components/ui/Icon";
import { ROUTES } from "@/app/routes";

export function Topbar() {
  const navigate = useNavigate();
  return <header className="topbar">
    <div className="search"><Icon name="search" size={18} /><input aria-label="Recherche globale" placeholder="Rechercher un rapport, une immatriculation, un nom…" /><kbd>⌘ K</kbd></div>
    <button className="notification" aria-label="Notifications"><Icon name="bell" size={20} /><i /></button>
    <button className="user" onClick={() => navigate(ROUTES.connexion)}><span className="avatar">SB</span><div><strong>Samira Benali</strong><small>Gestionnaire</small></div><Icon name="chevron" size={15} /></button>
  </header>;
}
