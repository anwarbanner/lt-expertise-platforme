package ma.ltexpertise.importation;

import java.util.List;
import java.util.UUID;

/** Récapitulatif d'un dépôt d'un ou plusieurs PDF (import en lot). */
public record RecapitulatifImport(UUID lot, int total, int acceptes, int refuses, int enErreur,
                                  List<ResultatImport> resultats) {

    static RecapitulatifImport de(UUID lot, List<ResultatImport> resultats) {
        int acceptes = (int) resultats.stream().filter(r -> r.issue().estAccepte()).count();
        int enErreur = (int) resultats.stream().filter(r -> r.issue() == IssueImport.ERREUR).count();
        return new RecapitulatifImport(lot, resultats.size(), acceptes, resultats.size() - acceptes - enErreur,
                enErreur, List.copyOf(resultats));
    }
}
