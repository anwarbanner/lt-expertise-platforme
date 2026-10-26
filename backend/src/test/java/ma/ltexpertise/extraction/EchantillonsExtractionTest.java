package ma.ltexpertise.extraction;

import ma.ltexpertise.controle.ControleCoherenceService;
import ma.ltexpertise.dossier.referentiel.CleCommentaire;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.importation.AnalyseurGabarit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extraction de tous les rapports réels de samples/ (cahier des charges §9 : au moins 20 rapports).
 * Un rapport est jugé correctement extrait quand ses contrôles de cohérence passent : chaque montant lu est
 * recoupé par au moins un autre (totaux, page 1 / page 2, montant en lettres). Exception connue : deux rapports
 * impriment des prix unitaires faux (le total de chaque ligne reste juste), seul PAGE2_LIGNES_PRIX y échoue.
 * Ignoré si samples/ est absent ; les messages ne citent que des noms de fichiers et des codes de contrôle.
 */
@EnabledIf("echantillonsPresents")
class EchantillonsExtractionTest {

    private static final Path ECHANTILLONS = Path.of("..", "samples");

    private final ExtracteurRapport extracteur = new ExtracteurRapport();
    private final AnalyseurGabarit analyseur = new AnalyseurGabarit();
    private final ControleCoherenceService controles = new ControleCoherenceService();

    static boolean echantillonsPresents() {
        return Files.isDirectory(ECHANTILLONS);
    }

    @Test
    void tousLesRapportsSontExtraitsEtCoherents() throws IOException {
        List<Path> pdfs;
        try (Stream<Path> fichiers = Files.list(ECHANTILLONS)) {
            pdfs = fichiers.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList();
        }
        assertThat(pdfs).hasSizeGreaterThanOrEqualTo(20);

        List<String> anomalies = new ArrayList<>();
        Map<NatureRapport, Integer> parNature = new EnumMap<>(NatureRapport.class);
        int prixUnitairesFaux = 0;
        for (Path pdf : pdfs) {
            byte[] contenu = Files.readAllBytes(pdf);
            RapportExtrait r;
            try {
                r = extracteur.extraire(contenu);
            } catch (GabaritNonReconnuException e) {
                anomalies.add(pdf.getFileName() + " : gabarit non reconnu (" + e.getMessage() + ")");
                continue;
            }
            parNature.merge(r.getNatureRapport(), 1, Integer::sum);
            if (!r.getNumeroRapport().equals(analyseur.analyser(contenu).numeroRapport())) {
                anomalies.add(pdf.getFileName() + " : n° différent de celui lu à l'import");
            }
            List<String> echecs = controles.controler(r).stream().filter(c -> !c.ok()).map(c -> c.code()).toList();
            if (echecs.equals(List.of("PAGE2_LIGNES_PRIX"))) {
                prixUnitairesFaux++;
            } else {
                echecs.forEach(code -> anomalies.add(pdf.getFileName() + " : contrôle " + code + " en échec"));
            }
            if (r.getNatureRapport() != NatureRapport.REFORME && r.getNatureRapport() != NatureRapport.NON_DEFINITIF) {
                if (r.getLignesDetail().isEmpty()) {
                    anomalies.add(pdf.getFileName() + " : aucune ligne de détail en page 2");
                }
            }
        }

        assertThat(anomalies).as("rapports mal extraits").isEmpty();
        assertThat(prixUnitairesFaux).as("rapports aux prix unitaires faux").isEqualTo(2);
        assertThat(parNature.getOrDefault(NatureRapport.REFORME, 0)).isEqualTo(3);
        assertThat(parNature.getOrDefault(NatureRapport.NON_DEFINITIF, 0)).isEqualTo(1);
    }

    /** La page 1 peut lister plusieurs zones (« 7 : Arriere », « 8 : Avant ») : toutes sont lues. */
    @Test
    void lesZonesDeChocMultiplesSontToutesLues() throws IOException {
        assertThat(extraire("C_rapport3_plus de 20000.pdf").getZonesChoc())
                .containsExactly(ZoneChoc.ARRIERE, ZoneChoc.AVANT);
        assertThat(extraire("A_rapport3_CLASSIQUE.pdf").getZonesChoc()).containsExactly(ZoneChoc.AVANT);
        assertThat(extraire("A_rapport1_CLASSIQUE_2026.pdf").getZonesChoc())
                .containsExactly(ZoneChoc.AVANT_LATERAL_GAUCHE);
    }

    /** Tout rapport ayant des lignes de détail rattachées à une zone porte au moins une zone de choc. */
    @Test
    void chaqueZoneDuDetailFigureDansLesZonesDuRapport() throws IOException {
        List<String> anomalies = new ArrayList<>();
        for (Path pdf : pdfs()) {
            RapportExtrait r = extracteur.extraire(Files.readAllBytes(pdf));
            r.getLignesDetail().stream().map(LigneDetailExtraite::getZoneChoc).filter(Objects::nonNull).distinct()
                    .filter(z -> !r.getZonesChoc().contains(z))
                    .forEach(z -> anomalies.add(pdf.getFileName() + " : zone " + z.getCode() + " du détail absente"));
        }
        assertThat(anomalies).isEmpty();
    }

    /** Valeurs clés du commentaire des rapports 2026 (F12). */
    @Test
    void lesValeursDuCommentaire2026SontExtraites() throws IOException {
        assertThat(valeurs("E_rapport1_WAFA 24_2026.pdf")).containsExactly(
                montant(CleCommentaire.VALEUR_NEUF_CATALOGUE, "519000.00"),
                montant(CleCommentaire.VALEUR_VENALE_FMA, "111400.45"),
                texte(CleCommentaire.DIVERGENCE_USAGE, "NON"),
                texte(CleCommentaire.PIECES_RECUPERATION_FERRAILLE, "OUI"),
                texte(CleCommentaire.PEINTURE_REGLES_ART, "OUI"),
                texte(CleCommentaire.LOCALISATION_CHOC, "AVG"),
                texte(CleCommentaire.CADRE_EXPERTISE, "FORFAITAIRE"));
        assertThat(valeurs("A_rapport2_CLASSIQUE_2026.pdf")).contains(
                montant(CleCommentaire.VALEUR_NEUF_CATALOGUE, "733000.00"),
                montant(CleCommentaire.VALEUR_VENALE_FMA, "395820.00"),
                texte(CleCommentaire.LOCALISATION_CHOC, "LAT ARD"),
                texte(CleCommentaire.CADRE_EXPERTISE, "CLASSIQUE"));
    }

    private List<Path> pdfs() throws IOException {
        try (Stream<Path> fichiers = Files.list(ECHANTILLONS)) {
            return fichiers.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList();
        }
    }

    private RapportExtrait extraire(String fichier) throws IOException {
        return extracteur.extraire(Files.readAllBytes(ECHANTILLONS.resolve(fichier)));
    }

    private List<ExtracteurCommentaire.ValeurCle> valeurs(String fichier) throws IOException {
        return ExtracteurCommentaire.extraire(extraire(fichier).getCommentaire());
    }

    private static ExtracteurCommentaire.ValeurCle montant(CleCommentaire cle, String valeur) {
        return new ExtracteurCommentaire.ValeurCle(cle, null, new BigDecimal(valeur));
    }

    private static ExtracteurCommentaire.ValeurCle texte(CleCommentaire cle, String valeur) {
        return new ExtracteurCommentaire.ValeurCle(cle, valeur, null);
    }
}
