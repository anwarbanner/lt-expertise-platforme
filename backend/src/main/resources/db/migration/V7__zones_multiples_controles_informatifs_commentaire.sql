-- V7 : retours du 22/10/2026 sur l'extraction
--
-- 1. Plusieurs zones de choc par rapport : la page 1 peut en lister plusieurs (« 7 : Arriere », « 8 : Avant »).
--    La colonne unique zone_choc devient la table rapport_zone_choc.
-- 2. Contrôles bloquants ou informatifs : seul un résultat bloquant non conforme empêche la validation.
--    - un champ absent du PDF est informatif (l'application extrait, la saisie n'est pas obligatoire), sauf les
--      données qui fondent l'indemnisation (ck_rapport_complet_si_valide) ;
--    - PAGE2_LIGNES_PRIX est informatif : le logiciel de l'expert imprime parfois un prix unitaire faux (négatif)
--      alors que le total de la ligne est juste ; les sommes de la page 2 restent contrôlées (bloquantes).
-- 3. Nouvelles valeurs clés du commentaire (F12), relevées sur les rapports 2026.

-- 1. Zones de choc
CREATE TABLE rapport_zone_choc (
    rapport_id bigint   NOT NULL REFERENCES rapport_expertise (id) ON DELETE CASCADE,
    zone_choc  smallint NOT NULL,
    PRIMARY KEY (rapport_id, zone_choc),
    CONSTRAINT ck_rapport_zone_choc_code CHECK (zone_choc BETWEEN 1 AND 8)
);

CREATE INDEX ix_rapport_zone_choc_zone ON rapport_zone_choc (zone_choc);

INSERT INTO rapport_zone_choc (rapport_id, zone_choc)
SELECT id, zone_choc FROM rapport_expertise WHERE zone_choc IS NOT NULL;

-- Rapports dont la page 1 listait plusieurs zones (non lues jusqu'ici) : zones des sections de la page 2
INSERT INTO rapport_zone_choc (rapport_id, zone_choc)
SELECT DISTINCT l.rapport_id, l.zone_choc
FROM ligne_detail l
JOIN rapport_expertise r ON r.id = l.rapport_id
WHERE r.zone_choc IS NULL AND l.zone_choc IS NOT NULL;

DROP INDEX ix_rapport_zone_choc;
ALTER TABLE rapport_expertise DROP CONSTRAINT ck_rapport_zone_choc;
ALTER TABLE rapport_expertise DROP COLUMN zone_choc;

-- 2. Contrôles bloquants ou informatifs
ALTER TABLE resultat_controle ADD COLUMN bloquant boolean NOT NULL DEFAULT true;

UPDATE resultat_controle SET bloquant = false
WHERE code = 'PAGE2_LIGNES_PRIX'
   OR (type = 'CHAMP_MANQUANT' AND champ NOT IN ('montantIndemnisation', 'typeProcedure', 'tvaRecuperable'));

DROP INDEX ix_resultat_controle_anomalies;
CREATE INDEX ix_resultat_controle_anomalies ON resultat_controle (rapport_id) WHERE NOT conforme AND bloquant;

-- Dossiers « à compléter » ou « à vérifier » sans anomalie bloquante : prêts à valider
UPDATE rapport_expertise r SET statut = 'PRET_A_VALIDER'
WHERE r.statut IN ('A_COMPLETER', 'A_VERIFIER')
  AND NOT EXISTS (SELECT 1 FROM resultat_controle c WHERE c.rapport_id = r.id AND NOT c.conforme AND c.bloquant);

-- 3. Valeurs clés du commentaire
ALTER TABLE valeur_commentaire DROP CONSTRAINT ck_valeur_commentaire_cle;
ALTER TABLE valeur_commentaire ADD CONSTRAINT ck_valeur_commentaire_cle CHECK (cle IN ('VALEUR_NEUF_CATALOGUE',
    'VALEUR_VENALE_FMA', 'MONTANT_DEVIS', 'ESTIMATION_SANS_DEVIS', 'CADRE_EXPERTISE', 'LOCALISATION_CHOC',
    'EXPERT_CONFRERE', 'MODE_EXPERTISE_CONFRERE', 'DATE_ACCORD', 'DIVERGENCE_USAGE', 'PIECES_RECUPERATION_FERRAILLE',
    'PEINTURE_REGLES_ART'));
