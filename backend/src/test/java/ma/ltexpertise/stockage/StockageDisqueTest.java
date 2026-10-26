package ma.ltexpertise.stockage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockageDisqueTest {

    @TempDir
    Path racine;

    private StockageDisque stockage() {
        return new StockageDisque(new ProprietesStockage("disque", racine, "http://inutilise", "a", "b", "c"));
    }

    @Test
    void deposeLeFichierEnLectureSeule() {
        byte[] pdf = "%PDF-1.7 original".getBytes(StandardCharsets.US_ASCII);

        DocumentStocke document = stockage().deposer("originaux/ab/abcd.pdf", pdf, "application/pdf");

        Path fichier = racine.resolve("originaux/ab/abcd.pdf");
        assertThat(document).isEqualTo(new DocumentStocke(StockageDisque.BUCKET, "originaux/ab/abcd.pdf", null));
        assertThat(fichier).hasBinaryContent(pdf);
        assertThat(Files.isWritable(fichier)).as("P1 : original non modifiable").isFalse();
        assertThat(stockage().lire(StockageDisque.BUCKET, "originaux/ab/abcd.pdf", null)).isEqualTo(pdf);
    }

    @Test
    void neReecritJamaisUnDocumentExistant() {
        StockageDisque stockage = stockage();
        byte[] original = "%PDF-1.7 original".getBytes(StandardCharsets.US_ASCII);
        stockage.deposer("originaux/cd/cdef.pdf", original, "application/pdf");

        stockage.deposer("originaux/cd/cdef.pdf", "%PDF-1.7 autre".getBytes(StandardCharsets.US_ASCII), "application/pdf");

        assertThat(stockage.lire(StockageDisque.BUCKET, "originaux/cd/cdef.pdf", null)).isEqualTo(original);
    }

    @Test
    void signaleUnDocumentIntrouvable() {
        assertThatThrownBy(() -> stockage().lire(StockageDisque.BUCKET, "originaux/00/absent.pdf", null))
                .isInstanceOf(DocumentIntrouvableException.class);
    }

    @Test
    void refuseUneCleQuiSortDeLaRacine() {
        assertThatThrownBy(() -> stockage().deposer("../evasion.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(StockageException.class)
                .hasMessageContaining("invalide");
        assertThat(racine.getParent().resolve("evasion.pdf")).doesNotExist();
    }
}
