// Mise en forme française des montants et des dates venant de l'API.

const MONTANT = new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const ENTIER = new Intl.NumberFormat("fr-FR", { maximumFractionDigits: 0 });

/** « 3 840,00 DH », ou « — » si absent. */
export function montant(valeur: number | null | undefined, unite = " DH"): string {
  return valeur === null || valeur === undefined ? "—" : `${MONTANT.format(valeur)}${unite}`;
}

/** « 356 400 DH » (sans décimales), pour les indicateurs. */
export function montantArrondi(valeur: number | null | undefined): string {
  return valeur === null || valeur === undefined ? "—" : `${ENTIER.format(valeur)} DH`;
}

export function nombre(valeur: number | null | undefined): string {
  return valeur === null || valeur === undefined ? "—" : ENTIER.format(valeur);
}

/** « 17/10/2026 » pour une date « 2026-10-17 » ou un horodatage ISO. */
export function date(valeur: string | null | undefined): string {
  if (!valeur) return "—";
  const d = new Date(valeur.length === 10 ? `${valeur}T00:00:00` : valeur);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleDateString("fr-FR");
}

export function dateHeure(valeur: string | null | undefined): string {
  if (!valeur) return "—";
  const d = new Date(valeur);
  return Number.isNaN(d.getTime()) ? "—" : d.toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "short" });
}

/** « Oct. 2026 » pour « 2026-10 ». */
export function mois(valeur: string, long = false): string {
  const [annee, m] = valeur.split("-").map(Number);
  return new Date(annee, m - 1, 1).toLocaleDateString("fr-FR", long ? { month: "long", year: "numeric" } : { month: "short" });
}
