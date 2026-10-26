package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.StatutDossier;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Récapitulatif d'un dépôt (import en lot) : réussis, dont à compléter / à vérifier / prêts à valider,
 * refusés (doublons, modèle non reconnu, fichier invalide) et en erreur technique.
 */
public record RecapitulatifLot(UUID lot, LotImport.Statut statut, int total, int traites, int reussis,
                               int aCompleter, int aVerifier, int pretsAValider, int refuses, int enErreur,
                               OffsetDateTime creeLe, OffsetDateTime termineLe, List<ResultatImport> resultats) {

    static RecapitulatifLot de(LotImport lot) {
        List<ResultatImport> resultats = lot.getResultats().stream().map(ResultatLotImport::versResultat).toList();
        int reussis = (int) resultats.stream().filter(r -> r.issue().estAccepte()).count();
        int enErreur = (int) resultats.stream().filter(r -> r.issue() == IssueImport.ERREUR).count();
        return new RecapitulatifLot(lot.getId(), lot.getStatut(), lot.getNombreFichiers(), resultats.size(), reussis,
                compter(resultats, StatutDossier.A_COMPLETER), compter(resultats, StatutDossier.A_VERIFIER),
                compter(resultats, StatutDossier.PRET_A_VALIDER), resultats.size() - reussis - enErreur, enErreur,
                lot.getCreeLe(), lot.getTermineLe(), resultats);
    }

    private static int compter(List<ResultatImport> resultats, StatutDossier statut) {
        return (int) resultats.stream().filter(r -> r.statutDossier() == statut).count();
    }
}
