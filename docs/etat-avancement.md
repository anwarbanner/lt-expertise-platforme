# État d'avancement

État au 22/10/2026. Légende : ✅ livré et testé · 🟡 partiel · ⬜ à faire.

## Fonctions (cahier des charges §6)

| | Fonction | État | Détail |
|---|---|---|---|
| F1 | Import PDF | ✅ | Glisser-déposer, lots en arrière-plan, doublons par empreinte et n° (P2), original verrouillé (P1), refus motivés |
| F2 | Extraction des champs | ✅ | Gabarit « tcpdf-expertise-v1 », 25 rapports réels extraits sans erreur ; montant en lettres converti |
| F3 | Extraction des tableaux | ✅ | Évaluation des dommages, détail page 2 par zone et famille ; plusieurs zones de choc |
| F4 | Nettoyage et validation | ✅ | 28 contrôles, statut calculé ; contrôles bloquants ou avertissements |
| F5 | Contrôle et modification manuelle | 🟡 | PDF + formulaire, champs en erreur surlignés, correction des prix, historique. Reste : surligner la valeur **dans le PDF** (D7) |
| F6 | Stockage | ✅ | PostgreSQL, migrations Flyway V1–V9 |
| F7 | Recherche et filtrage | 🟡 | Recherche, période, compagnie, statut, zone, montant, anomalies. Reste : filtre par **procédure** |
| F8 | Consultation | ✅ | Liste paginée et triable, fiche complète, PDF original, historique |
| F9 | Génération | ✅ | Rapport PDF fidèle (JasperReports), exports Excel (dossier, liste, prix, journal), historique |
| F10 | Graphiques et statistiques | 🟡 | Mois, statut, zone, compagnie, coût moyen, part MO / fournitures, alertes. Reste : répartition par **procédure** |
| F11 | Utilisateurs | ✅ | Connexion sécurisée, 3 rôles, comptes gérés par l'administrateur, verrouillage, sessions |
| F12 | Valeurs du commentaire | 🟡 | 7 valeurs extraites, affichées, exportées. Reste : **filtres** et **statistiques** sur ces valeurs |
| F13 | Délais de traitement | ⬜ | Durée mission → rapport, délais par étape, dépassements |
| F14 | Synchronisation Excel | 🟡 | Rattachement d'un PDF à un dossier « En cours » prêt côté import. Reste : import / export du fichier Excel de suivi, correspondance des colonnes (fichier exemple attendu) |

## Principes (cahier des charges §4)

| | Principe | Mise en œuvre |
|---|---|---|
| P1 | Le PDF original fait foi | Stockage MinIO avec Object Lock ; empreinte SHA-256 contrôlée à chaque lecture ; jamais modifié |
| P2 | Un rapport, un dossier | Unicité du n° de rapport ; fichier identique ignoré |
| P3 | Validation humaine | Seul un gestionnaire actif valide (unitaire ou en lot), contrainte en base |
| P4 | Cohérence des montants | `ControleCoherenceService` (28 contrôles) |
| P5 | TVA | Indemnisation sur HT si TVA récupérable, sur TTC sinon |
| P6 | Cycle de vie | Statuts En cours → … → Validé → Archivé ; « Prêt à valider » ajouté (D2) |
| P7 | Traçabilité | Journal d'audit en ajout seul (triggers), consultable et exportable |
| P8 | Référentiels | Énumérations Java / TypeScript : postes, types de pièces, zones 1–8, contrats |
| P9 | Accès selon le rôle | `@PreAuthorize` sur chaque route ; écrans masqués selon le rôle |
| P10 | Rapport régénéré fidèle | PDF uniquement depuis un dossier validé, structure d'origine |
| P11 | Base interne | Tout sur le serveur du cabinet, ports locaux, aucun service externe |

## Décisions prises

| Date | Décision |
|---|---|
| D1–D9 | Voir `conception/analyse-frontend-roadmap.md` §3 (design sur mesure, statut « Prêt à valider », périmètre de la génération, réinitialisation du mot de passe par l'administrateur…) |
| 22/10/2026 | Un champ absent du PDF n'est plus bloquant : l'application extrait, la saisie est facultative ; seuls montant d'indemnisation, procédure et TVA récupérable restent obligatoires |
| 22/10/2026 | Un rapport peut avoir plusieurs zones de choc |
| 22/10/2026 | Les prix unitaires faux imprimés par le logiciel de l'expert bloquent la validation et se corrigent à la main (prix proposé d'après le total) |
| 22/10/2026 | Valeurs du commentaire 2026 retenues : valeur à neuf, valeur vénale FMA, divergence d'usage, pièces de ferraille, peinture, choc décrit, cadre de l'expertise |
| 22/10/2026 | Validation en lot (dossiers cochés ou tous les prêts filtrés), par paquets de 500 |

## Reste à faire

1. **Lot 7** : F12 (filtres, statistiques), F14 (synchronisation Excel — fichier exemple à fournir).
2. **Compléments** : F13 (délais), filtre et statistique par procédure, surlignage dans le PDF (D7).
3. **Mise en service** : `docker compose` de production, HTTPS, sauvegardes quotidiennes, recette ; vérifier la
   génération PDF depuis le JAR de production.
4. **Commit** : le travail du 22/10/2026 n'est pas encore commité.
