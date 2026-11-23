package ma.ltexpertise.generation;

import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.CleCommentaire;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;

import java.util.List;
import java.util.stream.Collectors;

/** Libellés français des valeurs codées, pour les exports Excel et le rapport PDF (mêmes libellés qu'à l'écran). */
final class Libelles {

    private Libelles() {
    }

    static String statut(StatutDossier s) {
        if (s == null) {
            return null;
        }
        return switch (s) {
            case EN_COURS -> "En cours";
            case IMPORTE -> "Importé";
            case A_COMPLETER -> "À compléter";
            case A_VERIFIER -> "À vérifier";
            case PRET_A_VALIDER -> "Prêt à valider";
            case VALIDE -> "Validé";
            case ARCHIVE -> "Archivé";
            case ANNULE -> "Annulé";
        };
    }

    static String procedure(TypeProcedure p) {
        if (p == null) {
            return null;
        }
        return switch (p) {
            case PROCEDURE_CLASSIQUE -> "Procédure classique";
            case PRISE_EN_CHARGE_ET_PLUS_QUE_20000 -> "PEC et plus que 20 000";
            case PLUS_DE_20000 -> "Plus de 20 000";
            case PRISE_EN_CHARGE -> "Prise en charge";
            case WAFA_24 -> "Wafa 24 heures";
        };
    }

    static String poste(PosteMainOeuvre p) {
        return switch (p) {
            case MO_1_TOLERIE -> "MO 1 Tôlerie";
            case MO_2_TOLERIE -> "MO 2 Tôlerie";
            case MO_3_TOLERIE -> "MO 3 Tôlerie";
            case MO_PEINTURE -> "MO Peinture";
            case MO_1_MECANIQUE -> "MO 1 Mécanique";
            case MO_2_MECANIQUE -> "MO 2 Mécanique";
            case MO_3_MECANIQUE -> "MO 3 Mécanique";
        };
    }

    static String fourniture(CategorieFourniture c) {
        return switch (c) {
            case PIECES -> "Pièces";
            case INGREDIENT_PEINTURE -> "Ingrédient peinture";
            case PETITE_FOURNITURE -> "Petite fourniture";
            default -> "Autres";
        };
    }

    static String commentaire(CleCommentaire c) {
        return switch (c) {
            case VALEUR_NEUF_CATALOGUE -> "Valeur à neuf (prix catalogue)";
            case VALEUR_VENALE_FMA -> "Valeur vénale (barème FMA)";
            case DIVERGENCE_USAGE -> "Divergence usage / contrat constatée";
            case PIECES_RECUPERATION_FERRAILLE -> "Pièces de récupération (ferraille) valorisées";
            case PEINTURE_REGLES_ART -> "Peinture retenue selon les règles de l'art";
            case LOCALISATION_CHOC -> "Choc décrit par l'expert";
            case CADRE_EXPERTISE -> "Cadre de l'expertise";
            default -> c.name();
        };
    }

    /** « 7 · Arrière, 8 · Avant ». */
    static String zones(List<Integer> codes) {
        return codes == null || codes.isEmpty() ? null : codes.stream()
                .map(c -> c + " · " + ZoneChoc.depuisCode(c).getLibelle()).collect(Collectors.joining(", "));
    }

    static String ouiNon(Boolean b) {
        return b == null ? null : b ? "Oui" : "Non";
    }
}
