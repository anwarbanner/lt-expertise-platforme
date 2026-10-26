# Développement

## Conventions

- **Français partout** : classes métier (`RapportExpertise`, `Vehicule`), méthodes, messages, commentaires,
  tables et colonnes (`snake_case`).
- **Montants** : `BigDecimal`, colonnes `numeric(12,2)` ; jamais `double`. Pourcentages `numeric(5,2)`.
- **Énumérations** : stockées par leur nom (`@Enumerated(STRING)` + contrainte `CHECK`), sauf les zones de choc
  (code 1–8, `ZoneChocConverter`). Toute énumération Java est reportée dans `frontend/src/domain/`.
- **Schéma** : uniquement par migration Flyway ; Hibernate est en `ddl-auto: validate`.
- **Règles métier P1–P11** du cahier des charges : obligatoires ; les citer dans les commentaires qui les mettent
  en œuvre.
- **Données réelles** : jamais dans le dépôt (`samples/`, `.env`), ni dans les messages de test ou les journaux
  (les tests citent des noms de fichiers et des codes de contrôle, pas de contenu).

## Tests

| Commande | Contenu |
|---|---|
| `cd backend && mvn test` | Tests unitaires et d'intégration (Spring Boot + PostgreSQL et MinIO jetables via Testcontainers ; Docker requis) |
| `cd frontend && pnpm test` | Tests des écrans (Vitest + Testing Library, API simulée) |
| `cd frontend && pnpm typecheck` | Vérification TypeScript |
| `cd frontend && pnpm build` | Construction de production |

Tests notables :

- `EchantillonsExtractionTest` et `ImportPdfIntegrationTest.lesRapportsReelsSontImportesSansErreur` : extraient et
  importent tous les rapports réels de `samples/` (ignorés si le dossier est absent).
- `GenerationIntegrationTest.rapportPdf_dUnRapportReel` : régénère un rapport réel et écrit le PDF et l'image de
  ses pages dans `backend/target/rapport-regenere/` pour contrôle visuel.
- `RapportPdfFactice` (tests) : fabrique un PDF factice au gabarit de l'expert (cabinet masqué, prix faux,
  indemnisation fausse…).
- Frontend : `src/test/api.ts` fournit une API simulée ; les surcharges d'un test s'enregistrent après les
  réponses par défaut (axios-mock-adapter retient le premier gestionnaire qui correspond et remplace celui de même
  adresse).

Règle : chaque fonctionnalité est livrée avec ses tests ; lancer les tests avant de terminer.

## Recettes

### Ajouter une migration

1. Créer `backend/src/main/resources/db/migration/V<n>__description.sql` (n = dernière + 1).
2. Mettre à jour les entités JPA ; `ddl-auto: validate` vérifie la correspondance au démarrage.
3. Pour un calcul impossible en SQL : migration Java dans `backend/src/main/java/db/migration` (voir V8).
4. Mettre à jour `docs/exploitation.md` (liste des migrations) et `docs/conception/modele-donnees.md`.

### Ajouter une valeur clé du commentaire (F12)

1. Ajouter la clé à `CleCommentaire` (Java) et au type `CleCommentaire` (frontend `domain/dossiers.ts`).
2. Migration : élargir la contrainte `ck_valeur_commentaire_cle` (voir V7).
3. Ajouter le motif dans `ExtracteurCommentaire.extraire` (texte normalisé : minuscules, sans accents).
4. Libellé : `generation/Libelles.commentaire` et `frontend/.../ValeursCommentaire.tsx`.
5. Test sur un rapport réel dans `EchantillonsExtractionTest`.
6. Pour les dossiers déjà importés : migration Java qui relit le commentaire (modèle : V8).

### Ajouter un contrôle de cohérence

Dans `ControleCoherenceService.controler` : créer un `Controle` (code stable, libellé, attendu, obtenu, conforme,
bloquant ou non). Associer le champ à surligner dans `ConversionRapportExtrait.CHAMP_PAR_CONTROLE`. Mettre à jour
le décompte dans `docs/regles-metier.md`.

### Ajouter un écran

Créer `frontend/src/features/<module>/<Page>.tsx` (export par défaut), l'ajouter dans `app/routes.ts`
(`ROUTES` et `ROLES_PAR_ECRAN`) et dans le routeur `app/App.tsx` ; appels API typés dans `src/api/`.

## Graphe du code (graphify)

```bash
graphify query "<question>"          # explorer le code
PYTHONHASHSEED=0 python -m graphify update .   # après toute modification (le lanceur casse sur l'apostrophe du chemin)
```

Le graphe (`graphify-out/`) n'est pas commité ; `.graphifyignore` exclut `samples/`.

## Particularités de l'environnement

- Le chemin du projet contient une apostrophe : le Maven Wrapper échoue (utiliser `mvn`), et les scripts shell
  avec heredoc doivent être écrits dans un fichier plutôt que passés en ligne.
- JasperReports 7 : le compilateur d'expressions n'accepte pas les paramètres de type tableau ; `RapportPdf` passe
  chaque cellule de tableau comme paramètre texte (`evaluation_3_5`).
