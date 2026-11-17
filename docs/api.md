# API REST

Base : `/api`. Format JSON (montants en nombres, dates `AAAA-MM-JJ`, horodatages ISO 8601).
Documentation interactive : `http://localhost:8080/swagger-ui.html` (OpenAPI : `/v3/api-docs`).

**Authentification** : en-tête `Authorization: Bearer <jeton>` sur toutes les routes sauf `/api/auth/*`,
`/actuator/health` et la documentation. **Rôles** : A = administrateur, G = gestionnaire, C = consultation,
« tous » = tout utilisateur connecté.

**Erreurs** : corps `{ "message": "…" }` ; 401 non connecté, 403 rôle insuffisant, 404 introuvable,
409 règle métier refusée (validation impossible, version périmée, génération d'un dossier non validé…).

## Authentification — `/api/auth`

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| POST | `/auth/connexion` | — | Corps `{ identifiant, motDePasse }` → `{ jetonAcces, expireDans, utilisateur }` et cookie de session (HttpOnly). 401 si identifiants faux, 423 si compte verrouillé (5 échecs → 15 min) |
| POST | `/auth/rafraichir` | — (cookie) | Nouveau jeton d'accès ; le cookie est renouvelé (rotation) |
| POST | `/auth/deconnexion` | — (cookie) | Ferme la session, efface le cookie |
| GET | `/auth/moi` | tous | Utilisateur connecté (identifiant, nom, prénom, rôle) |

## Imports — `/api/imports`

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| POST | `/imports` | G | Multipart, champ `fichiers` (un ou plusieurs PDF, 20 Mo max chacun). Un fichier : 201 et récapitulatif ; plusieurs : 202, traitement en arrière-plan, récapitulatif à l'adresse de l'en-tête `Location` |
| GET | `/imports/{lot}` | tous | Récapitulatif d'un lot (UUID) : traités, réussis, à vérifier, prêts, refusés, résultat par fichier |
| GET | `/imports?refuses=false&page=0&size=20` | tous | Historique des imports ; `refuses=true` : seulement les fichiers refusés (liste « Fichiers refusés » du cahier des charges, pas encore d’onglet dédié à l’écran) |
| GET | `/imports/{id}/pdf` | tous | PDF original, contrôlé contre son empreinte (P1) |

## Dossiers — `/api/dossiers`

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/dossiers` | tous | Recherche paginée. Paramètres : `q` (n°, réf. sinistre, immatriculation, nom, réparateur), `statut`, `compagnie`, `zone` (1–8), `du`, `au` (date du rapport), `montantMin`, `montantMax`, `anomalies=true`, `page`, `size` (≤ 100), `sort` (`numeroRapport`, `dateSinistre`, `montantIndemnisation`, `creeLe`) |
| GET | `/dossiers/compteurs` | tous | Nombre de dossiers par statut |
| GET | `/dossiers/compagnies` | tous | Compagnies présentes (filtre) |
| GET | `/dossiers/{numero}` | tous | Fiche complète : données, lignes de détail (`prixCoherent`, `prixCalcule`), contrôles (`conforme`, `bloquant`), champs absents, valeurs du commentaire, zones de choc |
| PUT | `/dossiers/{numero}` | G | Modification (écran de vérification). Corps : champs modifiables + `version` lue (409 si un autre utilisateur a modifié entre-temps) + `zonesChoc` + `prixLignes: [{ ordre, prixUnitaire }]`. Contrôles et statut recalculés ; chaque changement tracé |
| POST | `/dossiers/{numero}/validation` | G | Validation (P3) ; 409 si le dossier n'est pas « Prêt à valider » |
| POST | `/dossiers/validation-groupee` | G | Validation en lot. Corps `{ numeros: [...] }` ou `{ tousLesPrets: true, q, compagnie, zone, du, au, montantMin, montantMax }` → `{ valides, ignores: [{ numeroRapport, motif }], paquets }` (paquets de 500) |
| GET | `/dossiers/{numero}/historique` | tous | Historique du dossier (journal : import, modifications, validation, exports) |

## Statistiques — `/api/statistiques`

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/statistiques/tableau-de-bord?periode=AAAA-MM` | tous | Indicateurs, répartitions par statut, zone, compagnie, mensuel sur 12 mois, alertes, derniers dossiers. Montants à `null` sauf pour l'administrateur (P9) |
| GET | `/statistiques/prix?designation=&type=&marque=&modele=&typeMine=` | tous | Analyse des prix unitaires (min, moyenne, max et par type O/A/R sur toutes les lignes filtrées ; détail paginé). Filtres texte : « contient », sans casse |

## Recherches — `/api/recherche`

Les trois tableaux de recherche (`/statistiques/prix`, `/recherche/pieces`, `/recherche/valeurs`) sont paginés en
base : `page` (à partir de 0), `taille` (25 par défaut, 200 au plus), `tri=<colonne>,asc|desc`, et un filtre par
colonne `f_<colonne>=texte` (cherché dans la valeur affichée, sans casse, accents ni espaces). Réponse : `nombre`
(total filtré), `page`, `taille`, `lignes` (la page). Colonnes : prix `dossier designation type marque modele
typeMine prix date` ; pièces `dossier piece type zone marque modele typeMine mec chassis prix date` ; valeurs
`dossier marque modele typeMine dmec sinistre age achat venale`.

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/recherche/suggestions` | tous | Marques, modèles et types mine présents dans les dossiers (suggestions des filtres) |
| GET | `/recherche/pieces?piece=&type=&zone=&marque=&modele=&typeMine=&annee=&chassis=` | tous | Pièces des rapports (lignes typées O/A/R) ; `annee` = année de MEC, `chassis` = n° de série |
| GET | `/recherche/valeurs?marque=&modele=&typeMine=&mecDu=&mecAu=&ageMin=&ageMax=&achatMin=&achatMax=&venaleMin=&venaleMax=` | tous | Valeur d'achat (valeur à neuf) et valeur vénale lues dans le commentaire ; âge en mois de la DMEC à la date du sinistre |

## Génération et exports (lot 6)

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/rapports/{numero}/pdf` | tous | Rapport PDF régénéré (P10) ; 409 si le dossier n'est pas validé |
| GET | `/rapports/historique?limite=20` | tous | Derniers rapports générés (date, auteur, dossier, assuré, compagnie) |
| GET | `/exports/dossiers/{numero}` | tous | Classeur Excel d'un dossier |
| GET | `/exports/dossiers` | tous | Classeur Excel de **toute** la liste répondant aux filtres (mêmes paramètres que `GET /dossiers`, sans pagination) |
| GET | `/exports/prix?designation=&type=&marque=&modele=&typeMine=` | tous | Classeur Excel de toutes les lignes de prix + synthèse |

Les fichiers sont renvoyés en pièce jointe (`Content-Disposition`, nom de fichier proposé).

## Administration

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/utilisateurs` | A | Comptes (rôle, actif, verrouillé, dernière connexion) |
| POST | `/utilisateurs` | A | Création `{ identifiant, nom, prenom, email, role, motDePasse }` (10 caractères au moins) |
| PUT | `/utilisateurs/{id}` | A | Modification `{ nom, prenom, email, role, actif }` ; un administrateur ne peut ni se désactiver ni retirer son propre rôle |
| POST | `/utilisateurs/{id}/mot-de-passe` | A | Nouveau mot de passe `{ motDePasse }` : déverrouille le compte et ferme ses sessions |
| GET | `/journal-audit?q=&action=&page=&size=` | A | Journal d'audit filtrable et paginé |
| GET | `/journal-audit/export?q=&action=` | A | Export Excel du journal |

Actions du journal : `CONNEXION`, `ECHEC_CONNEXION`, `IMPORT`, `IMPORT_REFUSE`, `CREATION`, `MODIFICATION`,
`CHANGEMENT_STATUT`, `VALIDATION`, `ARCHIVAGE`, `EXPORT_PDF`, `EXPORT_EXCEL`, `GESTION_UTILISATEUR`.

## Supervision

| Route | Description |
|---|---|
| `GET /actuator/health` | État de l'application (public) |
| `GET /actuator/info` | Informations (connecté) |
