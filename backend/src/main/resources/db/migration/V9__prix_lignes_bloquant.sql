-- V9 : le contrôle des prix de la page 2 redevient bloquant (demande du 22/10/2026)
-- Un prix unitaire incohérent avec le total de sa ligne empêche la validation : le gestionnaire le corrige à
-- l'écran de vérification (PUT /api/dossiers/{numero}, champ prixLignes), le prix proposé étant déduit du total.

UPDATE resultat_controle SET bloquant = true WHERE code = 'PAGE2_LIGNES_PRIX';

UPDATE rapport_expertise r SET statut = 'A_VERIFIER'
WHERE r.statut IN ('PRET_A_VALIDER', 'A_COMPLETER')
  AND EXISTS (SELECT 1 FROM resultat_controle c WHERE c.rapport_id = r.id AND NOT c.conforme AND c.bloquant);
