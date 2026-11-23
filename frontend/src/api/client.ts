import axios, { AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { ReponseConnexion } from "@/domain/auth";

/**
 * Client HTTP de l'API. Le jeton d'accès (JWT) n'existe qu'en mémoire ; le jeton de rafraîchissement est un cookie
 * HttpOnly que le navigateur joint seul aux appels /api/auth. Sur une réponse 401, la session est rafraîchie puis
 * la requête rejouée une fois ; si le rafraîchissement échoue (inactivité, déconnexion), la session est close.
 */
export const api = axios.create({ baseURL: "/api", withCredentials: true });

const ROUTES_AUTH = ["/auth/connexion", "/auth/rafraichir", "/auth/deconnexion"];

let jetonAcces: string | null = null;
let rafraichissementEnCours: Promise<ReponseConnexion> | null = null;
let surSessionExpiree: () => void = () => {};

export function definirJetonAcces(jeton: string | null) {
  jetonAcces = jeton;
}

export function jetonAccesCourant() {
  return jetonAcces;
}

/** Rappel appelé quand la session ne peut plus être rafraîchie (retour à l'écran de connexion). */
export function surExpirationSession(rappel: () => void) {
  surSessionExpiree = rappel;
}

/**
 * Rafraîchit la session avec le cookie. Un seul appel à la fois : les requêtes en échec au même moment attendent
 * le même résultat, car le serveur n'accepte chaque jeton de rafraîchissement qu'une fois.
 */
export function rafraichirSession(): Promise<ReponseConnexion> {
  if (!rafraichissementEnCours) {
    rafraichissementEnCours = api
      .post<ReponseConnexion>("/auth/rafraichir")
      .then((reponse) => {
        definirJetonAcces(reponse.data.jetonAcces);
        return reponse.data;
      })
      .finally(() => {
        rafraichissementEnCours = null;
      });
  }
  return rafraichissementEnCours;
}

function estRouteAuth(config: InternalAxiosRequestConfig) {
  return ROUTES_AUTH.some((route) => config.url?.endsWith(route));
}

api.interceptors.request.use((config) => {
  if (jetonAcces && !estRouteAuth(config)) {
    config.headers.set("Authorization", `Bearer ${jetonAcces}`);
  }
  return config;
});

type ConfigRejouable = InternalAxiosRequestConfig & { _rejouee?: boolean };

api.interceptors.response.use(
  (reponse) => reponse,
  async (erreur: AxiosError) => {
    const config = erreur.config as ConfigRejouable | undefined;
    if (erreur.response?.status !== 401 || !config || config._rejouee || estRouteAuth(config)) {
      throw erreur;
    }
    config._rejouee = true;
    try {
      await rafraichirSession();
    } catch {
      definirJetonAcces(null);
      surSessionExpiree();
      throw erreur;
    }
    config.headers.set("Authorization", `Bearer ${jetonAcces}`);
    return api(config);
  },
);

/** Message lisible d'une erreur d'API : celui du serveur s'il existe, sinon un message selon le cas. */
export function messageErreur(erreur: unknown, parDefaut = "Une erreur est survenue."): string {
  if (axios.isAxiosError(erreur)) {
    const message = (erreur.response?.data as { message?: string } | undefined)?.message;
    if (message) return message;
    if (!erreur.response) return "Serveur injoignable. Vérifiez que l'application est démarrée.";
    if (erreur.response.status === 403) return "Vous n'avez pas les droits pour cette action.";
    if (erreur.response.status === 413) return "Envoi trop volumineux pour le serveur : déposez moins de fichiers à la fois.";
  }
  return parDefaut;
}
