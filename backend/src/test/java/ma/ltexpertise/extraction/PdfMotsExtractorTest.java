package ma.ltexpertise.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PdfMotsExtractorTest {

    private static final float HAUTEUR_PAGE = 842;

    @Test
    void decoupeSurLesEspacesEtSurLesEcartsEntreCellules() throws IOException {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float largeurCabinet = police.getStringWidth("CABINET") / 1000 * 10;
        byte[] pdf = pdf(contenu -> {
            texte(contenu, police, 16, 20, "CABINET : LT EXPERTISES");
            // Cellule voisine à 6 pt : deux mots distincts malgré l'absence d'espace.
            texte(contenu, police, 16 + largeurCabinet + 6, 40, "SUITE");
            texte(contenu, police, 16, 40, "CABINET");
        });

        List<Ligne> lignes = lignes(pdf);

        assertThat(lignes).hasSize(2);
        assertThat(lignes.get(0).mots()).extracting(Mot::texte).containsExactly("CABINET", ":", "LT", "EXPERTISES");
        assertThat(lignes.get(0).mots().getFirst().x()).isCloseTo(16, within(0.5));
        assertThat(lignes.get(1).mots()).extracting(Mot::texte).containsExactly("CABINET", "SUITE");
    }

    @Test
    void regroupeEnUneLigneLesMotsDecalesDeMoinsDe2Points5() throws IOException {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        byte[] pdf = pdf(contenu -> {
            texte(contenu, police, 49, 414, "MO");
            texte(contenu, police, 160, 415.5f, "12.00");
            texte(contenu, police, 267, 420, "60.00");
        });

        List<Ligne> lignes = lignes(pdf);

        assertThat(lignes).extracting(Ligne::texte).containsExactly("MO 12.00", "60.00");
    }

    @Test
    void retablitLaLettreJLueCommeATilde() throws IOException {
        // La police des rapports TCPDF décode J en « Ã »
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        byte[] pdf = pdf(contenu -> texte(contenu, police, 49, 100, "ÃEAN TMAÃB81 41789-Ã-77"));

        assertThat(lignes(pdf).getFirst().texte()).isEqualTo("JEAN TMAJB81 41789-J-77");
    }

    @Test
    void conserveLesAccents() throws IOException {
        PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        byte[] pdf = pdf(contenu -> texte(contenu, police, 49, 100, "Évaluation des dommages"));

        assertThat(lignes(pdf).getFirst().texte()).isEqualTo("Évaluation des dommages");
    }

    private static List<Ligne> lignes(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PdfMotsExtractor().lignes(document, 1);
        }
    }

    /** Écrit un texte, {@code y} mesuré depuis le haut de la page comme dans les rapports. */
    private static void texte(PDPageContentStream contenu, PDType1Font police, float x, float y, String texte)
            throws IOException {
        contenu.beginText();
        contenu.setFont(police, 10);
        contenu.newLineAtOffset(x, HAUTEUR_PAGE - y);
        contenu.showText(texte);
        contenu.endText();
    }

    private static byte[] pdf(Dessin dessin) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(595, HAUTEUR_PAGE));
            document.addPage(page);
            try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                dessin.dessiner(contenu);
            }
            document.save(sortie);
            return sortie.toByteArray();
        }
    }

    @FunctionalInterface
    private interface Dessin {
        void dessiner(PDPageContentStream contenu) throws IOException;
    }
}
