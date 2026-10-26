package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.StatutDossier;

/**
 * Résultat de l'import d'un fichier. Les identifiants sont absents quand rien n'a été enregistré ; le statut du
 * dossier et les compteurs de contrôle ne sont renseignés que pour un PDF accepté.
 */
public record ResultatImport(String nomFichier, IssueImport issue, Long importId, Long rapportId,
                             String numeroRapport, String motif, StatutDossier statutDossier,
                             Integer controlesConformes, Integer controlesTotal, Integer champsASaisir) {

    static ResultatImport sansEnregistrement(String nomFichier, IssueImport issue, Long importExistantId,
                                             String motif) {
        return new ResultatImport(nomFichier, issue, importExistantId, null, null, motif, null, null, null, null);
    }
}
