import type { Role } from "@/domain/referentiels";

/** Adresses des écrans. Les écrans d'un dossier sont identifiés par son n° de rapport. */
export const ROUTES = {
  connexion: "/connexion",
  tableauDeBord: "/",
  imports: "/imports",
  dossiers: "/dossiers",
  dossier: (numero: string) => `/dossiers/${encodeURIComponent(numero)}`,
  verification: (numero: string) => `/dossiers/${encodeURIComponent(numero)}/verification`,
  analysePrix: "/analyse-prix",
  recherchePieces: "/recherche-pieces",
  valeursVenales: "/valeurs-venales",
  generation: "/generation",
  utilisateurs: "/utilisateurs",
} as const;

const TOUS: readonly Role[] = ["ADMINISTRATEUR", "GESTIONNAIRE", "CONSULTATION"];

/**
 * Rôles autorisés par écran (F11) : le gestionnaire importe, vérifie et valide ; la consultation lit et exporte ;
 * l'administrateur a tous les droits et gère en plus les comptes. Le serveur applique les mêmes règles sur chaque
 * appel (P9).
 */
export const ROLES_PAR_ECRAN = {
  tableauDeBord: TOUS,
  imports: ["GESTIONNAIRE", "ADMINISTRATEUR"],
  dossiers: TOUS,
  verification: ["GESTIONNAIRE", "ADMINISTRATEUR"],
  analysePrix: TOUS,
  recherchePieces: TOUS,
  valeursVenales: TOUS,
  generation: TOUS,
  utilisateurs: ["ADMINISTRATEUR"],
} as const satisfies Record<string, readonly Role[]>;

export function peutAcceder(role: Role, ecran: keyof typeof ROLES_PAR_ECRAN): boolean {
  return (ROLES_PAR_ECRAN[ecran] as readonly Role[]).includes(role);
}

/** Écran d'ouverture d'un dossier : vérification pour un gestionnaire tant qu'il n'est pas validé, sinon fiche. */
export function adresseDossier(numero: string, statut: string, role: Role): string {
  return statut !== "VALIDE" && peutAcceder(role, "verification") ? ROUTES.verification(numero) : ROUTES.dossier(numero);
}
