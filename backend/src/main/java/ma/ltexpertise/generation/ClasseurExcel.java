package ma.ltexpertise.generation;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Classeur Excel (.xlsx) écrit en flux (SXSSF) : une liste de dizaines de milliers de dossiers ne garde en mémoire
 * qu'une fenêtre de lignes. En-têtes en gras et figés, montants au format « # ##0,00 », dates « jj/mm/aaaa ».
 */
public final class ClasseurExcel implements AutoCloseable {

    private static final int LARGEUR_MAX = 60;
    private final SXSSFWorkbook classeur = new SXSSFWorkbook(200);
    private final CellStyle entete;
    private final CellStyle monetaire;
    private final CellStyle date;
    private final CellStyle horodatage;

    public ClasseurExcel() {
        Font gras = classeur.createFont();
        gras.setBold(true);
        entete = classeur.createCellStyle();
        entete.setFont(gras);
        entete.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        entete.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        entete.setBorderBottom(BorderStyle.THIN);
        monetaire = classeur.createCellStyle();
        monetaire.setDataFormat(classeur.createDataFormat().getFormat("#,##0.00"));
        date = classeur.createCellStyle();
        date.setDataFormat(classeur.createDataFormat().getFormat("dd/mm/yyyy"));
        horodatage = classeur.createCellStyle();
        horodatage.setDataFormat(classeur.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
    }

    /** Nouvelle feuille avec sa ligne d'en-têtes (figée, filtrable). */
    public Feuille feuille(String nom, String... entetes) {
        SXSSFSheet feuille = classeur.createSheet(nom);
        feuille.trackAllColumnsForAutoSizing();
        Feuille f = new Feuille(feuille, entetes.length);
        if (entetes.length > 0) {
            Row ligne = feuille.createRow(f.suivante++);
            for (int i = 0; i < entetes.length; i++) {
                Cell c = ligne.createCell(i);
                c.setCellValue(entetes[i]);
                c.setCellStyle(entete);
            }
            feuille.createFreezePane(0, 1);
            feuille.setAutoFilter(new CellRangeAddress(0, 0, 0, entetes.length - 1));
        }
        return f;
    }

    public byte[] octets() {
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            classeur.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public final class Feuille {

        private final SXSSFSheet feuille;
        private final int colonnes;
        private int suivante;

        private Feuille(SXSSFSheet feuille, int colonnes) {
            this.feuille = feuille;
            this.colonnes = colonnes;
        }

        /** Ajoute une ligne ; une valeur {@code null} laisse la cellule vide. */
        public Feuille ligne(Object... valeurs) {
            Row ligne = feuille.createRow(suivante++);
            for (int i = 0; i < valeurs.length; i++) {
                Object v = valeurs[i];
                if (v == null) {
                    continue;
                }
                Cell c = ligne.createCell(i);
                switch (v) {
                    case BigDecimal b -> {
                        c.setCellValue(b.doubleValue());
                        c.setCellStyle(monetaire);
                    }
                    case Number n -> c.setCellValue(n.doubleValue());
                    case Boolean b -> c.setCellValue(b ? "Oui" : "Non");
                    case LocalDate d -> {
                        c.setCellValue(d);
                        c.setCellStyle(date);
                    }
                    case OffsetDateTime t -> {
                        c.setCellValue(Date.from(t.atZoneSameInstant(ZoneId.systemDefault()).toInstant()));
                        c.setCellStyle(horodatage);
                    }
                    default -> c.setCellValue(v.toString());
                }
            }
            return this;
        }

        /** Largeur des colonnes ajustée au contenu (bornée), une fois les lignes écrites. */
        public void ajuster() {
            for (int i = 0; i < colonnes; i++) {
                feuille.autoSizeColumn(i);
                feuille.setColumnWidth(i, Math.min(feuille.getColumnWidth(i) + 512, LARGEUR_MAX * 256));
            }
        }
    }
}
