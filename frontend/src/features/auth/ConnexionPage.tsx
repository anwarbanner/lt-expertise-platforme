import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Navigate, useLocation, useNavigate } from "react-router";
import { messageErreur } from "@/api/client";
import { ROUTES } from "@/app/routes";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { useAuth } from "./AuthContext";
import { EcranChargement } from "./RouteProtegee";

const schema = z.object({
  identifiant: z.string().trim().min(1, "Saisissez votre identifiant.").max(50),
  motDePasse: z.string().min(1, "Saisissez votre mot de passe.").max(200),
});
type Formulaire = z.infer<typeof schema>;

/** Connexion (F11) : POST /api/auth/connexion ; verrouillage temporaire après 5 échecs (§8). */
export default function ConnexionPage() {
  const { etat, connecter } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [erreurServeur, setErreurServeur] = useState<string | null>(null);
  const [motDePasseVisible, setMotDePasseVisible] = useState(false);
  const [aideVisible, setAideVisible] = useState(false);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Formulaire>({
    resolver: zodResolver(schema),
    defaultValues: { identifiant: "", motDePasse: "" },
  });
  const destination = (location.state as { depuis?: string } | null)?.depuis ?? ROUTES.tableauDeBord;

  if (etat.statut === "chargement") return <EcranChargement />;
  if (etat.statut === "connecte") return <Navigate to={destination} replace />;

  async function envoyer(valeurs: Formulaire) {
    setErreurServeur(null);
    try {
      await connecter(valeurs.identifiant, valeurs.motDePasse);
      navigate(destination, { replace: true });
    } catch (erreur) {
      setErreurServeur(messageErreur(erreur, "Connexion impossible."));
    }
  }

  return <div className="login-page"><div className="login-decoration left" /><div className="login-decoration right" />
    <main className="login-card">
      <img src="/logo-lt-expertise.png" alt="LT Expertise" />
      <h1>Bienvenue</h1>
      <p>Plateforme de gestion des rapports d’expertise</p>
      <form className="login-form" onSubmit={handleSubmit(envoyer)} noValidate>
        {erreurServeur && <div className="login-error" role="alert"><Icon name="warning" size={17} /><span><strong>{erreurServeur}</strong>Le compte est verrouillé 15 minutes après 5 tentatives échouées.</span></div>}
        <label><span>Identifiant</span><div><Icon name="users" size={17} /><input autoComplete="username" placeholder="Votre identifiant" aria-invalid={!!errors.identifiant} {...register("identifiant")} /></div>
          {errors.identifiant && <small className="login-field-error">{errors.identifiant.message}</small>}</label>
        <label><span>Mot de passe</span><div><Icon name="lock" size={17} /><input type={motDePasseVisible ? "text" : "password"} autoComplete="current-password" placeholder="Votre mot de passe" aria-invalid={!!errors.motDePasse} {...register("motDePasse")} />
          <button type="button" className="login-eye" onClick={() => setMotDePasseVisible(!motDePasseVisible)} aria-label={motDePasseVisible ? "Masquer le mot de passe" : "Afficher le mot de passe"}><Icon name="eye" size={17} /></button></div>
          {errors.motDePasse && <small className="login-field-error">{errors.motDePasse.message}</small>}</label>
        <div className="login-meta"><button type="button" onClick={() => setAideVisible(!aideVisible)}>Mot de passe oublié ?</button></div>
        {aideVisible && <p className="login-aide">Les données restent au cabinet, sans envoi d’e-mail : demandez à l’administrateur de réinitialiser votre mot de passe.</p>}
        <Button disabled={isSubmitting}>{isSubmitting ? "Connexion…" : "Se connecter"} {!isSubmitting && <Icon name="arrow" size={15} />}</Button>
      </form>
      <small>Accès sécurisé réservé aux collaborateurs de LT Expertise</small>
    </main>
    <footer>© 2026 LT Expertise · Confidentialité · Assistance</footer>
  </div>;
}
