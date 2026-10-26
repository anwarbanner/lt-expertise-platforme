package ma.ltexpertise.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Extraction complète d'un rapport PDF (F2, F3) : mots positionnés puis lecture du gabarit. */
@Component
public class ExtracteurRapport {

    private final PdfMotsExtractor mots = new PdfMotsExtractor();
    private final ParseurTcpdfExpertiseV1 parseur = new ParseurTcpdfExpertiseV1();

    /**
     * @throws GabaritNonReconnuException si le PDF ne suit pas le gabarit : rien ne doit être enregistré
     * @throws IOException                si le PDF est illisible
     */
    public RapportExtrait extraire(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            List<List<Ligne>> pages = new ArrayList<>();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                pages.add(mots.lignes(document, page));
            }
            return parseur.parser(pages);
        }
    }
}
