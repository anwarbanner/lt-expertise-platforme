import { useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";

/** Connexion (F11). À brancher sur l'authentification JWT, pas encore disponible côté backend. */
export default function ConnexionPage() {
  const navigate = useNavigate();
  return <div className="login-page"><div className="login-decoration left" /><div className="login-decoration right" /><main className="login-card"><img src="/logo-lt-expertise.png" alt="LT Expertise" /><h1>Bienvenue</h1><p>Plateforme de gestion des rapports d’expertise</p><div className="login-error"><Icon name="warning" size={17} /><span><strong>Identifiant ou mot de passe incorrect</strong>Compte verrouillé après 5 tentatives.</span></div><label><span>Identifiant</span><div><Icon name="users" size={17} /><input placeholder="Votre identifiant" /></div></label><label><span>Mot de passe</span><div><Icon name="lock" size={17} /><input type="password" value="motdepasse" readOnly /><Icon name="eye" size={17} /></div></label><div className="login-meta"><label><input type="checkbox" /> Se souvenir de moi</label><button>Mot de passe oublié ?</button></div><Button onClick={() => navigate(ROUTES.tableauDeBord)}>Se connecter <Icon name="arrow" size={15} /></Button><small>Accès sécurisé réservé aux collaborateurs de LT Expertise</small></main><footer>© 2026 LT Expertise · Confidentialité · Assistance</footer></div>;
}
