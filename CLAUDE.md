# Projet LT Expertise — Plateforme des rapports d'expertise

Lire docs/cahier-des-charges.md avant toute tâche fonctionnelle.

## Stack
- Backend : Java 21, Spring Boot 3, Maven, Spring Data JPA/Hibernate, Flyway,
  Spring Security + JWT, Apache PDFBox 3, JasperReports, Apache POI, springdoc-openapi
- Frontend : React + TypeScript (Vite), React Router, TanStack Query, react-hook-form + zod,
  MUI, Recharts, react-pdf
- Base : PostgreSQL 16 (Docker en dev)
- Tests : JUnit 5, Testcontainers, Vitest

## Règles
- Montants en BigDecimal (precision 12, scale 2), jamais double.
- Toute évolution du schéma passe par une migration Flyway, jamais ddl-auto=update.
- Les principes métier P1 à P11 du cahier des charges sont obligatoires.
- Un dossier ne passe à VALIDE que par une action d'un GESTIONNAIRE.
- Code, noms métier et messages en français (entités : RapportExpertise, Vehicule…).
- Ne jamais committer le dossier samples/ ni de données réelles.
- Chaque fonctionnalité livrée avec ses tests ; lancer les tests avant de terminer.
- Référence d'extraction : docs/reference/extracteur_rapport.py (prototype validé).