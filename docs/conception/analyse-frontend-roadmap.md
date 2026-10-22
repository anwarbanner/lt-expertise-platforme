# Frontend — analyse de la maquette Figma et roadmap front ↔ backend

Date : 22/10/2026. Sources : export Figma Make (`frontend/`), cahier des charges (F1–F14, P1–P11),
backend actuel (`backend/`), modèle validé (`docs/conception/modele-donnees.md`).

## 1. Constat sur l'export Figma

| Point | Constat | Conséquence |
|---|---|---|
| Code | Un seul `App.tsx` de 351 lignes, 9 écrans, navigation par `useState` | Pas d'URL par écran, pas de retour arrière, fichier ingérable |
| Données | Toutes en dur dans les composants | Aucune frontière nette entre maquette et API |
| Style | ~440 lignes de CSS fait main (Tailwind importé mais quasi inutilisé), charte navy/rouge cohérente | Design de qualité, à conserver |
| Stack | React 19, Vite 8, TypeScript, Tailwind 4 ; pas de routeur, pas de client API | Écart avec la stack du `CLAUDE.md` (React Router, TanStack Query, react-hook-form + zod, MUI, Recharts, react-pdf) |
| Git | L'export n'avait **jamais été commité** (le commit contient l'ancien squelette) | Risque de perte : à commiter rapidement |
| P11 | `index.css` charge les polices depuis Google Fonts ; `vite.config.ts` contient des options Google Analytics (vides) | Connexion à un service externe interdite : polices à embarquer localement |
| Ergonomie | `body { min-width: 1180px }`, textes de 7 à 9 px | Inutilisable sur tablette (§8 du cahier des charges), lisibilité faible |

## 2. Restructuration réalisée (sans changement visuel)

```
frontend/
├── maquette-figma/App.figma.tsx   ← export d'origine conservé comme référence (hors compilation)
├── src/
│   ├── main.tsx
│   ├── index.css                  ← CSS de la maquette, inchangé (+1 règle pour les liens)
│   ├── app/
│   │   ├── App.tsx                ← routeur (React Router 7) ; RoutesApplication testable
│   │   ├── App.test.tsx           ← 13 tests : chaque route s'affiche, navigation, redirections
│   │   ├── routes.ts              ← adresses des écrans
│   │   └── layout/                ← AppLayout, Sidebar (NavLink), Topbar
│   ├── components/
│   │   ├── ui/                    ← Icon, Button, StatusBadge, PageHead, Kpi, DataTable, Field, InfoCard
│   │   └── charts/                ← BarChart, HorizontalBars (statiques, à remplacer par Recharts)
│   ├── domain/                    ← miroir TypeScript des énumérations et DTO du backend
│   │   ├── referentiels.ts        ← StatutDossier, TypeProcedure, Role, zones de choc…
│   │   └── imports.ts             ← RecapitulatifImport, ResultatImport, ImportFichier (API F1)
│   ├── mocks/donnees.ts           ← toutes les données de démo, chaque bloc annoté de l'endpoint cible
│   └── features/                  ← un dossier par module fonctionnel
│       ├── auth/ConnexionPage.tsx
│       ├── dashboard/TableauDeBordPage.tsx
│       ├── imports/ImportPage.tsx
│       ├── dossiers/{DossiersPage, VerificationPage, DossierDetailPage}.tsx + components/
│       ├── analyse-prix/AnalysePrixPage.tsx
│       ├── generation/GenerationPage.tsx
│       └── utilisateurs/UtilisateursPage.tsx
└── vitest.config.ts
```

Routes : `/connexion`, `/`, `/imports`, `/dossiers`, `/dossiers/:numero`, `/dossiers/:numero/verification`,
`/analyse-prix`, `/generation`, `/utilisateurs`.

Corrections d'alignement faites au passage :
- statuts manipulés par leur code backend (`A_VERIFIER`…), libellés centralisés dans `domain/referentiels.ts` ;
- taille max d'import 10 Mo → **20 Mo** (valeur du backend) ;
- procédure « Contradictoire » → « Procédure classique » (« contradictoire » est un mode d'expertise, pas une procédure) ;
- opérations « Redressage » / « Peinture » → « Échange » / « Réparation » (seules valeurs des PDF).

## 3. Écarts maquette ↔ cahier des charges ↔ backend

| # | Écart | Recommandation | Décision |
|---|---|---|---|
| D1 | Maquette en CSS sur mesure, `CLAUDE.md` prévoit MUI | Garder le design Figma (refaire en MUI perdrait la charte) ; retirer MUI de la stack | à valider |
| D2 | Statut « Prêt à valider » absent de P6 et du backend | L'ajouter à `StatutDossier` (migration V2) : c'est l'état « contrôles OK, en attente du gestionnaire » (P3), filtré sur le tableau de bord | à valider |
| D3 | « Refusé » affiché comme statut de dossier | C'est un statut d'**import** : le garder dans l'écran Import et la liste « Fichiers refusés » seulement | appliqué (type `StatutAffiche`) |
| D4 | Écran « Analyse des prix » hors F1–F14 ; colonnes « référence » et « fournisseur » absentes des PDF | Soit avenant, soit version réduite sur `ligne_detail` (désignation, type O/A/R, prix, compagnie, véhicule) | à valider |
| D5 | Génération : Word, arabe, choix des sections, brouillons | Hors périmètre ; F9 = PDF fidèle au rapport d'origine (P10) + export Excel | à valider |
| D6 | Zones de choc maquette (« Toit », « Latéral droit »…) ≠ codes 1–8 | Utiliser `ZONES_CHOC` ; replacer les pastilles du schéma selon les codes | à faire (lot 4) |
| D7 | Surlignage du champ dans le PDF (écran Vérification) | Nécessite d'enregistrer la position (page, x, y) de chaque valeur à l'extraction : colonne ou table à ajouter | à valider |
| D8 | Lien « Mot de passe oublié » | Pas d'e-mail possible (P11, aucun service externe) : réinitialisation par l'administrateur | à valider |
| D9 | Notifications, centre d'aide, recherche ⌘K | Notifications = alertes d'anomalies ; centre d'aide = guide utilisateur (§9) ; ⌘K = recherche F7 | lots 4–5 |
| D10 | Écrans manquants : fichiers refusés (§3), délais F13, synchro Excel F14, valeurs du commentaire F12 | À maquetter dans Figma avant développement | à faire |
| D11 | Tablette et lisibilité (§8) | Supprimer `min-width: 1180px`, police de base ≥ 12 px, grilles responsives | lot 1 |

## 4. Écrans ↔ API

| Écran | Module | Endpoints | État backend |
|---|---|---|---|
| Connexion | F11 | `POST /api/auth/connexion`, `POST /api/auth/rafraichir`, `GET /api/moi` | ❌ à faire |
| Importer | F1 | `POST /api/imports`, `GET /api/imports?refuses=true`, `GET /api/imports/{id}/pdf` | ✅ disponible |
| Importer (suivi extraction) | F2–F4 | statut d'extraction par fichier (synchrone d'abord, < 5 s par rapport) | ❌ à faire |
| Dossiers | F7, F8 | `GET /api/dossiers?q=&statut=&compagnie=&zone=&du=&au=&montantMin=&page=&tri=` | ❌ (repository prêt : `JpaSpecificationExecutor`) |
| Vérification | F5, P4 | `GET/PUT /api/dossiers/{id}`, `GET /api/dossiers/{id}/controles`, `POST /api/dossiers/{id}/validation` | ❌ (règle P3 prête dans l'entité) |
| Détail | F8, P7 | `GET /api/dossiers/{id}`, `GET /api/dossiers/{id}/historique` | ❌ (journal d'audit prêt) |
| Tableau de bord | F10, F13 | `GET /api/statistiques/{mensuel,statuts,zones,compagnies,delais}`, `GET /api/dossiers/anomalies` | ❌ (requête anomalies prête) |
| Génération | F9 | `POST /api/dossiers/{id}/rapport-pdf`, `GET /api/dossiers/export-excel?…` | ❌ |
| Utilisateurs | F11, P7 | `GET/POST/PUT /api/utilisateurs`, `POST /api/utilisateurs/{id}/reinitialisation`, `GET /api/journal-audit` | ❌ |
| Analyse des prix | hors CDC | `GET /api/statistiques/prix?designation=&compagnie=&marque=` | selon D4 |

## 5. Roadmap

Jalons du cahier des charges : **version de test le 16/11/2026**, **mise en service locale le 07/12/2026**.

### Lot 0 — Structure ✅ (22/10)
Restructuration ci-dessus, routeur, types du domaine, tests de routes. Backend : modèle, import F1, MinIO.

### Lot 1 — Socle et authentification (23/10 → 30/10)
- Front : client API (`fetch` + TanStack Query), proxy Vite `/api` → `localhost:8080`, gestion des erreurs et du 401/403 ;
  écran Connexion branché ; garde de routes et menu selon le rôle ; polices embarquées (P11) ; responsive tablette (D11).
- Back : JWT à durée limitée, verrouillage après 5 échecs, déconnexion sur inactivité, `GET /api/moi`, API sans état (CSRF off, CORS limité au front).
- Tests : MSW pour simuler l'API côté front ; tests MockMvc des droits par rôle côté back.

### Lot 2 — Import réel (31/10 → 04/11)
- Front : glisser-déposer branché sur `POST /api/imports`, récapitulatif du lot, liste « Fichiers refusés » avec motif, visionneuse react-pdf du PDF original.
- Back : déjà prêt ; ajout de la réimportation en un clic d'un fichier refusé (§3).

### Lot 3 — Extraction et vérification (04/11 → 14/11) — cœur du projet
- Back : port Java de `extracteur_rapport.py` (F2, F3), 27 contrôles P4 + règle réforme, statuts À compléter / À vérifier / (Prêt à valider, D2), endpoints dossier + validation, trace de chaque modification (P7).
- Front : formulaires par onglet (react-hook-form + zod), champs en erreur surlignés, panneau des contrôles en direct, bouton Valider actif seulement pour un gestionnaire et des contrôles conformes.

**→ Version de test (16/11)** : import, extraction, vérification, recherche et consultation utilisables.

### Lot 4 — Recherche et consultation (14/11 → 20/11)
- Back : `GET /api/dossiers` multicritère paginé (< 2 s sur plusieurs milliers de dossiers), fiche complète, historique.
- Front : filtres synchronisés avec l'URL, onglets rapides par statut, tri, pagination, recherche globale ⌘K, fiche détail, zones de choc conformes (D6).

### Lot 5 — Tableau de bord, délais et alertes (20/11 → 26/11)
- Back : agrégats mensuels, répartitions, coût moyen, part MO / fournitures (F10), délais par étape et dépassements (F13), alertes.
- Front : graphiques Recharts, filtre de période, liste d'alertes branchée.

### Lot 6 — Génération et exports (24/11 → 30/11)
- Back : PDF JasperReports fidèle au gabarit (P10), uniquement depuis un dossier validé ; export Excel (POI) d'un dossier et d'une liste filtrée ; trace EXPORT_*.
- Front : écran Génération simplifié au périmètre retenu (D5), téléchargement, historique.

### Lot 7 — Utilisateurs, audit, F12, F14 (26/11 → 03/12)
- Comptes, rôles, réinitialisation par l'administrateur, journal d'audit filtrable ; valeurs clés du commentaire (F12) ; synchronisation du fichier Excel des rapports en cours (F14).

### Mise en service (03/12 → 07/12)
- `docker compose` de production : PostgreSQL, MinIO (image durable, question 10 du modèle), backend, nginx servant le front et `/api` en HTTPS sur le réseau interne ; sauvegardes quotidiennes base + bucket ; restauration testée ; recette signée.

### Hors périmètre (avenant éventuel)
Analyse des prix complète (D4), génération Word / arabe (D5), notifications temps réel, mot de passe oublié par e-mail (D8).

## 6. Règles pour la suite
- Le dépôt devient la source de vérité : les nouveaux écrans se maquettent dans Figma, puis s'intègrent dans `features/` en réutilisant `components/ui`.
- Un appel API = une fonction typée dans `features/<module>/api.ts` utilisant les types de `domain/`.
- Les données de `mocks/donnees.ts` disparaissent au fur et à mesure que les endpoints arrivent.
- Toute énumération ajoutée côté Java est reportée dans `domain/referentiels.ts`.
