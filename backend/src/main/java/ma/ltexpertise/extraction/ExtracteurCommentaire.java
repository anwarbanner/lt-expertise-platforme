package ma.ltexpertise.extraction;

import ma.ltexpertise.dossier.referentiel.CleCommentaire;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Valeurs clés du commentaire de l'expert (F12), d'après les phrases types des rapports 2026 :
 * <ul>
 *   <li>« LA VALEUR A NEUF DU VEHICULE A ETE RETENUE SUR LA BASE DU PRIX CATALOGUE DISPONIBLE : 519000.00 » ;</li>
 *   <li>« LA VALEUR VENALE DU VEHICULE AU JOUR DU SINISTRE SELON LE BAREME FMA : 111400.45 » ;</li>
 *   <li>« NOUS N AVONS PAS CONSTATE DE DIVERGENCE PARTICULIERE ENTRE L USAGE DU VEHICULE ET LES ELEMENTS DECLARES
 *       AU CONTRAT » : divergence NON (OUI si l'expert en constate une) ;</li>
 *   <li>« L ENSEMBLE DES PIECES DE RECUPERATION ACHETEES AUPRES DE LA FERRAILLE A ETE VALORISE… » : OUI ;</li>
 *   <li>« LA MAIN-D OEUVRE AINSI QUE LES PRODUITS ET FOURNITURES NECESSAIRES AUX TRAVAUX DE PEINTURE ONT ETE RETENUS
 *       CONFORMEMENT AUX REGLES DE L ART… » : OUI ;</li>
 *   <li>« LE VEHICULE A SUBI UN CHOC LAT ARD » : localisation « LAT ARD » ;</li>
 *   <li>« DOSSIER TRAITE DANS LE CADRE D'UNE EXPERTISE FORFAITAIRE » : cadre « FORFAITAIRE ».</li>
 * </ul>
 * Une phrase absente ne produit pas de valeur. Le texte complet du commentaire reste conservé dans le rapport.
 */
public final class ExtracteurCommentaire {

    private static final String NOMBRE = "(\\d+(?:[.,]\\d+)?)";
    private static final Pattern VALEUR_NEUF = Pattern.compile("valeur a neuf du vehicule[^:]*:\\s*" + NOMBRE);
    private static final Pattern VALEUR_VENALE = Pattern.compile("valeur venale du vehicule[^:]*:\\s*" + NOMBRE);
    private static final Pattern PAS_DE_DIVERGENCE = Pattern.compile("(?:n ?'?\\s?avons|n ?'?\\s?a) pas constate de divergence");
    private static final Pattern DIVERGENCE = Pattern.compile("constate (?:une |des )?divergences?");
    private static final Pattern FERRAILLE = Pattern.compile("pieces de recuperation achetees aupres de la ferraille");
    private static final Pattern PEINTURE = Pattern.compile("travaux de peinture ont ete retenus conformement aux regles de l ?'?\\s?art");
    private static final Pattern CHOC = Pattern.compile("vehicule a subi un choc ([a-z0-9 /]+?)\\s*(?:-|\\.|$)");
    private static final Pattern CADRE = Pattern.compile("dans le cadre d ?'?\\s?une expertise ([a-z]+)");

    private ExtracteurCommentaire() {
    }

    /** Valeur clé lue dans le commentaire : un montant ou un texte (OUI / NON, libellé). */
    public record ValeurCle(CleCommentaire cle, String texte, BigDecimal montant) {
    }

    public static List<ValeurCle> extraire(String commentaire) {
        List<ValeurCle> valeurs = new ArrayList<>();
        if (commentaire == null || commentaire.isBlank()) {
            return valeurs;
        }
        String t = Textes.norm(commentaire);
        montant(t, VALEUR_NEUF).ifPresent(m -> valeurs.add(new ValeurCle(CleCommentaire.VALEUR_NEUF_CATALOGUE, null, m)));
        montant(t, VALEUR_VENALE).ifPresent(m -> valeurs.add(new ValeurCle(CleCommentaire.VALEUR_VENALE_FMA, null, m)));
        if (PAS_DE_DIVERGENCE.matcher(t).find()) {
            valeurs.add(new ValeurCle(CleCommentaire.DIVERGENCE_USAGE, "NON", null));
        } else if (t.contains("usage du vehicule") && DIVERGENCE.matcher(t).find()) {
            valeurs.add(new ValeurCle(CleCommentaire.DIVERGENCE_USAGE, "OUI", null));
        }
        if (FERRAILLE.matcher(t).find()) {
            valeurs.add(new ValeurCle(CleCommentaire.PIECES_RECUPERATION_FERRAILLE, "OUI", null));
        }
        if (PEINTURE.matcher(t).find()) {
            valeurs.add(new ValeurCle(CleCommentaire.PEINTURE_REGLES_ART, "OUI", null));
        }
        texte(t, CHOC).ifPresent(v -> valeurs.add(new ValeurCle(CleCommentaire.LOCALISATION_CHOC, v, null)));
        texte(t, CADRE).ifPresent(v -> valeurs.add(new ValeurCle(CleCommentaire.CADRE_EXPERTISE, v, null)));
        return valeurs;
    }

    private static Optional<BigDecimal> montant(String t, Pattern motif) {
        Matcher m = motif.matcher(t);
        return m.find()
                ? Optional.of(new BigDecimal(m.group(1).replace(',', '.')).setScale(2, RoundingMode.HALF_UP))
                : Optional.empty();
    }

    private static Optional<String> texte(String t, Pattern motif) {
        Matcher m = motif.matcher(t);
        return m.find() ? Optional.of(m.group(1).strip().toUpperCase(Locale.ROOT)) : Optional.empty();
    }
}
