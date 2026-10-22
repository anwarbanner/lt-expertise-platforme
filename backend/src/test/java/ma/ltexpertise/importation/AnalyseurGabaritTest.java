package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.NatureRapport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyseurGabaritTest {

    private final AnalyseurGabarit analyseur = new AnalyseurGabarit();

    @ParameterizedTest
    @CsvSource({
            "'RAPPORT D''EXPERTISE : 268202-0001', 268202-0001, DEFINITIF",
            "'RAPPORT D''EXPERTISE REFORME : 268202-0002', 268202-0002, REFORME",
            "'RAPPORT D''EXPERTISE NON DEFINITIF : 0000wca000', 0000wca000, NON_DEFINITIF"
    })
    void litLeNumeroEtLaNatureDuRapport(String titre, String numero, NatureRapport nature) {
        AnalyseGabarit analyse = analyseur.analyser(GenerateurPdf.pdf(titre, 2, "page 2"));

        assertThat(analyse.lisible()).isTrue();
        assertThat(analyse.reconnu()).isTrue();
        assertThat(analyse.gabarit()).isEqualTo(AnalyseurGabarit.GABARIT_V1);
        assertThat(analyse.nombrePages()).isEqualTo(2);
        assertThat(analyse.numeroRapport()).isEqualTo(numero);
        assertThat(analyse.natureRapport()).isEqualTo(nature);
    }

    @Test
    void refuseUnPdfDUneSeulePage() {
        AnalyseGabarit analyse = analyseur.analyser(GenerateurPdf.pdf("RAPPORT D'EXPERTISE : 268202-0003", 1, ""));

        assertThat(analyse.lisible()).isTrue();
        assertThat(analyse.reconnu()).isFalse();
        assertThat(analyse.motif()).contains("1 page(s)");
    }

    @Test
    void refuseUnPdfSansTitreDeRapport() {
        AnalyseGabarit analyse = analyseur.analyser(GenerateurPdf.pdf("FACTURE N 12", 2, "page 2"));

        assertThat(analyse.reconnu()).isFalse();
        assertThat(analyse.motif()).contains("Modèle non reconnu");
    }

    @Test
    void signaleUnPdfIllisible() {
        AnalyseGabarit analyse = analyseur.analyser("%PDF-1.7 contenu tronqué".getBytes(StandardCharsets.US_ASCII));

        assertThat(analyse.lisible()).isFalse();
        assertThat(analyse.reconnu()).isFalse();
        assertThat(analyse.motif()).startsWith("PDF illisible");
    }
}
