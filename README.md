# LT Expertise — Plateforme des rapports d'expertise

Application interne du cabinet LT Expertise : elle importe les rapports d'expertise automobile au format PDF,
en extrait automatiquement les données, les contrôle, les fait vérifier et valider par un gestionnaire, puis
permet de les rechercher, d'en tirer des statistiques, de régénérer le rapport en PDF et d'exporter en Excel.

Les données restent sur le serveur du cabinet (base PostgreSQL et PDF originaux dans MinIO, aucun service cloud).

## En bref

| Étape | Ce que fait l'application |
|---|---|
| **Import** | Dépôt de un à plusieurs centaines de PDF (traités en arrière-plan) ; doublons refusés (même fichier ou même n° de rapport) ; PDF original conservé, verrouillé |
| **Extraction** | En-tête, parties, véhicule, zones de choc (plusieurs possibles), tableau « Évaluation des dommages », détail des pièces de la page 2, commentaire et ses valeurs clés (rapports 2026) |
| **Contrôles** | 28 contrôles de cohérence des montants (heures × taux, HT + TVA = TTC, sommes, page 1 = page 2, indemnisation, montant en lettres, prix de chaque ligne) |
| **Vérification** | PDF à gauche, formulaire à droite ; correction des champs et des prix unitaires faux ; historique de chaque modification |
| **Validation** | Uniquement par un gestionnaire, dossier par dossier ou en lot (paquets de 500) |
| **Exploitation** | Recherche multicritère, tableau de bord, analyse des prix, rapport PDF régénéré, exports Excel, journal d'audit |

## Démarrage rapide (développement)

Prérequis : Java 21, Maven 3.9+, Node 22 + pnpm, Docker.

```bash
# 1. PostgreSQL 16 (port 5433) et MinIO (ports 9000 / console 9001)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend — http://localhost:8080 (santé : /actuator/health, API : /swagger-ui.html)
cd backend
mvn spring-boot:run

# 3. Frontend — http://localhost:5173 (/api est relayé vers le backend)
cd frontend
pnpm install
pnpm dev --port 5173
```

Comptes créés au premier démarrage (base vide) : `admin`, `gestionnaire`, `consultant`, mot de passe
`LtExpertise-Dev-2026` (variable `LT_MOT_DE_PASSE_INITIAL`) — **à changer avant toute mise en service**.

> Le Maven Wrapper n'est pas utilisé : ses scripts échouent quand le chemin contient une apostrophe
> (`anwar's desk`). Utiliser Maven installé localement.

## Tests

```bash
cd backend && mvn test          # JUnit 5 + Testcontainers (Docker requis) — 174 tests
cd frontend && pnpm test        # Vitest — 70 tests
cd frontend && pnpm typecheck   # TypeScript
```

Les tests sur les rapports réels (`samples/`) ne s'exécutent que si ce dossier est présent.

## Documentation

| Document | Contenu |
|---|---|
| [docs/cahier-des-charge.md](docs/cahier-des-charge.md) | Besoin : principes P1–P11, fonctions F1–F14 (référence contractuelle) |
| [docs/etat-avancement.md](docs/etat-avancement.md) | Ce qui est livré, ce qui reste, décisions prises |
| [docs/architecture.md](docs/architecture.md) | Modules backend et frontend, flux d'un rapport, sécurité, stockage |
| [docs/regles-metier.md](docs/regles-metier.md) | Statuts, contrôles, champs essentiels, zones, valeurs du commentaire, validation |
| [docs/api.md](docs/api.md) | Toutes les routes REST, rôles requis, paramètres |
| [docs/guide-utilisateur.md](docs/guide-utilisateur.md) | Mode d'emploi écran par écran, par rôle |
| [docs/exploitation.md](docs/exploitation.md) | Configuration, lancement, sauvegarde, migrations, dépannage |
| [docs/developpement.md](docs/developpement.md) | Conventions, tests, ajouter une migration ou une valeur du commentaire |
| [docs/conception/modele-donnees.md](docs/conception/modele-donnees.md) | Modèle de données détaillé (tables, contraintes) |
| [docs/conception/analyse-frontend-roadmap.md](docs/conception/analyse-frontend-roadmap.md) | Correspondance écrans / API, lots de réalisation |

## Organisation du dépôt

```
backend/        Spring Boot 3 (Java 21) : API REST, extraction PDF, contrôles, génération, migrations Flyway
frontend/       React 19 + TypeScript (Vite) : écrans de l'application
docs/           Cahier des charges, conception, documentation
docs/reference/ Prototype Python de l'extraction (référence validée)
samples/        Rapports PDF réels — JAMAIS commités (voir .gitignore)
docker-compose.dev.yml   PostgreSQL + MinIO pour le développement
```

## Règles du projet

- Montants en `BigDecimal` (précision 12, échelle 2), jamais `double`.
- Schéma de base modifié uniquement par migration Flyway (jamais `ddl-auto=update`).
- Un dossier ne passe à « Validé » que par l'action d'un gestionnaire.
- Code, noms métier et messages en français.
- Aucune donnée réelle dans le dépôt (`samples/`, `.env`).
