import type {
  AnalysePrix, CompteUtilisateur, DossierDetail, DossierModification, DossierResume, EntreeJournal,
  EvenementHistorique, FiltresPieces, FiltresPrix, FiltresValeurs, Page, ResultatPieces, ResultatValeurs,
  ResultatValidationGroupee, SuggestionsVehicule, TableauDeBord, ValidationGroupee,
} from "@/domain/dossiers";
import type { Role, StatutDossier } from "@/domain/referentiels";
import { api } from "./client";

export interface FiltresDossiers {
  q?: string;
  statut?: string;
  compagnie?: string;
  zone?: string;
  du?: string;
  au?: string;
  montantMin?: string;
  montantMax?: string;
  anomalies?: string;
  page?: number;
  sort?: string;
}

/** Paramètres non vides seulement : un filtre vide ne doit pas restreindre la recherche. */
function nettoyer(params: object) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ""));
}

export const cles = {
  dossiers: (filtres: FiltresDossiers) => ["dossiers", "liste", filtres] as const,
  compteurs: ["dossiers", "compteurs"] as const,
  compagnies: ["dossiers", "compagnies"] as const,
  dossier: (numero: string) => ["dossiers", "detail", numero] as const,
  historique: (numero: string) => ["dossiers", "historique", numero] as const,
  tableauDeBord: (periode: string) => ["statistiques", "tableau-de-bord", periode] as const,
  prix: (filtres: object) => ["statistiques", "prix", filtres] as const,
  suggestions: ["recherche", "suggestions"] as const,
  pieces: (filtres: object) => ["recherche", "pieces", filtres] as const,
  valeurs: (filtres: object) => ["recherche", "valeurs", filtres] as const,
  utilisateurs: ["utilisateurs"] as const,
  journal: (filtres: object) => ["journal", filtres] as const,
  pdf: (importId: number) => ["pdf", importId] as const,
};

export async function rechercherDossiers(filtres: FiltresDossiers, taille = 10) {
  return (await api.get<Page<DossierResume>>("/dossiers", { params: nettoyer({ ...filtres, size: taille }) })).data;
}

export async function compteursDossiers() {
  return (await api.get<Partial<Record<StatutDossier, number>>>("/dossiers/compteurs")).data;
}

export async function compagnies() {
  return (await api.get<string[]>("/dossiers/compagnies")).data;
}

export async function dossier(numero: string) {
  return (await api.get<DossierDetail>(`/dossiers/${encodeURIComponent(numero)}`)).data;
}

export async function modifierDossier(numero: string, modification: DossierModification) {
  return (await api.put<DossierDetail>(`/dossiers/${encodeURIComponent(numero)}`, modification)).data;
}

export async function validerDossier(numero: string) {
  return (await api.post<DossierDetail>(`/dossiers/${encodeURIComponent(numero)}/validation`)).data;
}

/** Validation en lot (gestionnaire) : renvoie le nombre de dossiers validés et ceux ignorés avec leur motif. */
export async function validerEnLot(demande: ValidationGroupee) {
  return (await api.post<ResultatValidationGroupee>("/dossiers/validation-groupee", nettoyer(demande))).data;
}

export async function historique(numero: string) {
  return (await api.get<EvenementHistorique[]>(`/dossiers/${encodeURIComponent(numero)}/historique`)).data;
}

/** PDF original (P1), en objet Blob affichable dans la page. */
export async function pdfOriginal(importId: number) {
  return (await api.get<Blob>(`/imports/${importId}/pdf`, { responseType: "blob" })).data;
}

export async function tableauDeBord(periode: string) {
  return (await api.get<TableauDeBord>("/statistiques/tableau-de-bord", { params: nettoyer({ periode }) })).data;
}

/** Paramètres d'un tableau paginé côté serveur (page, taille, tri, f_<colonne>), voir parametresTableau. */
type ParametresTableau = Record<string, string | number>;

export async function analysePrix(filtres: FiltresPrix, tableau: ParametresTableau = {}) {
  return (await api.get<AnalysePrix>("/statistiques/prix", { params: nettoyer({ ...filtres, ...tableau }) })).data;
}

export async function suggestionsVehicule() {
  return (await api.get<SuggestionsVehicule>("/recherche/suggestions")).data;
}

export async function rechercherPieces(filtres: FiltresPieces, tableau: ParametresTableau = {}) {
  return (await api.get<ResultatPieces>("/recherche/pieces", { params: nettoyer({ ...filtres, ...tableau }) })).data;
}

export async function rechercherValeurs(filtres: FiltresValeurs, tableau: ParametresTableau = {}) {
  return (await api.get<ResultatValeurs>("/recherche/valeurs", { params: nettoyer({ ...filtres, ...tableau }) })).data;
}

export async function utilisateurs() {
  return (await api.get<CompteUtilisateur[]>("/utilisateurs")).data;
}

export interface CreationCompte { identifiant: string; nom: string; prenom: string; email: string | null; role: Role; motDePasse: string }
export interface ModificationCompte { nom: string; prenom: string; email: string | null; role: Role; actif: boolean }

export async function creerUtilisateur(compte: CreationCompte) {
  return (await api.post<CompteUtilisateur>("/utilisateurs", compte)).data;
}

export async function modifierUtilisateur(id: number, compte: ModificationCompte) {
  return (await api.put<CompteUtilisateur>(`/utilisateurs/${id}`, compte)).data;
}

export async function reinitialiserMotDePasse(id: number, motDePasse: string) {
  await api.post(`/utilisateurs/${id}/mot-de-passe`, { motDePasse });
}

export async function journal(filtres: { q?: string; action?: string; page?: number }) {
  return (await api.get<Page<EntreeJournal>>("/journal-audit", { params: nettoyer({ ...filtres, size: 20 }) })).data;
}
