package ma.ltexpertise.extraction;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalisation du texte lu dans les PDF (accents perdus ou mal encodés, valeurs masquées, montants, dates). */
final class Textes {

    private static final Pattern NOMBRE = Pattern.compile("-?\\d+(?:[.,]\\d+)?");
    private static final Pattern MASQUE = Pattern.compile("[Xx/ ]+");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private Textes() {
    }

    /** Minuscules, sans accents, espaces réduites : pour reconnaître les étiquettes. */
    static String norm(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.replaceAll("[^\\p{ASCII}]", "").toLowerCase().replaceAll("\\s+", " ").strip();
    }

    /**
     * Comme {@link #norm} mais caractère pour caractère, sans changer la longueur : une position trouvée
     * dans le texte « à plat » vaut dans le texte d'origine, ce qui permet de garder les accents des valeurs.
     */
    static String plat(String texte) {
        StringBuilder resultat = new StringBuilder(texte.length());
        for (int i = 0; i < texte.length(); i++) {
            String decompose = Normalizer.normalize(String.valueOf(texte.charAt(i)), Normalizer.Form.NFD);
            resultat.append(Character.toLowerCase(decompose.charAt(0)));
        }
        return resultat.toString();
    }

    /** Groupe {@code groupe} du motif cherché sur le texte à plat, restitué avec la casse et les accents d'origine. */
    static Optional<String> extraire(String brut, Pattern motifSurPlat, int groupe) {
        Matcher m = motifSurPlat.matcher(plat(brut));
        if (!m.find() || m.group(groupe) == null) {
            return Optional.empty();
        }
        return Optional.of(brut.substring(m.start(groupe), m.end(groupe)).strip());
    }

    /** {@code true} si la valeur est masquée dans le PDF (XXXX, XX/XX/XXXX). */
    static boolean estMasquee(String valeur) {
        if (valeur == null) {
            return false;
        }
        String v = nettoyer(valeur);
        return !v.isEmpty() && MASQUE.matcher(v).matches();
    }

    /** Valeur texte utilisable, ou {@code null} si elle est vide, masquée ou « NC ». */
    static String valeur(String brut) {
        if (brut == null) {
            return null;
        }
        String v = nettoyer(brut);
        if (v.isEmpty() || MASQUE.matcher(v).matches() || v.equalsIgnoreCase("NC")) {
            return null;
        }
        return v;
    }

    /** Premier nombre du texte (« :0.00 », « 1200.00 », « :25 % »), ou {@code null}. */
    static BigDecimal nombre(String texte) {
        if (texte == null) {
            return null;
        }
        Matcher m = NOMBRE.matcher(texte);
        return m.find() ? new BigDecimal(m.group().replace(',', '.')) : null;
    }

    /** Date jj/mm/aaaa valide, ou {@code null} (masquée, vide ou impossible). */
    static LocalDate date(String texte) {
        String v = valeur(texte);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v, DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String nettoyer(String valeur) {
        return valeur.replaceAll("^[\\s:]+|[\\s:]+$", "");
    }
}
