package ma.ltexpertise.importation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** PDF factices pour les tests : aucune donnée réelle (les PDF de samples/ ne sont jamais commités). */
final class GenerateurPdf {

    private GenerateurPdf() {
    }

    /** Rapport au gabarit attendu : 2 pages, titre « RAPPORT D'EXPERTISE : n° » en page 1. */
    static byte[] rapport(String numero) {
        return pdf("RAPPORT D'EXPERTISE : " + numero, 2, "Detail fournitures");
    }

    /** Même n° de rapport mais contenu différent, donc empreinte différente. */
    static byte[] autreVersion(String numero) {
        return pdf("RAPPORT D'EXPERTISE : " + numero, 2, "Detail fournitures - version corrigee");
    }

    static byte[] pdf(String textePage1, int pages, String textePage2) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PDType1Font police = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                    contenu.beginText();
                    contenu.setFont(police, 12);
                    contenu.newLineAtOffset(50, 750);
                    contenu.showText("CABINET : CABINET TEST");
                    contenu.newLineAtOffset(0, -30);
                    contenu.showText(i == 0 ? textePage1 : textePage2);
                    contenu.endText();
                }
            }
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
