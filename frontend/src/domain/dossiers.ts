// Contrats de l'API des dossiers, des statistiques et de l'administration (backend : dossier, statistiques,
// utilisateur, audit). Les montants (BigDecimal) arrivent en nombres.
import type { NatureContrat, NatureRapport, Role, StatutDossier, TypeOperation, TypePiece, TypeProcedure } from "./referentiels";

export type Energie = "ESSENCE" | "DIESEL" | "HYBRIDE" | "ELECTRIQUE" | "AUTRE";
export type NatureEvenement = "CIRCULATION_HORS_STATIONNEMENT" | "STATIONNEMENT" | "AUTRE";
export type FamilleTravaux = "TOLERIE" | "PEINTURE" | "MECANIQUE";

export interface Montants { ht: number | null; tva: number | null; ttc: number | null }

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface DossierResume {
  id: number;
  numeroRapport: string;
  statut: StatutDossier;
  natureRapport: NatureRapport;
  typeProcedure: TypeProcedure | null;
  dateSinistre: string | null;
  dateRapport: string | null;
  assure: string | null;
  immatriculation: string | null;
  compagnie: string | null;
  /** Zones de choc 1 à 8 : un rapport peut en avoir plusieurs. */
  zonesChoc: number[];
  montantIndemnisation: number | null;
  creeLe: string;
  /** Contrôles bloquants en échec. */
  anomalies: number;
}

export interface Partie {
  nomPrenom: string | null;
  vehicule: string | null;
  immatriculation: string | null;
  compagnie: string | null;
  numeroPolice: string | null;
  intermediaire: string | null;
}

export interface Vehicule {
  marque: string | null;
  modele: string | null;
  typeMine: string | null;
  immatriculation: string | null;
  numeroSerie: string | null;
  energie: Energie | null;
  kilometrage: number | null;
  kilometrageNonEstimable: boolean;
  puissanceFiscale: number | null;
  dateMiseCirculation: string | null;
  etatGeneral: string | null;
  usurePneuAvg: number | null;
  usurePneuAvd: number | null;
  usurePneuArg: number | null;
  usurePneuArd: number | null;
}

export interface LigneDetail {
  ordre: number;
  zoneLibelle: string | null;
  zoneChoc: number | null;
  famille: FamilleTravaux | null;
  quantite: number;
  designation: string;
  typePiece: TypePiece | null;
  prixUnitaire: number | null;
  vetustePct: number | null;
  tvaPct: number | null;
  totalFourniture: number | null;
  operation: TypeOperation | null;
  nombreHeures: number | null;
  tarifHoraire: number | null;
  montantMoTtc: number | null;
  /** quantité × prix × (1 + TVA) × (1 − vétusté) = total : sinon la ligne est en rouge et son prix à corriger. */
  prixCoherent: boolean;
  /** Prix unitaire qui redonne le total imprimé, proposé pour la correction. */
  prixCalcule: number | null;
}

export interface Controle {
  type: "COHERENCE" | "CHAMP_MANQUANT";
  code: string;
  libelle: string;
  champ: string | null;
  attendu: number | null;
  obtenu: number | null;
  conforme: boolean;
  /** Un échec bloquant empêche la validation ; sinon c'est un avertissement (champ absent du PDF, prix imprimé faux). */
  bloquant: boolean;
}

export type CleCommentaire = "VALEUR_NEUF_CATALOGUE" | "VALEUR_VENALE_FMA" | "DIVERGENCE_USAGE" | "PIECES_RECUPERATION_FERRAILLE"
  | "PEINTURE_REGLES_ART" | "LOCALISATION_CHOC" | "CADRE_EXPERTISE" | "MONTANT_DEVIS" | "ESTIMATION_SANS_DEVIS"
  | "EXPERT_CONFRERE" | "MODE_EXPERTISE_CONFRERE" | "DATE_ACCORD";

/** Valeur clé lue dans le commentaire de l'expert (F12, rapports 2026). */
export interface ValeurCommentaire { cle: CleCommentaire; texte: string | null; montant: number | null; date: string | null }

export interface DossierDetail {
  id: number;
  numeroRapport: string;
  statut: StatutDossier;
  natureRapport: NatureRapport;
  typeProcedure: TypeProcedure | null;
  cabinet: string | null;
  expert: string | null;
  compagnie: string | null;
  refSinistre: string | null;
  dateSinistre: string | null;
  dateMission: string | null;
  dateExpertise: string | null;
  dateAvantReparation: string | null;
  dateEnCoursTravaux: string | null;
  dateApresTravaux: string | null;
  dateRapport: string | null;
  lieuRapport: string | null;
  natureContrat: NatureContrat | null;
  natureEvenement: NatureEvenement | null;
  zonesChoc: number[];
  reparateur: string | null;
  accordReparateur: boolean | null;
  assure: Partie | null;
  adversaire: Partie | null;
  vehicule: Vehicule | null;
  mainOeuvre: { poste: string; nombreHeures: number | null; tauxHoraire: number | null; montants: Montants }[];
  fournitures: { categorie: string; montants: Montants }[];
  totalMainOeuvre: Montants | null;
  totalFournitures: Montants | null;
  totalGeneral: Montants | null;
  vetusteTtc: number | null;
  vetusteHt: number | null;
  depannage: number | null;
  immobilisation: number | null;
  typeReforme: "ECONOMIQUE" | "TECHNIQUE" | null;
  epavisteNom: string | null;
  epavisteVille: string | null;
  valeurNeuf: number | null;
  valeurAssuree: number | null;
  valeurVenale: number | null;
  valeurEpave: number | null;
  tvaRecuperable: boolean | null;
  montantIndemnisation: number | null;
  montantLettres: string | null;
  commentaire: string | null;
  valeursCommentaire: ValeurCommentaire[];
  lignesDetail: LigneDetail[];
  controles: Controle[];
  champsASaisir: string[];
  importId: number | null;
  nomFichier: string | null;
  validePar: string | null;
  valideLe: string | null;
  /** Import annulé (statut ANNULE) : par qui, quand et pourquoi. */
  annulePar: string | null;
  annuleLe: string | null;
  motifAnnulation: string | null;
  creeLe: string;
  version: number;
}

/** Champs modifiables à l'écran de vérification (PUT /api/dossiers/{numero}). */
export type DossierModification = Pick<DossierDetail,
  "version" | "typeProcedure" | "cabinet" | "expert" | "compagnie" | "refSinistre" | "dateSinistre" | "dateMission"
  | "dateExpertise" | "dateAvantReparation" | "dateEnCoursTravaux" | "dateApresTravaux" | "dateRapport" | "lieuRapport"
  | "natureContrat" | "natureEvenement" | "zonesChoc" | "reparateur" | "accordReparateur" | "assure" | "adversaire"
  | "vehicule" | "vetusteTtc" | "depannage" | "valeurNeuf" | "valeurAssuree" | "valeurVenale" | "valeurEpave"
  | "tvaRecuperable" | "montantIndemnisation" | "montantLettres" | "commentaire"> & {
  /** Prix unitaires corrigés des lignes de la page 2 (lignes au prix incohérent). */
  prixLignes: { ordre: number; prixUnitaire: number | null }[];
};

/** Validation en lot : les numéros cochés, ou tous les dossiers prêts à valider répondant aux filtres. */
export interface ValidationGroupee {
  numeros?: string[];
  tousLesPrets?: boolean;
  q?: string;
  compagnie?: string;
  zone?: string;
  du?: string;
  au?: string;
  montantMin?: string;
  montantMax?: string;
}

export interface ResultatValidationGroupee { valides: number; ignores: { numeroRapport: string; motif: string }[]; paquets: number }

/** Archivage en lot : les numéros cochés (dossiers validés), ou tous les validés répondant aux filtres. */
export type ArchivageGroupe = Omit<ValidationGroupee, "tousLesPrets"> & { tousLesValides?: boolean };

export interface ResultatArchivageGroupe { archives: number; ignores: { numeroRapport: string; motif: string }[]; paquets: number }

export interface EvenementHistorique {
  horodatage: string;
  action: string;
  utilisateur: string | null;
  champ: string | null;
  ancienneValeur: string | null;
  nouvelleValeur: string | null;
  details: Record<string, unknown> | null;
}

/** Montants à null hors administrateur : le serveur ne les transmet qu'à ce rôle (P9). */
export interface TableauDeBord {
  periode: string | null;
  dossiers: number;
  montantTotal: number | null;
  coutMoyen: number | null;
  enAttenteValidation: number;
  parStatut: Partial<Record<StatutDossier, number>>;
  mensuel: { mois: string; nombre: number; montant: number | null; mainOeuvreTtc: number | null; fournituresTtc: number | null }[];
  parZone: { cle: string; nombre: number }[];
  parCompagnie: { cle: string; nombre: number }[];
  alertes: { numeroRapport: string; incoherences: number; champsManquants: number; premierControle: string | null }[];
  dossiersEnAnomalie: number;
  derniersDossiers: { numeroRapport: string; importeLe: string; compagnie: string | null; montantIndemnisation: number | null; statut: StatutDossier }[];
}

export interface FiltresPrix { designation?: string; type?: string; marque?: string; modele?: string; typeMine?: string }

export interface LignePrix {
  numeroRapport: string;
  designation: string;
  typePiece: TypePiece;
  marque: string | null;
  modele: string | null;
  typeMine: string | null;
  prixUnitaire: number;
  /** Date du prix : celle du rapport, à défaut celle de l'import. */
  date: string | null;
}

export interface AnalysePrix {
  nombre: number;
  minimum: number | null;
  moyenne: number | null;
  maximum: number | null;
  moyenneParType: Partial<Record<TypePiece, number>>;
  /** Page {@link page} (à partir de 0) de {@link taille} lignes sur les {@link nombre} filtrées. */
  lignes: LignePrix[];
  page: number;
  taille: number;
}

/** Valeurs déjà rencontrées, proposées dans les filtres du véhicule. */
export interface SuggestionsVehicule { marques: string[]; modeles: string[]; typesMine: string[] }

export interface FiltresPieces { piece?: string; type?: string; zone?: string; marque?: string; modele?: string; typeMine?: string; annee?: string; chassis?: string }

export interface LignePiece {
  numeroRapport: string;
  designation: string;
  typePiece: TypePiece;
  zoneChoc: number | null;
  marque: string | null;
  modele: string | null;
  typeMine: string | null;
  dateMiseCirculation: string | null;
  numeroChassis: string | null;
  prixUnitaire: number | null;
  date: string | null;
}

export interface ResultatPieces { nombre: number; lignes: LignePiece[]; page: number; taille: number }

/** Âges en mois ; valeurs d'achat (valeur à neuf) et vénale lues dans le commentaire de l'expert. */
export interface FiltresValeurs {
  marque?: string; modele?: string; typeMine?: string; mecDu?: string; mecAu?: string; ageMin?: string; ageMax?: string;
  achatMin?: string; achatMax?: string; venaleMin?: string; venaleMax?: string;
}

export interface LigneValeur {
  numeroRapport: string;
  marque: string | null;
  modele: string | null;
  typeMine: string | null;
  dateMiseCirculation: string | null;
  dateSinistre: string | null;
  ageMois: number | null;
  valeurAchat: number | null;
  valeurVenale: number | null;
}

export interface ResultatValeurs { nombre: number; moyenneAchat: number | null; moyenneVenale: number | null; lignes: LigneValeur[]; page: number; taille: number }

export interface CompteUtilisateur {
  id: number;
  identifiant: string;
  nom: string;
  prenom: string;
  email: string | null;
  role: Role;
  actif: boolean;
  verrouille: boolean;
  derniereConnexion: string | null;
  creeLe: string;
}

export interface EntreeJournal {
  id: number;
  horodatage: string;
  utilisateur: string | null;
  action: string;
  typeEntite: string | null;
  entiteId: number | null;
  numeroRapport: string | null;
  champ: string | null;
  ancienneValeur: string | null;
  nouvelleValeur: string | null;
  details: Record<string, unknown> | null;
  adresseIp: string | null;
}
