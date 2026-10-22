package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.NatureRapport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Passe l'analyseur sur les rapports réels de {@code samples/} (cahier des charges §9 : au moins 20 rapports).
 * Ces PDF ne sont jamais commités : le test est ignoré là où le dossier est absent (CI, autres postes),
 * et aucune donnée réelle n'apparaît dans ce fichier ni dans ses messages.
 */
@EnabledIf("echantillonsPresents")
class AnalyseurGabaritEchantillonsTest {

    private static final Path ECHANTILLONS = Path.of("..", "samples");

    private final AnalyseurGabarit analyseur = new AnalyseurGabarit();

    static boolean echantillonsPresents() {
        return Files.isDirectory(ECHANTILLONS);
    }

    @Test
    void reconnaitTousLesRapportsReels() throws IOException {
        List<Path> pdfs;
        try (Stream<Path> fichiers = Files.list(ECHANTILLONS)) {
            pdfs = fichiers.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList();
        }
        assertThat(pdfs).hasSizeGreaterThanOrEqualTo(20);

        Map<NatureRapport, Integer> parNature = new EnumMap<>(NatureRapport.class);
        Set<String> numeros = new HashSet<>();
        for (Path pdf : pdfs) {
            AnalyseGabarit analyse = analyseur.analyser(Files.readAllBytes(pdf));
            assertThat(analyse.reconnu()).as("gabarit reconnu pour %s : %s", pdf.getFileName(), analyse.motif())
                    .isTrue();
            assertThat(analyse.nombrePages()).as("pages de %s", pdf.getFileName()).isGreaterThanOrEqualTo(2);
            numeros.add(analyse.numeroRapport());
            parNature.merge(analyse.natureRapport(), 1, Integer::sum);
        }

        assertThat(numeros).as("un n° distinct par rapport").hasSize(pdfs.size());
        // Constats de l'analyse des 25 échantillons (docs/conception/modele-donnees.md §4)
        assertThat(parNature.getOrDefault(NatureRapport.REFORME, 0)).isEqualTo(3);
        assertThat(parNature.getOrDefault(NatureRapport.NON_DEFINITIF, 0)).isEqualTo(1);
    }
}
