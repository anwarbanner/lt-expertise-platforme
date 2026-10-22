package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.NatureRapport;

/**
 * Résultat de la lecture minimale d'un PDF à l'import : le fichier est-il lisible, au gabarit connu,
 * et quel est son n° de rapport ? L'extraction complète des champs (F2, F3) se fait ensuite.
 */
public record AnalyseGabarit(boolean lisible, boolean reconnu, int nombrePages, String gabarit,
                             String numeroRapport, NatureRapport natureRapport, String motif) {

    static AnalyseGabarit reconnu(int nombrePages, String gabarit, String numero, NatureRapport nature) {
        return new AnalyseGabarit(true, true, nombrePages, gabarit, numero, nature, null);
    }

    static AnalyseGabarit nonReconnu(int nombrePages, String motif) {
        return new AnalyseGabarit(true, false, nombrePages, null, null, null, motif);
    }

    static AnalyseGabarit illisible(String motif) {
        return new AnalyseGabarit(false, false, 0, null, null, null, motif);
    }
}
