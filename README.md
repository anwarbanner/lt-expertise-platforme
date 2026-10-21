# LT Expertise — Plateforme des rapports d'expertise

Monorepo : `backend/` (Spring Boot 3, Java 21) · `frontend/` (Vite + React + TypeScript).

## Lancement en développement

```bash
# 1. Base PostgreSQL 16 (port hôte 5433, volume persistant)
docker compose -f docker-compose.dev.yml up -d

# 2. Backend — http://localhost:8080 (santé : /actuator/health)
cd backend
mvn spring-boot:run

# 3. Frontend — http://localhost:5173 (/api est relayé vers le backend)
cd frontend
npm install
npm run dev
```

> Le Maven Wrapper n'est pas utilisé : ses scripts échouent quand le chemin contient
> une apostrophe (`anwar's desk`). Utiliser Maven 3.9+ installé localement.

## Tests

```bash
cd backend && mvn test     # JUnit 5 + Testcontainers (Docker requis)
cd frontend && npm test    # Vitest
```

## Configuration

Variables (valeurs par défaut de dev) : `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT`.
Voir `.env.example` pour docker compose.
