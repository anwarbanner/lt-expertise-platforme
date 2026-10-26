-- V2 : vétusté HT
-- Quand la TVA est récupérable, le rapport imprime « Vétusté HT » au lieu de « Vétusté TTC »
-- (constat de l'extraction sur les rapports réels, docs/conception/modele-donnees.md, question 11).

ALTER TABLE rapport_expertise ADD COLUMN vetuste_ht numeric(12, 2);

ALTER TABLE rapport_expertise
    ADD CONSTRAINT ck_rapport_vetuste_ht_positive CHECK (vetuste_ht IS NULL OR vetuste_ht >= 0);
