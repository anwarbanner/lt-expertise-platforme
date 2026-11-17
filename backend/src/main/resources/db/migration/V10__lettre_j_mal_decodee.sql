-- La police des données des rapports TCPDF décode la lettre J en « Ã » (« JEAN » lu « ÃEAN », châssis « TMAÃ… »).
-- L'extraction rétablit désormais le J (PdfMotsExtractor) ; cette migration corrige les dossiers déjà importés.
-- Toutes les colonnes texte des tables du dossier sont visées ; le journal d'audit, les comptes et les imports
-- (noms de fichiers) ne sont pas modifiés, et le PDF original reste intact (P1).
DO $$
DECLARE
    colonne record;
BEGIN
    FOR colonne IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = current_schema()
          AND c.table_name IN ('rapport_expertise', 'partie', 'vehicule', 'evaluation_main_oeuvre', 'fourniture',
                               'ligne_detail', 'valeur_commentaire', 'resultat_controle')
          AND c.data_type IN ('character varying', 'text')
    LOOP
        EXECUTE format('UPDATE %I SET %I = replace(%I, %L, %L) WHERE strpos(%I, %L) > 0',
                       colonne.table_name, colonne.column_name, colonne.column_name, chr(195), 'J',
                       colonne.column_name, chr(195));
    END LOOP;
END
$$;
