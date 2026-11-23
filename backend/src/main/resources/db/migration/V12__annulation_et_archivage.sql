-- Annulation d'un import erroné et archivage d'un dossier validé (P6).
-- Rien n'est supprimé : le PDF original (P1) et le journal (P7) restent ; le dossier change de statut.

-- Nouveau statut ANNULE : import erroné écarté par un administrateur, avec son motif
ALTER TABLE rapport_expertise DROP CONSTRAINT ck_rapport_statut;
ALTER TABLE rapport_expertise ADD CONSTRAINT ck_rapport_statut CHECK (statut IN
    ('EN_COURS', 'IMPORTE', 'A_COMPLETER', 'A_VERIFIER', 'PRET_A_VALIDER', 'VALIDE', 'ARCHIVE', 'ANNULE'));

ALTER TABLE rapport_expertise
    ADD COLUMN annule_par       bigint REFERENCES utilisateur (id),
    ADD COLUMN annule_le        timestamptz,
    ADD COLUMN motif_annulation text;

ALTER TABLE rapport_expertise ADD CONSTRAINT ck_rapport_annulation CHECK (statut <> 'ANNULE' OR (
    annule_par IS NOT NULL AND annule_le IS NOT NULL AND motif_annulation IS NOT NULL
    AND length(btrim(motif_annulation)) > 0));

-- Un dossier archivé garde la trace de sa validation
ALTER TABLE rapport_expertise ADD CONSTRAINT ck_rapport_archive_valide CHECK (statut <> 'ARCHIVE' OR (
    valide_par IS NOT NULL AND valide_le IS NOT NULL));

-- Le n° de rapport reste unique parmi les dossiers non annulés : le bon PDF peut être réimporté
-- après l'annulation d'un import erroné portant le même n° (P2).
ALTER TABLE rapport_expertise DROP CONSTRAINT uk_rapport_numero;
CREATE UNIQUE INDEX uk_rapport_numero_actif ON rapport_expertise (numero_rapport) WHERE statut <> 'ANNULE';
CREATE INDEX ix_rapport_numero ON rapport_expertise (numero_rapport);

-- Nouvelle action tracée
ALTER TABLE journal_audit DROP CONSTRAINT ck_journal_audit_action;
ALTER TABLE journal_audit ADD CONSTRAINT ck_journal_audit_action CHECK (action IN ('CONNEXION', 'ECHEC_CONNEXION',
    'IMPORT', 'IMPORT_REFUSE', 'CREATION', 'MODIFICATION', 'CHANGEMENT_STATUT', 'VALIDATION', 'ARCHIVAGE',
    'ANNULATION', 'EXPORT_PDF', 'EXPORT_EXCEL', 'GESTION_UTILISATEUR'));
