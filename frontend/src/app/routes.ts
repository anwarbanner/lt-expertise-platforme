/** Adresses des écrans. Les écrans d'un dossier sont identifiés par son n° de rapport. */
export const ROUTES = {
  connexion: "/connexion",
  tableauDeBord: "/",
  imports: "/imports",
  dossiers: "/dossiers",
  dossier: (numero: string) => `/dossiers/${encodeURIComponent(numero)}`,
  verification: (numero: string) => `/dossiers/${encodeURIComponent(numero)}/verification`,
  analysePrix: "/analyse-prix",
  generation: "/generation",
  utilisateurs: "/utilisateurs",
} as const;
