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
| GET | `/statistiques/tableau-de-bord?periode=AAAA-MM` | tous | Indicateurs, répartitions par statut, zone, compagnie, mensuel sur 12 mois, alertes, derniers dossiers |
| GET | `/statistiques/prix?q=&type=&compagnie=&marque=` | tous | Analyse des prix unitaires (min, moyenne, max, par type O/A/R, 200 lignes les plus récentes) |

## Génération et exports (lot 6)

| Méthode | Route | Rôle | Description |
|---|---|---|---|
| GET | `/rapports/{numero}/pdf` | tous | Rapport PDF régénéré (P10) ; 409 si le dossier n'est pas validé |
| GET | `/rapports/historique?limite=20` | tous | Derniers rapports générés (date, auteur, dossier, assuré, compagnie) |
| GET | `/exports/dossiers/{numero}` | tous | Classeur Excel d'un dossier |
| GET | `/exports/dossiers` | tous | Classeur Excel de **toute** la liste répondant aux filtres (mêmes paramètres que `GET /dossiers`, sans pagination) |
| GET | `/exports/prix?q=&type=&compagnie=&marque=` | tous | Classeur Excel de toutes les lignes de prix + synthèse |

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
