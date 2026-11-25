// API simulée pour les tests d'écrans : réponses minimales et valides de chaque endpoint, à surcharger au besoin.
import MockAdapter from "axios-mock-adapter";
import { api } from "@/api/client";
import type { AnalysePrix, CompteUtilisateur, DossierDetail, DossierResume, Page, TableauDeBord } from "@/domain/dossiers";
import type { StatutDossier } from "@/domain/referentiels";

export function page<T>(content: T[], number = 0, size = 20, totalElements = content.length): Page<T> {
  return { content, page: { size, number, totalElements, totalPages: Math.ceil(totalElements / size) } };
}

export function dossierResume(numeroRapport: string, statut: StatutDossier, autres: Partial<DossierResume> = {}): DossierResume {
  return {
    id: Number(numeroRapport.replace(/\D/g, "")), numeroRapport, statut, natureRapport: "DEFINITIF", typeProcedure: null,
    dateSinistre: "2026-10-01", dateRapport: "2026-10-10", assure: "Hassan Mansouri", immatriculation: "32148-B-6",
    compagnie: "Wafa Assurance", zonesChoc: [1], montantIndemnisation: 2400, creeLe: "2026-10-20T10:00:00Z", anomalies: 0, libellesAnomalies: [], ...autres,
  };
}

export function dossierDetail(numeroRapport: string, statut: StatutDossier, autres: Partial<DossierDetail> = {}): DossierDetail {
  const nul = null;
  return {
    id: 1, numeroRapport, statut, natureRapport: "DEFINITIF", typeProcedure: nul, cabinet: "LT Expertise", expert: nul,
    compagnie: "Wafa Assurance", refSinistre: nul, dateSinistre: "2026-10-01", dateMission: nul, dateExpertise: nul,
    dateAvantReparation: nul, dateEnCoursTravaux: nul, dateApresTravaux: nul, dateRapport: "2026-10-10", lieuRapport: "Casablanca",
    natureContrat: nul, natureEvenement: nul, zonesChoc: [1], reparateur: nul, accordReparateur: nul,
    assure: { nomPrenom: "Hassan Mansouri", vehicule: nul, immatriculation: "32148-B-6", compagnie: nul, numeroPolice: nul, intermediaire: nul },
    adversaire: nul,
    vehicule: {
      marque: "Renault", modele: "Clio V", typeMine: nul, immatriculation: "32148-B-6", numeroSerie: nul, energie: nul,
      kilometrage: nul, kilometrageNonEstimable: false, puissanceFiscale: nul, dateMiseCirculation: nul, etatGeneral: nul,
      usurePneuAvg: nul, usurePneuAvd: nul, usurePneuArg: nul, usurePneuArd: nul,
    },
    mainOeuvre: [], fournitures: [], totalMainOeuvre: { ht: 1200, tva: 240, ttc: 1440 }, totalFournitures: { ht: 800, tva: 160, ttc: 960 },
    totalGeneral: { ht: 2000, tva: 400, ttc: 2400 }, vetusteTtc: nul, vetusteHt: nul, depannage: nul, immobilisation: nul,
    typeReforme: nul, epavisteNom: nul, epavisteVille: nul, valeurNeuf: nul, valeurAssuree: nul, valeurVenale: nul, valeurEpave: nul,
    tvaRecuperable: nul, montantIndemnisation: 2400, montantLettres: "Deux mille quatre cents dirhams", commentaire: nul, valeursCommentaire: [],
    lignesDetail: [], controles: [], champsASaisir: [], importId: nul, nomFichier: nul, validePar: nul, valideLe: nul,
    annulePar: nul, annuleLe: nul, motifAnnulation: nul,
    creeLe: "2026-10-20T10:00:00Z", version: 0, ...autres,
  };
}

export const TABLEAU_VIDE: TableauDeBord = {
  periode: null, dossiers: 0, montantTotal: 0, coutMoyen: null, enAttenteValidation: 0, parStatut: {}, mensuel: [],
  parZone: [], parCompagnie: [], alertes: [], dossiersEnAnomalie: 0, derniersDossiers: [],
};

export const PRIX_VIDE: AnalysePrix = {
  nombre: 0, dossiers: 0, minimum: null, moyenne: null, maximum: null, parType: [], lignes: [], page: 0, taille: 25,
};

export function compte(identifiant: string, autres: Partial<CompteUtilisateur> = {}): CompteUtilisateur {
  return {
    id: identifiant.length, identifiant, nom: "Benali", prenom: "Samira", email: null, role: "GESTIONNAIRE", actif: true,
    verrouille: false, derniereConnexion: null, creeLe: "2026-10-01T08:00:00Z", ...autres,
  };
}

/**
 * Remplace le client HTTP par une API simulée : liste de dossiers (268202-0313 à vérifier, 268202-0284 validé),
 * fiches correspondantes et réponses vides pour le reste. Les surcharges du test priment : axios-mock-adapter
 * retient le premier gestionnaire qui correspond, et remplace celui de même adresse. Les surcharges s'enregistrent
 * donc après les adresses exactes (qu'elles remplacent) et avant les motifs génériques de fiche de dossier.
 */
export function simulerApi(surcharges: (serveur: MockAdapter) => void = () => {}) {
  const serveur = new MockAdapter(api);
  const dossiers = [dossierResume("268202-0313", "A_VERIFIER", { anomalies: 1, libellesAnomalies: ["Total général TTC = total MO + total fournitures"] }), dossierResume("268202-0284", "VALIDE")];
  serveur.onGet("/imports").reply(200, page([]));
  serveur.onGet("/dossiers/compteurs").reply(200, { A_VERIFIER: 1, VALIDE: 1 });
  serveur.onGet("/dossiers/compagnies").reply(200, ["Wafa Assurance"]);
  serveur.onGet("/dossiers").reply(200, page(dossiers, 0, 10));
  serveur.onGet("/statistiques/tableau-de-bord").reply(200, TABLEAU_VIDE);
  serveur.onGet("/statistiques/prix").reply(200, PRIX_VIDE);
  serveur.onGet("/recherche/suggestions").reply(200, { marques: [], modeles: [], typesMine: [] });
  serveur.onGet("/recherche/valeurs").reply(200, { nombre: 0, moyenneAchat: null, moyenneVenale: null, lignes: [], page: 0, taille: 25 });
  serveur.onGet("/utilisateurs").reply(200, []);
  serveur.onGet("/journal-audit").reply(200, page([]));
  serveur.onGet("/rapports/historique").reply(200, []);
  surcharges(serveur);
  serveur.onGet(/^\/dossiers\/[^/]+\/historique$/).reply(200, []);
  serveur.onGet(/^\/dossiers\/[^/]+$/).reply((config) => {
    const numero = decodeURIComponent(config.url!.split("/").pop()!);
    const resume = dossiers.find((d) => d.numeroRapport === numero);
    return resume ? [200, dossierDetail(numero, resume.statut)] : [404, { message: "Dossier introuvable." }];
  });
  return serveur;
}
