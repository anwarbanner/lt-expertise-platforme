// Données de démonstration de la maquette Figma (noms et montants fictifs).
// Chaque bloc sera remplacé par un appel à l'API ; la colonne « API » indique l'endpoint cible
// (voir docs/conception/analyse-frontend-roadmap.md).
import type { StatutAffiche } from "@/domain/referentiels";

/** Dossier utilisé par les écrans Vérification et Détail tant qu'ils ne lisent pas l'API. */
export const DOSSIER_DEMO = "268202-0284";

// API : GET /api/statistiques/statuts (F10)
export const repartitionStatuts: readonly (readonly [StatutAffiche, number])[] = [
  ["VALIDE", 121], ["PRET_A_VALIDER", 9], ["A_VERIFIER", 6], ["A_COMPLETER", 8], ["IMPORTE", 4], ["ARCHIVE", 0],
];

// API : GET /api/dossiers?tri=importeLe,desc&taille=5 (F8)
export const derniersImports: readonly (readonly [string, string, string, string, StatutAffiche])[] = [
  ["268202-0314", "28/10/2026", "Wafa Assurance", "3 840,00 DH", "PRET_A_VALIDER"],
  ["268202-0313", "28/10/2026", "Sanlam Maroc", "5 120,00 DH", "A_COMPLETER"],
  ["268202-0312", "27/10/2026", "RMA Assurance", "2 760,00 DH", "VALIDE"],
  ["268202-0311", "27/10/2026", "AtlantaSanad", "6 400,00 DH", "A_VERIFIER"],
  ["268202-0310", "26/10/2026", "AXA Assurance", "1 980,00 DH", "IMPORTE"],
];

// API : GET /api/dossiers/anomalies (alertes d'anomalies, resultat_controle non conforme)
export const alertes = [
  ["error", "268202-0291", "Total TTC incohérent (attendu 2 400,00, trouvé 2 640,00)"],
  ["warn", "268202-0302", "Taux horaire 75 DH au lieu de 60 DH"],
  ["warn", "268202-0288", "6 champs à compléter"],
  ["error", "268202-0305", "Montant en lettres différent du montant en chiffres"],
] as const;

// API : GET /api/statistiques/zones-choc et /compagnies (F10)
export const dossiersParZone = [["Avant", 84], ["Arrière", 66], ["Latéral droit", 42], ["Latéral gauche", 35], ["Central", 22], ["Toit", 12]] as const;
export const dossiersParCompagnie = [["Wafa Assurance", 78], ["RMA Assurance", 62], ["Sanlam Maroc", 54], ["AXA Assurance", 43], ["AtlantaSanad", 31]] as const;

// API : POST /api/imports → RecapitulatifImport (F1, disponible)
export const lignesImport: readonly (readonly [string, string, string, string, string, StatutAffiche, string])[] = [
  ["rapport_268202-0314.pdf", "268202-0314", "3 840,00 DH", "27/27", "0", "PRET_A_VALIDER", ""],
  ["expertise_0313.pdf", "268202-0313", "5 120,00 DH", "24/27", "3", "A_COMPLETER", ""],
  ["rapport_268202-0312.pdf", "268202-0312", "2 760,00 DH", "27/27", "0", "PRET_A_VALIDER", ""],
  ["scan_expertise_0309.pdf", "268202-0309", "—", "—", "—", "REFUSE", "Modèle de PDF non reconnu"],
  ["rapport_0284_copie.pdf", "268202-0284", "2 400,00 DH", "27/27", "0", "REFUSE", "Doublon : rapport 268202-0284 déjà importé"],
];

// API : GET /api/dossiers?… (F7, F8)
export const dossiers: readonly (readonly [string, string, string, string, string, string, string, StatutAffiche])[] = [
  ["268202-0314", "26/10/2026", "Nadia Alaoui", "42819-A-6", "Wafa Assurance", "Avant", "3 840,00", "PRET_A_VALIDER"],
  ["268202-0313", "25/10/2026", "Youssef Amrani", "15274-B-8", "Sanlam Maroc", "Arrière", "5 120,00", "A_COMPLETER"],
  ["268202-0312", "25/10/2026", "Salma Idrissi", "78421-A-1", "RMA Assurance", "Latéral droit", "2 760,00", "VALIDE"],
  ["268202-0311", "24/10/2026", "Omar Chraïbi", "9567-D-6", "AtlantaSanad", "Avant", "6 400,00", "A_VERIFIER"],
  ["268202-0310", "23/10/2026", "Khadija Bennani", "36482-A-3", "AXA Assurance", "Arrière", "1 980,00", "IMPORTE"],
  ["268202-0309", "22/10/2026", "Mehdi El Fassi", "85214-B-7", "Wafa Assurance", "Central", "7 250,00", "VALIDE"],
  ["268202-0308", "22/10/2026", "Imane Tazi", "19548-A-2", "RMA Assurance", "Latéral gauche", "4 300,00", "VALIDE"],
  ["268202-0307", "21/10/2026", "Karim Berrada", "62149-D-5", "Sanlam Maroc", "Avant", "8 940,00", "A_COMPLETER"],
  ["268202-0306", "21/10/2026", "Sara Lahlou", "47510-A-9", "AXA Assurance", "Toit", "3 125,00", "VALIDE"],
  ["268202-0284", "20/10/2026", "Hassan Mansouri", "32148-B-6", "Wafa Assurance", "Arrière", "2 400,00", "VALIDE"],
];

// API : à définir — hors cahier des charges (voir roadmap, « Analyse des prix »)
export const tarifs = [
  { name: "Pare-chocs arrière", type: "Pièce", reference: "PC-CL5-AR", client: "Wafa Assurance", vehicle: "Renault Clio V", supplier: "Auto Parts Maroc", price: 1680, date: "24/10/2026" },
  { name: "Pare-chocs arrière", type: "Pièce", reference: "PC-CL5-AR", client: "RMA Assurance", vehicle: "Renault Clio V", supplier: "Carrosserie Plus", price: 1920, date: "19/10/2026" },
  { name: "Pare-chocs arrière", type: "Pièce", reference: "PC-208-AR", client: "Sanlam Maroc", vehicle: "Peugeot 208", supplier: "Atlas Pièces", price: 2140, date: "16/10/2026" },
  { name: "Phare avant droit", type: "Pièce", reference: "PH-DAC-D", client: "AXA Assurance", vehicle: "Dacia Sandero", supplier: "Auto Parts Maroc", price: 1260, date: "12/10/2026" },
  { name: "Aile avant gauche", type: "Pièce", reference: "AI-C3-G", client: "Wafa Assurance", vehicle: "Citroën C3", supplier: "Carrosserie Plus", price: 980, date: "08/10/2026" },
  { name: "Peinture métallisée", type: "Fourniture", reference: "FOU-PEINT-M", client: "AtlantaSanad", vehicle: "Renault Clio V", supplier: "Color Auto", price: 520, date: "25/10/2026" },
  { name: "Ingrédient peinture", type: "Fourniture", reference: "FOU-ING-01", client: "RMA Assurance", vehicle: "Peugeot 208", supplier: "Color Auto", price: 310, date: "21/10/2026" },
  { name: "Kit de fixation", type: "Fourniture", reference: "FOU-FIX-12", client: "Sanlam Maroc", vehicle: "Dacia Sandero", supplier: "Atlas Pièces", price: 145, date: "15/10/2026" },
];

// API : GET /api/rapports-generes (F9)
export const rapportsGeneres = [
  ["RAP-2026-0284", "Rapport d’expertise", "268202-0284", "Hassan Mansouri", "28/10/2026 à 10:42", "PDF"],
  ["SYN-2026-0312", "Synthèse d’indemnisation", "268202-0312", "Salma Idrissi", "27/10/2026 à 16:18", "PDF"],
  ["RAP-2026-0309", "Rapport d’expertise", "268202-0309", "Mehdi El Fassi", "26/10/2026 à 11:07", "PDF"],
] as const;

// API : GET /api/utilisateurs (F11)
export const utilisateurs = [
  ["Samira Benali", "s.benali", "Gestionnaire", "Actif", "Aujourd’hui à 10:42"],
  ["Amine El Khatib", "a.elkhatib", "Administrateur", "Actif", "Aujourd’hui à 09:18"],
  ["Nadia Alaoui", "n.alaoui", "Consultation", "Actif", "Hier à 16:32"],
  ["Youssef Amrani", "y.amrani", "Gestionnaire", "Verrouillé", "24/10/2026 à 11:05"],
  ["Salma Idrissi", "s.idrissi", "Consultation", "Actif", "21/10/2026 à 14:12"],
] as const;

// API : GET /api/journal-audit (P7)
export const evenementsAudit = [
  { date: "28/10/2026", time: "10:42", user: "Samira Benali", initials: "SB", action: "Connexion réussie", detail: "Accès à la plateforme", category: "Connexion", ip: "196.200.176.42" },
  { date: "28/10/2026", time: "09:36", user: "Amine El Khatib", initials: "AE", action: "Rôle modifié", detail: "Nadia Alaoui · Consultation → Gestionnaire", category: "Modification", ip: "196.200.176.18" },
  { date: "28/10/2026", time: "09:18", user: "Amine El Khatib", initials: "AE", action: "Utilisateur créé", detail: "Création du compte de Salma Idrissi", category: "Création", ip: "196.200.176.18" },
  { date: "27/10/2026", time: "16:32", user: "Nadia Alaoui", initials: "NA", action: "Export de données", detail: "Export de la liste des dossiers au format Excel", category: "Export", ip: "105.158.94.61" },
  { date: "27/10/2026", time: "14:07", user: "Samira Benali", initials: "SB", action: "Mot de passe réinitialisé", detail: "Réinitialisation demandée pour Youssef Amrani", category: "Sécurité", ip: "196.200.176.42" },
  { date: "24/10/2026", time: "11:05", user: "Youssef Amrani", initials: "YA", action: "Compte verrouillé", detail: "5 tentatives de connexion échouées", category: "Sécurité", ip: "41.248.132.17" },
];
