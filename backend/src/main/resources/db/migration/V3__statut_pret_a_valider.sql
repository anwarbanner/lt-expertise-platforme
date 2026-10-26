-- V3 : statut « Prêt à valider »
-- Dossier dont tous les contrôles de cohérence passent et dont aucun champ ne manque : il attend la validation
-- d'un gestionnaire (P3). Jamais VALIDE automatiquement.

ALTER TABLE rapport_expertise DROP CONSTRAINT ck_rapport_statut;

ALTER TABLE rapport_expertise ADD CONSTRAINT ck_rapport_statut CHECK (statut IN
    ('EN_COURS', 'IMPORTE', 'A_COMPLETER', 'A_VERIFIER', 'PRET_A_VALIDER', 'VALIDE', 'ARCHIVE'));
