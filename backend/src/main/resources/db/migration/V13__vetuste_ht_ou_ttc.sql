-- V13 : la vétusté d'un rapport est soit TTC, soit HT
-- Le rapport imprime « Vétusté TTC » ou « Vétusté HT » ; ce choix fixe le calcul de l'indemnisation
-- (total général TTC − vétusté TTC, ou total général HT − vétusté HT). Les deux ne coexistent jamais.

-- L'extraction n'en lit qu'une ; si une saisie en a ajouté une seconde, on garde la vétusté HT imprimée.
UPDATE rapport_expertise SET vetuste_ttc = NULL
WHERE vetuste_ttc IS NOT NULL AND vetuste_ht IS NOT NULL;

ALTER TABLE rapport_expertise
    ADD CONSTRAINT ck_rapport_vetuste_ht_ou_ttc CHECK (vetuste_ttc IS NULL OR vetuste_ht IS NULL);
