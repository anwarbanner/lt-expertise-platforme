package ma.ltexpertise.importation;

import ma.ltexpertise.dossier.referentiel.NatureRapport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reconnaît le gabarit des rapports LT Expertise (PDF TCPDF d'au moins 2 pages : le détail des fournitures
 * peut déborder sur une 3e page) et lit le n° de rapport,
 * comme le prototype {@code docs/reference/extracteur_rapport.py}. Un PDF au modèle inconnu est refusé
 * proprement pour ne jamais enregistrer de données fausses (§2 du cahier des charges).
 */
@Component
public class AnalyseurGabarit {

    public static final String GABARIT_V1 = "tcpdf-expertise-v1";

    /** « RAPPORT D'EXPERTISE [NON DEFINITIF | REFORME] : 268202-3166 » */
    private static final Pattern TITRE = Pattern.compile(
            "RAPPORT\\s+D['’]EXPERTISE(?:\\s+(NON\\s+DEFINITIF|REFORME))?\\s*:\\s*(\\S+)");

    public AnalyseGabarit analyser(byte[] contenu) {
        try (PDDocument document = Loader.loadPDF(contenu)) {
            int pages = document.getNumberOfPages();
            if (pages < 2) {
                return AnalyseGabarit.nonReconnu(pages, "Modèle non reconnu : " + pages
                        + " page(s), au moins 2 attendues.");
            }
            PDFTextStripper lecteur = new PDFTextStripper();
            lecteur.setStartPage(1);
            lecteur.setEndPage(1);
            Matcher titre = TITRE.matcher(lecteur.getText(document));
            if (!titre.find()) {
                return AnalyseGabarit.nonReconnu(pages,
                        "Modèle non reconnu : titre « RAPPORT D'EXPERTISE : n° » absent de la page 1.");
            }
            return AnalyseGabarit.reconnu(pages, GABARIT_V1, titre.group(2), nature(titre.group(1)));
        } catch (IOException e) {
            return AnalyseGabarit.illisible("PDF illisible : " + e.getMessage());
        }
    }

    private static NatureRapport nature(String mention) {
        if (mention == null) {
            return NatureRapport.DEFINITIF;
        }
        return mention.startsWith("NON") ? NatureRapport.NON_DEFINITIF : NatureRapport.REFORME;
    }
}
