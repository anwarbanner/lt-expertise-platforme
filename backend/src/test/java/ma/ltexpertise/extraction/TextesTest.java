package ma.ltexpertise.extraction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TextesTest {

    @ParameterizedTest
    @ValueSource(strings = {"XXXXXXXXX", " : XXXX ", "XX/XX/XXXX", "NC", "  ", ":"})
    void uneValeurMasqueeOuVideDevientNull(String brut) {
        assertThat(Textes.valeur(brut)).isNull();
    }

    @Test
    void distingueMasqueEtVide() {
        assertThat(Textes.estMasquee("XX/XX/XXXX")).isTrue();
        assertThat(Textes.estMasquee(": XXXXX")).isTrue();
        assertThat(Textes.estMasquee("")).isFalse();
        assertThat(Textes.estMasquee("NC")).isFalse();
        assertThat(Textes.estMasquee("XAVIER")).isFalse();
    }

    @Test
    void garderLaValeurSansLesDeuxPoints() {
        assertThat(Textes.valeur(": GARAGE PRIVE ")).isEqualTo("GARAGE PRIVE");
    }

    @Test
    void litLePremierNombre() {
        assertThat(Textes.nombre(":25 %")).isEqualByComparingTo("25");
        assertThat(Textes.nombre(":2320.00")).isEqualByComparingTo("2320.00");
        assertThat(Textes.nombre("12,50")).isEqualByComparingTo("12.50");
        assertThat(Textes.nombre("NC")).isNull();
    }

    @Test
    void litUneDateValide() {
        assertThat(Textes.date("17/10/2026")).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(Textes.date("XX/XX/XXXX")).isNull();
        assertThat(Textes.date("31/02/2026")).isNull();
    }

    @Test
    void extraitEnGardantLesAccentsDOrigine() {
        Pattern motif = Pattern.compile("reparateur\\s*:\\s*(.*?)\\s*accord");

        assertThat(Textes.extraire("Réparateur : GARAGE ÉLITE Accord du réparateur", motif, 1)).hasValue("GARAGE ÉLITE");
    }

    @Test
    void normaliseLesEtiquettes() {
        assertThat(Textes.norm("Cie d'assurance")).isEqualTo("cie d'assurance");
        assertThat(Textes.norm("N° Police")).isEqualTo("n police");
        assertThat(Textes.norm("Évaluation  des   dommages")).isEqualTo("evaluation des dommages");
    }
}
