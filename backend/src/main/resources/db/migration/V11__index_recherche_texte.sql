-- Recherches « contient » (ILIKE '%…%') de l'analyse des prix, de la recherche de pièces et des valeurs vénales :
-- index trigrammes, utilisables par ILIKE quelle que soit la position du texte cherché. pg_trgm est une extension
-- « de confiance » (PostgreSQL ≥ 13) : le propriétaire de la base peut la créer sans être superutilisateur.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX ix_ligne_detail_designation_trgm ON ligne_detail USING gin (designation gin_trgm_ops);
CREATE INDEX ix_vehicule_marque_trgm ON vehicule USING gin (marque gin_trgm_ops);
CREATE INDEX ix_vehicule_modele_trgm ON vehicule USING gin (modele gin_trgm_ops);
CREATE INDEX ix_vehicule_type_mine_trgm ON vehicule USING gin (type_mine gin_trgm_ops);
CREATE INDEX ix_vehicule_numero_serie_trgm ON vehicule USING gin (numero_serie gin_trgm_ops);

-- Lignes de pièces (typées O/A/R) : seules lues par les trois recherches
CREATE INDEX ix_ligne_detail_pieces ON ligne_detail (rapport_id, type_piece) WHERE type_piece IS NOT NULL;
