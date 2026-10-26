-- V5 : prix unitaire enregistré tel qu'imprimé
-- Deux rapports réels impriment des prix unitaires négatifs (et faux) sur la petite visserie, alors que le total
-- de chaque ligne est juste. Le prix est conservé tel quel (P1, P10) et signalé par le contrôle PAGE2_LIGNES_PRIX,
-- qui fait passer le dossier « à vérifier ». Les autres montants de la ligne restent positifs.

ALTER TABLE ligne_detail DROP CONSTRAINT ck_ligne_detail_positifs;

ALTER TABLE ligne_detail ADD CONSTRAINT ck_ligne_detail_positifs CHECK (
    coalesce(total_fourniture, 0) >= 0 AND coalesce(nombre_heures, 0) >= 0
    AND coalesce(tarif_horaire, 0) >= 0 AND coalesce(montant_mo_ttc, 0) >= 0);
