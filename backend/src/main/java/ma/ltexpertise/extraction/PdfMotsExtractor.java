package ma.ltexpertise.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lit les glyphes d'une page avec leur position ({@link TextPosition}), les assemble en mots puis regroupe
 * les mots en lignes visuelles par ordonnée.
 *
 * <p>Un mot se termine sur un espace ou sur un écart horizontal entre deux glyphes : le gabarit TCPDF
 * juxtapose des cellules sans espace (« DésignationType » dans certains rapports), qu'un découpage sur
 * les seuls espaces fusionnerait.
 *
 * <p>La police des données des rapports TCPDF associe la lettre J au caractère « Ã » (table ToUnicode erronée :
 * « JEAN » est lu « ÃEAN », un châssis « TMAJ… » est lu « TMAÃ… ») ; « Ã » n'existant dans aucun mot français
 * ni dans un châssis ou une immatriculation, il est rétabli en J à la lecture.
 */
public class PdfMotsExtractor {

    /** Écart vertical maximal entre deux mots d'une même ligne, en points (comme le prototype Python). */
    public static final double TOLERANCE_LIGNE = 2.5;

    /** Caractère produit par la police des rapports à la place de la lettre J. */
    private static final String J_MAL_DECODE = "Ã";

    /** Écart horizontal, en fraction de la taille de police, au-delà duquel deux glyphes appartiennent à deux mots. */
    private static final double ECART_ENTRE_MOTS = 0.15;

    /** Lignes de la page {@code numeroPage} (à partir de 1), de haut en bas. */
    public List<Ligne> lignes(PDDocument document, int numeroPage) throws IOException {
        return regrouperEnLignes(mots(document, numeroPage));
    }

    /** Mots de la page {@code numeroPage} (à partir de 1), dans l'ordre de lecture des glyphes. */
    public List<Mot> mots(PDDocument document, int numeroPage) throws IOException {
        CollecteurGlyphes collecteur = new CollecteurGlyphes();
        collecteur.setStartPage(numeroPage);
        collecteur.setEndPage(numeroPage);
        collecteur.getText(document);
        return assemblerMots(collecteur.glyphes);
    }

    static List<Mot> assemblerMots(List<TextPosition> glyphes) {
        List<TextPosition> tries = new ArrayList<>(glyphes);
        // Regroupement préalable par ligne pour assembler les glyphes de gauche à droite.
        tries.sort(Comparator.comparingDouble(PdfMotsExtractor::centreY));
        List<List<TextPosition>> parLigne = new ArrayList<>();
        for (TextPosition glyphe : tries) {
            List<TextPosition> derniere = parLigne.isEmpty() ? null : parLigne.getLast();
            if (derniere != null && Math.abs(centreY(derniere.getFirst()) - centreY(glyphe)) <= TOLERANCE_LIGNE) {
                derniere.add(glyphe);
            } else {
                parLigne.add(new ArrayList<>(List.of(glyphe)));
            }
        }

        List<Mot> mots = new ArrayList<>();
        for (List<TextPosition> ligne : parLigne) {
            ligne.sort(Comparator.comparingDouble(TextPosition::getXDirAdj));
            StringBuilder texte = new StringBuilder();
            double debut = 0;
            double fin = 0;
            double y = 0;
            for (TextPosition glyphe : ligne) {
                String caractere = corriger(glyphe.getUnicode());
                boolean blanc = caractere == null || caractere.isBlank();
                double ecart = glyphe.getXDirAdj() - fin;
                boolean coupure = blanc || (!texte.isEmpty() && ecart > ECART_ENTRE_MOTS * taillePolice(glyphe));
                if (coupure && !texte.isEmpty()) {
                    mots.add(new Mot(debut, fin, y, texte.toString()));
                    texte.setLength(0);
                }
                if (blanc) {
                    continue;
                }
                if (texte.isEmpty()) {
                    debut = glyphe.getXDirAdj();
                    y = centreY(glyphe);
                }
                texte.append(caractere);
                fin = glyphe.getXDirAdj() + glyphe.getWidthDirAdj();
            }
            if (!texte.isEmpty()) {
                mots.add(new Mot(debut, fin, y, texte.toString()));
            }
        }
        return mots;
    }

    /** Regroupe les mots en lignes : un mot rejoint la première ligne dont l'ordonnée est à moins de 2,5 pt. */
    static List<Ligne> regrouperEnLignes(List<Mot> mots) {
        List<Double> ordonnees = new ArrayList<>();
        List<List<Mot>> groupes = new ArrayList<>();
        for (Mot mot : mots) {
            int indice = -1;
            for (int i = 0; i < ordonnees.size(); i++) {
                if (Math.abs(ordonnees.get(i) - mot.y()) <= TOLERANCE_LIGNE) {
                    indice = i;
                    break;
                }
            }
            if (indice < 0) {
                ordonnees.add(mot.y());
                groupes.add(new ArrayList<>());
                indice = groupes.size() - 1;
            }
            groupes.get(indice).add(mot);
        }
        List<Ligne> lignes = new ArrayList<>();
        for (int i = 0; i < groupes.size(); i++) {
            List<Mot> groupe = new ArrayList<>(groupes.get(i));
            groupe.sort(Comparator.comparingDouble(Mot::x));
            lignes.add(new Ligne(ordonnees.get(i), groupe));
        }
        lignes.sort(Comparator.comparingDouble(Ligne::y));
        return lignes;
    }

    static String corriger(String caractere) {
        return J_MAL_DECODE.equals(caractere) ? "J" : caractere;
    }

    private static double centreY(TextPosition glyphe) {
        return glyphe.getYDirAdj() - glyphe.getHeightDir() / 2;
    }

    private static double taillePolice(TextPosition glyphe) {
        double taille = glyphe.getFontSizeInPt();
        return taille > 0 ? taille : glyphe.getHeightDir();
    }

    /** Collecte les glyphes sans produire de texte. */
    private static final class CollecteurGlyphes extends PDFTextStripper {

        private final List<TextPosition> glyphes = new ArrayList<>();

        CollecteurGlyphes() throws IOException {
            super();
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            glyphes.add(text);
        }
    }
}
