import type { ReactNode } from "react";
import { Navigate, useLocation, useNavigate } from "react-router";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { PageHead } from "@/components/ui/PageHead";
import type { Role } from "@/domain/referentiels";
import { useAuth } from "./AuthContext";

/**
 * Affiche son contenu à un utilisateur connecté qui a l'un des rôles demandés (tous si {@code roles} est absent).
 * Le contrôle réel est fait par le serveur sur chaque appel (P9) : ceci évite seulement d'afficher un écran inutile.
 */
export function RouteProtegee({ roles, children }: { roles?: readonly Role[]; children: ReactNode }) {
  const { etat } = useAuth();
  const location = useLocation();
  if (etat.statut === "chargement") return <EcranChargement />;
  if (etat.statut === "deconnecte") {
    return <Navigate to={ROUTES.connexion} replace state={{ depuis: location.pathname }} />;
  }
  if (roles && !roles.includes(etat.utilisateur.role)) return <AccesRefuse />;
  return <>{children}</>;
}

export function EcranChargement() {
  return <div className="ecran-chargement" role="status">Chargement de la session…</div>;
}

function AccesRefuse() {
  const navigate = useNavigate();
  return <main className="main">
    <PageHead title="Accès refusé" subtitle="Votre rôle ne donne pas accès à cet écran." />
    <article className="card acces-refuse">
      <p>Si vous pensez devoir y accéder, adressez-vous à l’administrateur de la plateforme.</p>
      <Button onClick={() => navigate(ROUTES.tableauDeBord)}>Retour au tableau de bord</Button>
    </article>
  </main>;
}
