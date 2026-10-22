package ma.ltexpertise.importation;

/** Résultat de l'import d'un fichier. Les identifiants sont absents quand rien n'a été enregistré. */
public record ResultatImport(String nomFichier, IssueImport issue, Long importId, Long rapportId,
                             String numeroRapport, String motif) {

    static ResultatImport sansEnregistrement(String nomFichier, IssueImport issue, Long importExistantId,
                                             String motif) {
        return new ResultatImport(nomFichier, issue, importExistantId, null, null, motif);
    }
}
