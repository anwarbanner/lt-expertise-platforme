package ma.ltexpertise.controle;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;

/**
 * Convertit le montant écrit en toutes lettres du rapport en nombre (F2, contrôle P4) :
 * « cent neuf mille trois cent quatre-vingt treize Dirhams et quatorze centimes » → 109393.14.
 */
public final class MontantEnLettres {

    private static final Map<String, Integer> UNITES = Map.ofEntries(
            Map.entry("zero", 0), Map.entry("un", 1), Map.entry("une", 1), Map.entry("deux", 2),
            Map.entry("trois", 3), Map.entry("quatre", 4), Map.entry("cinq", 5), Map.entry("six", 6),
            Map.entry("sept", 7), Map.entry("huit", 8), Map.entry("neuf", 9), Map.entry("dix", 10),
            Map.entry("onze", 11), Map.entry("douze", 12), Map.entry("treize", 13), Map.entry("quatorze", 14),
            Map.entry("quinze", 15), Map.entry("seize", 16), Map.entry("vingt", 20), Map.entry("vingts", 20),
            Map.entry("trente", 30), Map.entry("quarante", 40), Map.entry("cinquante", 50),
            Map.entry("soixante", 60));

    private MontantEnLettres() {
    }

    /** Montant en dirhams (centimes compris), ou vide si le texte contient un mot inconnu. */
    public static Optional<BigDecimal> versNombre(String texte) {
        if (texte == null || texte.isBlank()) {
            return Optional.empty();
        }
        String normalise = normaliser(texte);
        int dirham = normalise.indexOf("dirham");
        String partieDirhams = dirham >= 0 ? normalise.substring(0, dirham) : normalise;
        String reste = dirham >= 0 ? normalise.substring(dirham).replaceFirst("^dirhams?", "") : "";

        Optional<Long> dirhams = entier(partieDirhams);
        if (dirhams.isEmpty()) {
            return Optional.empty();
        }
        long centimes = 0;
        int centime = reste.indexOf("centime");
        if (centime >= 0) {
            Optional<Long> valeur = entier(reste.substring(0, centime));
            if (valeur.isEmpty() || valeur.get() > 99) {
                return Optional.empty();
            }
            centimes = valeur.get();
        }
        return Optional.of(BigDecimal.valueOf(dirhams.get() * 100 + centimes, 2));
    }

    private static String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase().replaceAll("\\s+", " ").strip();
    }

    /** « deux mille quatre cents » → 2400 ; gère quatre-vingt(s), cent(s), mille, million(s). */
    static Optional<Long> entier(String texte) {
        long total = 0;
        long courant = 0;
        for (String mot : texte.split("[\\s-]+")) {
            if (mot.isEmpty() || mot.equals("et")) {
                continue;
            }
            Integer unite = UNITES.get(mot);
            if (unite != null) {
                // quatre-vingt : 4 × 20
                courant = unite == 20 && courant % 100 == 4 ? courant - 4 + 80 : courant + unite;
            } else if (mot.equals("cent") || mot.equals("cents")) {
                courant = Math.max(courant, 1) * 100;
            } else if (mot.equals("mille")) {
                total += Math.max(courant, 1) * 1_000;
                courant = 0;
            } else if (mot.equals("million") || mot.equals("millions")) {
                total += Math.max(courant, 1) * 1_000_000;
                courant = 0;
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(total + courant);
    }
}
