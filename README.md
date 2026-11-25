# LT Expertise — Plateforme des rapports d'expertise

Application interne du cabinet LT Expertise : elle importe les rapports d'expertise automobile au format PDF,
en extrait automatiquement les données, les contrôle, les fait vérifier et valider, puis permet de les rechercher,
d'en tirer des statistiques, de régénérer le rapport en PDF et d'exporter en Excel.

Les données restent sur le serveur du cabinet : base PostgreSQL et PDF originaux dans MinIO, polices et
bibliothèques embarquées, aucun service externe ni cloud (P11).

## En bref

| Étape | Ce que fait l'application |
|---|---|
| **Import** | Dépôt de un à plusieurs centaines de PDF (traités en arrière-plan) ; doublons refusés (même fichier ou même n° de rapport) ; PDF original conservé, verrouillé |
| **Extraction** | En-tête, parties, véhicule, zones de choc (plusieurs possibles), tableau « Évaluation des dommages », détail des pièces de la page 2, commentaire et ses valeurs clés (rapports 2026) |
| **Contrôles** | 28 contrôles de cohérence des montants (heures × taux, HT + TVA = TTC, sommes, page 1 = page 2, indemnisation, montant en lettres, prix de chaque ligne) |
| **Vérification** | PDF à gauche, formulaire à droite ; correction des champs et des prix unitaires faux ; historique de chaque modification |
| **Validation** | Par un gestionnaire ou un administrateur, dossier par dossier ou en lot (paquets de 500) |
| **Recherches** | Analyse des prix, recherche de pièces, valeur vénale / achat : tableaux paginés, triables, colonnes déplaçables par glisser-déposer, filtre par colonne |
| **Exploitation** | Recherche multicritère des dossiers, tableau de bord selon le rôle, rapport PDF régénéré, exports Excel, journal d'audit |

## Rôles

| Rôle | Peut faire | Tableau de bord |
|---|---|---|
| **Administrateur** | Tout : import, vérification, validation, recherches, exports, comptes utilisateurs, journal d'audit | Complet, avec les montants (total, coût moyen, montants par mois) |
| **Gestionnaire** | Import, vérification, modification, validation, recherches, exports | Ses tâches : à vérifier, à compléter, prêts à valider, anomalies, derniers dossiers — **sans montant** |
| **Consultation** | Lecture des dossiers, recherches, exports | Statistiques : zones de choc, compagnies, dossiers par mois et par statut — **sans montant** |

Les droits sont vérifiés par le serveur sur chaque appel (P9) : les montants du tableau de bord ne sont envoyés
qu'à l'administrateur.

## Démarrer l'application (Docker)

Prérequis : **Docker Desktop**, rien d'autre (ni Java, ni Node).

```bash
# Première fois : copier la configuration et l'adapter si besoin (ports, mots de passe, clé des jetons)
cp .env.example .env

# Construire et démarrer : base, stockage des PDF, backend, interface
docker compose up -d --build
```

Ouvrir **http://localhost:8088**, puis se connecter avec un des comptes créés au premier démarrage :

| Identifiant | Rôle | Mot de passe initial |
|---|---|---|
| `admin` | Administrateur | `LtExpertise-Dev-2026` |
| `gestionnaire` | Gestionnaire | `LtExpertise-Dev-2026` |
| `consultant` | Consultation | `LtExpertise-Dev-2026` |

Le mot de passe initial vient de `LT_MOT_DE_PASSE_INITIAL` ; **le changer avant toute mise en service**.

### Commandes utiles

| Commande | Effet |
|---|---|
| `docker compose up -d --build` | Démarre tout ; reconstruit les images après une modification du code |
| `docker compose ps` | État des quatre conteneurs (tous doivent être « healthy ») |
| `docker compose logs -f backend` | Journal du backend |
| `docker compose down` | Arrête tout ; **les données restent** dans les volumes |
| `docker compose up -d postgres minio` | Base et stockage seuls, pour développer (voir plus bas) |

### Les conteneurs

| Conteneur | Rôle | Accès depuis le poste |
|---|---|---|
| `lt-expertise-frontend` | Interface (nginx) ; relaie `/api` vers le backend | http://localhost:8088 (`LT_PORT`) |
| `lt-expertise-backend` | API Spring Boot, extraction, contrôles, génération | Aucun : joint seulement par l'interface |
| `lt-expertise-postgres` | Base PostgreSQL 16 (volume `lt-expertise-pgdata`) | 127.0.0.1:5433 |
| `lt-expertise-minio` | PDF originaux (volume `lt-expertise-minio`) | 127.0.0.1:9000, console 127.0.0.1:9001 |

L'interface est publiée sur `0.0.0.0` (`LT_ADRESSE`) : depuis un autre appareil du réseau, ouvrir
**http://<IP du poste>:8088** (IP donnée par `ipconfig`). La base et MinIO ne sont publiés que sur `127.0.0.1`. Les migrations de base (Flyway)
s'appliquent automatiquement au démarrage du backend.

### Configuration (`.env`)

| Variable | Défaut | Rôle |
|---|---|---|
| `LT_PORT` | `8088` | Port de l'application sur le poste |
| `LT_ADRESSE` | `0.0.0.0` | Adresse d’écoute de l’interface : `0.0.0.0` = réseau local, `127.0.0.1` = poste seul |
| `LT_MOT_DE_PASSE_INITIAL` | `LtExpertise-Dev-2026` | Mot de passe des comptes créés sur une base vide |
| `LT_JWT_SECRET` | vide | Clé des jetons de connexion (≥ 32 caractères). Vide : les utilisateurs se reconnectent après un redémarrage |
| `LT_COOKIE_SECURISE` | `false` | `true` dès que l'application est servie en HTTPS |
| `LT_DOC_API` | `false` | Documentation de l'API (`/swagger-ui.html`), à laisser désactivée en production |
| `DB_*`, `MINIO_*` | voir `.env.example` | Base de données et stockage des PDF |

Avant la mise en service : `LT_JWT_SECRET` fixé, mots de passe initiaux changés, HTTPS et `LT_COOKIE_SECURISE=true`,
sauvegardes quotidiennes de la base et du stockage (voir [docs/exploitation.md](docs/exploitation.md)).

## Développer (rechargement à chaud)

Prérequis : Java 21, Maven 3.9+, Node 22 + pnpm, Docker.

```bash
# 1. PostgreSQL 16 (port 5433) et MinIO (ports 9000 / console 9001) seulement
docker compose up -d postgres minio

# 2. Backend — http://localhost:8080 (santé : /actuator/health, API : /swagger-ui.html)
cd backend
mvn spring-boot:run

# 3. Frontend — http://localhost:5173 (/api est relayé vers le backend)
cd frontend
pnpm install
pnpm dev --port 5173
```

> Ne pas faire tourner en même temps le backend Docker et `mvn spring-boot:run` sur la même base sans le vouloir :
> les deux appliquent les migrations. Après un changement de code, pour la version Docker :
> `docker compose up -d --build`.

> Le Maven Wrapper n'est pas utilisé : ses scripts échouent quand le chemin contient une apostrophe
> (`anwar's desk`). Utiliser Maven installé localement.

## Tests

```bash
cd backend && mvn test          # JUnit 5 + Testcontainers (Docker requis) — 186 tests
cd frontend && pnpm test        # Vitest — 88 tests
cd frontend && pnpm typecheck   # TypeScript
cd frontend && pnpm audit --prod  # failles connues des dépendances
```

Les tests sur les rapports réels (`samples/`) ne s'exécutent que si ce dossier est présent. Les images Docker
sont construites sans relancer les tests : les lancer avant `docker compose up -d --build`.

## Documentation

| Document | Contenu |
|---|---|
| [docs/cahier-des-charge.md](docs/cahier-des-charge.md) | Besoin : principes P1–P11, fonctions F1–F14 (référence contractuelle) |
| [docs/etat-avancement.md](docs/etat-avancement.md) | Ce qui est livré, ce qui reste, décisions prises |
| [docs/architecture.md](docs/architecture.md) | Modules backend et frontend, flux d'un rapport, sécurité, stockage |
| [docs/regles-metier.md](docs/regles-metier.md) | Statuts, contrôles, champs essentiels, zones, valeurs du commentaire, validation |
| [docs/api.md](docs/api.md) | Toutes les routes REST, rôles requis, paramètres (dont pagination des recherches) |
| [docs/guide-utilisateur.md](docs/guide-utilisateur.md) | Mode d'emploi écran par écran, par rôle |
| [docs/exploitation.md](docs/exploitation.md) | Configuration, Docker, sauvegarde, migrations, dépannage |
| [docs/developpement.md](docs/developpement.md) | Conventions, tests, ajouter une migration ou une valeur du commentaire |
| [docs/conception/modele-donnees.md](docs/conception/modele-donnees.md) | Modèle de données détaillé (tables, contraintes) |
| [docs/conception/analyse-frontend-roadmap.md](docs/conception/analyse-frontend-roadmap.md) | Correspondance écrans / API, lots de réalisation |

## Organisation du dépôt

```
docker-compose.yml   Application complète : PostgreSQL, MinIO, backend, interface nginx
.env.example         Modèle de configuration (copier en .env, jamais commité)
backend/             Spring Boot 3 (Java 21) : API REST, extraction PDF, contrôles, recherches, génération,
                     migrations Flyway ; Dockerfile
frontend/            React 19 + TypeScript (Vite) : écrans de l'application ; Dockerfile et nginx.conf
docs/                Cahier des charges, conception, documentation
docs/reference/      Prototype Python de l'extraction (référence validée)
samples/             Rapports PDF réels — JAMAIS commités (voir .gitignore)
```

## Règles du projet

- Montants en `BigDecimal` (précision 12, échelle 2), jamais `double`.
- Schéma de base modifié uniquement par migration Flyway (jamais `ddl-auto=update`).
- Un dossier ne passe à « Validé » que par l'action d'un gestionnaire ou d'un administrateur.
- Code, noms métier et messages en français.
- Aucune donnée réelle dans le dépôt (`samples/`, `.env`).
