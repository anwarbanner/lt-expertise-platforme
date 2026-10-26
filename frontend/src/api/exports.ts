// Lot 6 : rapport PDF régénéré (F9, P10), exports Excel et historique des rapports générés (backend : generation).
import axios from "axios";
import type { FiltresDossiers } from "./dossiers";
import { api } from "./client";

export interface RapportGenere { genereLe: string; generePar: string | null; numeroRapport: string; assure: string | null; compagnie: string | null }

/** Paramètres non vides seulement, comme pour les recherches. */
function nettoyer(params: object) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ""));
}

/** Nom proposé par le serveur (Content-Disposition, forme UTF-8 prioritaire). */
export function nomDeFichier(entete: string | undefined): string | null {
  if (!entete) return null;
  const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(entete);
  if (utf8) return decodeURIComponent(utf8[1]);
  const simple = /filename="?([^";]+)"?/i.exec(entete);
  return simple ? simple[1] : null;
}

/** Texte d'un Blob (FileReader : disponible partout, y compris dans les tests). */
function lireTexte(contenu: Blob): Promise<string> {
  return new Promise((resoudre, rejeter) => {
    const lecteur = new FileReader();
    lecteur.onload = () => resoudre(String(lecteur.result));
    lecteur.onerror = () => rejeter(lecteur.error);
    lecteur.readAsText(contenu);
  });
}

/** Fait enregistrer le fichier par le navigateur. */
function enregistrer(contenu: Blob, nom: string) {
  const adresse = URL.createObjectURL(contenu);
  const lien = document.createElement("a");
  lien.href = adresse;
  lien.download = nom;
  document.body.appendChild(lien);
  lien.click();
  lien.remove();
  setTimeout(() => URL.revokeObjectURL(adresse), 1000);
}

/**
 * Télécharge un fichier produit par le serveur et renvoie son nom. Une erreur arrive elle aussi en binaire : son
 * message JSON est relu pour que messageErreur() l'affiche (ex. « validez d'abord le dossier »).
 */
export async function telecharger(url: string, params: object = {}, nomParDefaut = "export"): Promise<string> {
  try {
    const reponse = await api.get<Blob>(url, { params: nettoyer(params), responseType: "blob" });
    const nom = nomDeFichier(reponse.headers["content-disposition"] as string | undefined) ?? nomParDefaut;
    enregistrer(reponse.data, nom);
    return nom;
  } catch (erreur) {
    if (axios.isAxiosError(erreur) && erreur.response?.data instanceof Blob) {
      try {
        erreur.response.data = JSON.parse(await lireTexte(erreur.response.data));
      } catch {
        // corps non JSON : l'erreur d'origine suffit
      }
    }
    throw erreur;
  }
}

export const telechargerRapportPdf = (numero: string) => telecharger(`/rapports/${encodeURIComponent(numero)}/pdf`, {}, `rapport-${numero}.pdf`);
export const exporterDossier = (numero: string) => telecharger(`/exports/dossiers/${encodeURIComponent(numero)}`, {}, `dossier-${numero}.xlsx`);

/** Toute la liste répondant aux filtres (pas seulement la page affichée). */
export const exporterListe = (filtres: FiltresDossiers) => {
  const { page: _page, sort: _sort, ...criteres } = filtres;
  return telecharger("/exports/dossiers", criteres, "dossiers.xlsx");
};
export const exporterPrix = (filtres: { q?: string; type?: string; compagnie?: string; marque?: string }) => telecharger("/exports/prix", filtres, "analyse-prix.xlsx");
export const exporterJournal = (filtres: { q?: string; action?: string }) => telecharger("/journal-audit/export", filtres, "journal-audit.xlsx");

export async function historiqueRapports(limite = 20) {
  return (await api.get<RapportGenere[]>("/rapports/historique", { params: { limite } })).data;
}
