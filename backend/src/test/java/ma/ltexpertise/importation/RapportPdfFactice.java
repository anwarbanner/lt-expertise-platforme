package ma.ltexpertise.importation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Rapport d'expertise factice au gabarit TCPDF (2 pages), aux positions relevées sur un rapport réel.
 * Données fictives et montants cohérents : MO 1584, fournitures 736, total 2320 = indemnisation ; les 27
 * contrôles passent et aucun champ ne manque, sauf modification.
 */
public final class RapportPdfFactice {

    private static final float HAUTEUR = 842;
    private static final PDType1Font POLICE = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private String titre = "RAPPORT D'EXPERTISE";
    private final String numero;
    private String cabinet = "CABINET TEST";
    private String indemnisation = "2320.00";
    private String commentaire = "- DOSSIER DE TEST.";
    private String prixPpEtIng = "613.33";

    private RapportPdfFactice(String numero) {
        this.numero = numero;
    }

    public static RapportPdfFactice numero(String numero) {
        return new RapportPdfFactice(numero);
    }

    /** « RAPPORT D'EXPERTISE REFORME », « RAPPORT D'EXPERTISE NON DEFINITIF »… */
    public RapportPdfFactice titre(String titre) {
        this.titre = titre;
        return this;
    }

    /**
     * Prix unitaire faux imprimé sur la ligne « PP ET ING » (2e ligne de la page 2), son total restant juste :
     * le contrôle PAGE2_LIGNES_PRIX échoue, le prix attendu est 613,33.
     */
    public RapportPdfFactice prixFaux(String prix) {
        this.prixPpEtIng = prix;
        return this;
    }

    /** Cabinet masqué (« XXXX ») : signalé, sans bloquer la validation. */
    public RapportPdfFactice cabinetMasque() {
        this.cabinet = "XXXXXXXXX";
        return this;
    }

    /** Indemnisation différente du total : le contrôle échoue, le dossier passe à vérifier. */
    public RapportPdfFactice indemnisation(String montant) {
        this.indemnisation = montant;
        return this;
    }

    /** Change le contenu (donc l'empreinte) sans changer le n° de rapport. */
    public RapportPdfFactice commentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public byte[] pdf() {
        List<String[]> page1 = new ArrayList<>();
        ligne(page1, 20.9, "CABINET : " + cabinet, 16, "EXPERT : EXPERT TEST", 438);
        ligne(page1, 48.7, titre + " : " + numero, 193);
        ligne(page1, 59.8, "Procedure classique", 254);
        ligne(page1, 85.6, "Réf sinistre : 000000000001", 47, "Date sinistre : 05/10/2026", 153,
                "Date mission : 06/10/2026", 248, "Assurance : COMPAGNIE TEST", 346);
        ligne(page1, 100.4, "ASSURE", 173, "ADVERSAIRE", 364);
        ligne(page1, 113.1, "Nom & Prénom", 49, "ASSURE FICTIF", 176);
        ligne(page1, 126.6, "Véhicule", 49, "MARQUE MODELE", 176);
        ligne(page1, 140.0, "Immatriculation", 49, "00000-A-00", 176);
        ligne(page1, 153.5, "Cie d'assurance", 49, "COMPAGNIE TEST", 176);
        ligne(page1, 167.0, "N° Police", 49, "0000000000", 176);
        ligne(page1, 180.4, "Intermédiaire", 49, "AGENCE TEST", 176);
        ligne(page1, 196.9, "Nature de contrat :RC", 47);
        ligne(page1, 213.4, "Réparateur : GARAGE TEST", 49, "Accord du réparateur", 170);
        ligne(page1, 226.1, "Adresse :", 49, "Oui ( X ) Non ( )", 212);
        ligne(page1, 244.8, "CARACTERISTIQUES TECHNIQUES DU VEHICULE D'EXPERTISE", 178);
        ligne(page1, 259.3, "Marque", 49, ": MARQUE", 113, "Immatriculation", 223, ":", 302, "00000-A-00", 309,
                "N.E:circulation hors stationnement", 425);
        ligne(page1, 274.9, "Modèle", 49, ": MODELE", 113, "N° série", 223, ":", 302, "VF0000000000000000", 309);
        ligne(page1, 290.4, "type mine", 49, ": MODELE", 113, "Puissance", 223, ":6", 302);
        ligne(page1, 306.0, "Energie", 49, ": DIESEL", 113, "D.M.C", 223, ":", 302, "07/10/2022", 309);
        ligne(page1, 319.7, "Kilométrage", 49, ": 84000 Km", 113, "Et.général", 223, ":", 302, "Normal", 309);
        ligne(page1, 331.7, "Usure pneus", 49, "AVG :25 %", 113, "AVD :25 %", 223, "4 : Central/Lateral droit", 446);
        ligne(page1, 343.7, "ARG :25 %", 113, "ARD :25 %", 223);
        ligne(page1, 361.6, "Date expertise véhicule :07/10/2026", 232);
        ligne(page1, 374.4, "Avant réparation 07/10/2026", 63, "En cours travaux 08/10/2026", 223,
                "Aprés travaux 14/10/2026", 382);
        ligne(page1, 392.3, "EVALUATION DES DOMMAGES", 244);
        String[][] postes = {
                {"MO 1 Tôlerie", "12.00", "60.00", "720.00", "144.00", "864.00"},
                {"MO 2 Tôlerie", "0.00", "0.00", "0.00", "0.00", "0.00"},
                {"MO 3 Tôlerie", "0.00", "0.00", "0.00", "0.00", "0.00"},
                {"MO Peinture", "10.00", "60.00", "600.00", "120.00", "720.00"},
                {"MO 1 Mécanique", "0.00", "0.00", "0.00", "0.00", "0.00"},
                {"MO 2 Mécanique", "0.00", "0.00", "0.00", "0.00", "0.00"},
                {"MO 3 Mécanique", "0.00", "0.00", "0.00", "0.00", "0.00"}};
        double y = 414.0;
        for (String[] p : postes) {
            ligne(page1, y, p[0], 49, p[1], 160, p[2], 267, p[3], 354, p[4], 446, p[5], 538);
            y += 12.7;
        }
        ligne(page1, 503.0, "Total main d'oeuvre", 49, "1320.00", 350, "264.00", 446, "1584.00", 534);
        ligne(page1, 516.5, "Pièces", 49, "0.00", 363, "0.00", 455, "0.00", 547);
        ligne(page1, 529.2, "Ingrédient peinture", 49, "613.33", 354, "122.67", 446, "736.00", 538);
        ligne(page1, 541.9, "Petite fourniture", 49, "0.00", 363, "0.00", 455, "0.00", 547);
        ligne(page1, 554.6, "Autres", 49, "0.00", 363, "0.00", 455, "0.00", 547);
        ligne(page1, 567.4, "Total fournitures (Dh)", 49, "613.33", 354, "122.67", 446, "736.00", 538);
        ligne(page1, 580.8, "Total général", 49, "1933.33", 350, "386.67", 446, "2320.00", 534);
        ligne(page1, 593.5, "A Déduire", 49, "Vétusté TTC :0.00", 290);
        ligne(page1, 606.3, "Immobilisation :", 49, "Dépannage :0.00", 290);
        ligne(page1, 635.8, "Epaviste retenu RéformeEconomique( ) Technique( )", 123);
        ligne(page1, 647.0, "Valeur à neuf", 196, ": NC", 307);
        ligne(page1, 657.5, "Valeur assurée", 196, ": NC", 307);
        ligne(page1, 667.9, "Valeur vénale", 196, ": NC", 307);
        ligne(page1, 678.4, "Valeur épave", 196, ": NC", 307);
        ligne(page1, 689.6, "Récupérabilité de TVA:Non récupérable", 123);
        ligne(page1, 703.4, "Montant de l'indemnisation :" + indemnisation, 123);
        ligne(page1, 715.3, "Arrêté le présent rapport à la somme de :", 123);
        ligne(page1, 725.8, "deux mille trois cent vingt Dirhams", 123);
        ligne(page1, 813.9, "Expert : EXPERT TEST", 15, "Fait à Casablanca le 17/10/2026", 452);

        List<String[]> page2 = new ArrayList<>();
        ligne(page2, 20.9, "CABINET : " + cabinet, 16, "EXPERT : EXPERT TEST", 438);
        ligne(page2, 51.7, "Réf sinistre : 000000000001", 47, "Date sinistre : 05/10/2026", 142);
        ligne(page2, 65.0, "Détail Fourniture", 135, "Travaux de réparation", 325);
        ligne(page2, 75.1, "Qte", 46, "Désignation", 62, "Type", 114, "Prix", 138, "Vétusté%", 177, "Tva %", 224,
                "Total", 252, "Désignation", 291, "Nbr H", 345, "Tarif", 369, "Montant TTC", 393);
        ligne(page2, 85.9, "Central/Lateral droit", 46, ".", 253);
        ligne(page2, 97.9, "· Peinture", 46, ".", 225);
        detail(page2, 109.1, "MOP", null, "0.00", "0.00", "0.00", "0.00", "10.00", "60.00", "720.00");
        detail(page2, 120.4, "PP ET ING", null, prixPpEtIng, "0.00", "20.00", "736.00", "0.00", "0.00", "0.00");
        ligne(page2, 131.6, "· Tolerie", 46, ".", 225);
        detail(page2, 142.8, "PORTE AV D", "R", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00");
        detail(page2, 154.0, "MOT", null, "0.00", "0.00", "0.00", "0.00", "12.00", "60.00", "864.00");
        ligne(page2, 164.7, "O: Pièce d'origine A: Pièce adaptable et R: Pièce réemploi", 43);
        ligne(page2, 187.5, "COMMENTAIRE RAPPORT :", 45);
        ligne(page2, 200.5, commentaire, 45);
        ligne(page2, 813.9, "Expert : EXPERT TEST", 15, "Fait à Casablanca le 17/10/2026", 443);
        return dessiner(List.of(page1, page2));
    }

    private static void detail(List<String[]> page, double y, String designation, String type, String prix,
                               String vetuste, String tva, String total, String heures, String tarif, String montant) {
        List<Object> segments = new ArrayList<>(List.of("1", 46, designation, 63));
        if (type != null) {
            segments.addAll(List.of(type, 116));
        }
        segments.addAll(List.of(prix, 151, vetuste, 206, tva, 230, total, 264, "Réparation", 292,
                heures, 346, tarif, 371, montant, 420));
        ligne(page, y, segments.toArray());
    }

    /** Segments « texte, x » d'une ligne ; chaque segment est une cellule TCPDF. */
    private static void ligne(List<String[]> page, double y, Object... segments) {
        for (int i = 0; i < segments.length; i += 2) {
            page.add(new String[]{String.valueOf(y), String.valueOf(segments[i + 1]), (String) segments[i]});
        }
    }

    private static byte[] dessiner(List<List<String[]>> pages) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            for (List<String[]> segments : pages) {
                PDPage page = new PDPage(new PDRectangle(595, HAUTEUR));
                document.addPage(page);
                try (PDPageContentStream contenu = new PDPageContentStream(document, page)) {
                    for (String[] s : segments) {
                        contenu.beginText();
                        contenu.setFont(POLICE, 7);
                        contenu.newLineAtOffset(Float.parseFloat(s[1]), HAUTEUR - Float.parseFloat(s[0]));
                        contenu.showText(s[2]);
                        contenu.endText();
                    }
                }
            }
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
