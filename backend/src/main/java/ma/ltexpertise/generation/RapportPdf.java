package ma.ltexpertise.generation;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignGroup;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.TextAdjustEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Modèle JasperReports du rapport régénéré (P10), dessiné en Java sur la structure du rapport d'origine
 * (gabarit TCPDF de l'expert) : page 1 (en-tête, parties, véhicule, évaluation des dommages, conclusion), puis le
 * détail des fournitures et travaux par zone et famille, et le commentaire. Toutes les valeurs arrivent déjà mises
 * en forme (paramètres texte et tableaux de texte, champs texte des lignes) : le modèle ne fait que les placer.
 * Compilé une fois, au premier usage.
 */
final class RapportPdf {

    static final String POLICE = "DejaVu Sans";
    private static final int LARGEUR = 555;
    private static final Color GRIS = new Color(0xE6, 0xE7, 0xEE);
    /** Marge intérieure des cellules du détail : deux colonnes voisines ne se touchent pas. */
    private static final int MARGE_CELLULE = 3;

    /** Lignes du tableau « Évaluation des dommages » : 7 postes, total MO, 4 fournitures, totaux. */
    static final int LIGNES_EVALUATION = 14;
    private static final List<Integer> LIGNES_TOTAL = List.of(7, 12, 13);
    static final int LIGNES_PARTIES = 6;
    static final int LIGNES_VEHICULE = 6;

    /** Colonnes du détail : titre, champ, largeur, aligné à droite. */
    private record Colonne(String titre, String champ, int largeur, boolean droite) {
    }

    private static final List<Colonne> COLONNES = List.of(
            new Colonne("N°", "n", 20, false), new Colonne("Qté", "qte", 28, true),
            new Colonne("Désignation", "designation", 114, false), new Colonne("Type", "type", 26, false),
            new Colonne("Prix", "prix", 50, true), new Colonne("Vétusté%", "vetuste", 40, true),
            new Colonne("Tva %", "tva", 30, true), new Colonne("Total", "total", 54, true),
            new Colonne("Désignation", "travail", 55, false), new Colonne("Nbr H", "heures", 32, true),
            new Colonne("Tarif", "tarif", 42, true), new Colonne("Montant TTC", "montant", 64, true));

    private volatile JasperReport modele;

    byte[] generer(Map<String, Object> parametres, Collection<Map<String, ?>> lignes) {
        try {
            JasperPrint impression = JasperFillManager.fillReport(modele(), parametres, new JRMapCollectionDataSource(lignes));
            return JasperExportManager.exportReportToPdf(impression);
        } catch (JRException e) {
            throw new IllegalStateException("Génération du rapport PDF impossible : " + e.getMessage(), e);
        }
    }

    private JasperReport modele() throws JRException {
        JasperReport m = modele;
        if (m == null) {
            synchronized (this) {
                if (modele == null) {
                    modele = JasperCompileManager.compileReport(conception());
                }
                m = modele;
            }
        }
        return m;
    }

    // ------------------------------------------------------------------ conception

    static JasperDesign conception() throws JRException {
        JasperDesign d = new JasperDesign();
        d.setName("rapport_expertise");
        d.setPageWidth(595);
        d.setPageHeight(842);
        d.setColumnWidth(LARGEUR);
        d.setLeftMargin(20);
        d.setRightMargin(20);
        d.setTopMargin(20);
        d.setBottomMargin(20);
        d.setTitleNewPage(true);
        d.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);

        for (String p : List.of("cabinet", "expert", "titre", "procedure", "sinistre", "natureContrat", "reparateur",
                "datesTravaux", "deduire", "valeurs", "tva", "indemnisation", "montantLettres", "faitA", "mention",
                "commentaire", "refSinistre", "dateSinistre")) {
            parametre(d, p, String.class);
        }
        // Tableaux : une cellule par paramètre texte (le compilateur de Jasper n'accepte pas les types tableau)
        cellules(d, "parties", LIGNES_PARTIES, 3);
        cellules(d, "vehicule", LIGNES_VEHICULE, 4);
        cellules(d, "evaluation", LIGNES_EVALUATION, 6);
        for (Colonne c : COLONNES) {
            champ(d, c.champ());
        }
        champ(d, "zone");
        champ(d, "famille");

        d.setTitle(page1());
        d.setColumnHeader(enTeteDetail());
        groupe(d, "zone", "$F{zone}", 14, b -> b.addElement(texte(0, 2, LARGEUR, 11, "$F{zone}", 8, true, false)));
        groupe(d, "famille", "$F{famille}", 12, b -> b.addElement(texte(8, 1, 300, 10, "\"· \" + $F{famille}", 7.5f, true, false)));

        JRDesignBand detail = bande(11);
        int x = 0;
        for (Colonne c : COLONNES) {
            detail.addElement(texte(x + MARGE_CELLULE, 0, c.largeur() - 2 * MARGE_CELLULE, 11, "$F{" + c.champ() + "}",
                    7, false, c.droite()));
            x += c.largeur();
        }
        ((JRDesignSection) d.getDetailSection()).addBand(detail);
        d.setSummary(resume());
        d.setPageFooter(piedDePage());
        return d;
    }

    private static JRDesignBand page1() {
        JRDesignBand b = bande(780);
        b.addElement(texte(0, 0, 330, 12, "\"CABINET : \" + $P{cabinet}", 8, false, false));
        b.addElement(texte(330, 0, 225, 12, "\"EXPERT : \" + $P{expert}", 8, false, true));
        b.addElement(trait(0, 16, LARGEUR));
        b.addElement(texte(0, 24, LARGEUR, 18, "$P{titre}", 13, true, false, HorizontalTextAlignEnum.CENTER));
        b.addElement(texte(0, 43, LARGEUR, 12, "$P{procedure}", 9, false, false, HorizontalTextAlignEnum.CENTER));
        b.addElement(texte(0, 60, LARGEUR, 12, "$P{sinistre}", 7.5f, false, false));

        // Parties : étiquette, assuré, adversaire
        int y = 78;
        b.addElement(bandeau(150, y, 200, "ASSURE"));
        b.addElement(bandeau(355, y, 200, "ADVERSAIRE"));
        y += 16;
        for (int i = 0; i < LIGNES_PARTIES; i++) {
            b.addElement(texte(0, y, 148, 12, cellule("parties", i, 0), 7.5f, true, false));
            b.addElement(texte(150, y, 200, 12, cellule("parties", i, 1), 7.5f, false, false));
            b.addElement(texte(355, y, 200, 12, cellule("parties", i, 2), 7.5f, false, false));
            y += 12;
        }
        y += 6;
        b.addElement(texte(0, y, LARGEUR, 12, "$P{natureContrat}", 7.5f, false, false));
        b.addElement(texte(0, y + 13, LARGEUR, 12, "$P{reparateur}", 7.5f, false, false));

        // Caractéristiques du véhicule : deux colonnes « étiquette : valeur »
        y += 32;
        b.addElement(bandeau(0, y, LARGEUR, "CARACTERISTIQUES TECHNIQUES DU VEHICULE D'EXPERTISE"));
        y += 18;
        for (int i = 0; i < LIGNES_VEHICULE; i++) {
            b.addElement(texte(0, y, 90, 12, cellule("vehicule", i, 0), 7.5f, true, false));
            b.addElement(texte(90, y, 185, 12, cellule("vehicule", i, 1), 7.5f, false, false));
            b.addElement(texte(285, y, 95, 12, cellule("vehicule", i, 2), 7.5f, true, false));
            b.addElement(texte(380, y, 175, 12, cellule("vehicule", i, 3), 7.5f, false, false));
            y += 12;
        }
        y += 4;
        b.addElement(texte(0, y, LARGEUR, 12, "$P{datesTravaux}", 7.5f, false, false));

        // Évaluation des dommages
        y += 18;
        b.addElement(bandeau(0, y, LARGEUR, "EVALUATION DES DOMMAGES"));
        y += 18;
        String[] titres = {"Désignation", "Nbr heures", "Taux horaire", "Montant HT", "TVA", "TTC"};
        int[] largeurs = {155, 70, 80, 85, 80, 85};
        int x = 0;
        for (int j = 0; j < titres.length; j++) {
            JRDesignStaticText t = statique(x, y, largeurs[j], 13, titres[j], 7.5f, true, j > 0);
            fond(t);
            b.addElement(t);
            x += largeurs[j];
        }
        y += 14;
        for (int i = 0; i < LIGNES_EVALUATION; i++) {
            boolean total = LIGNES_TOTAL.contains(i);
            x = 0;
            for (int j = 0; j < titres.length; j++) {
                JRDesignTextField t = texte(x + 2, y, largeurs[j] - 4, 12, cellule("evaluation", i, j), 7.5f, total, j > 0);
                b.addElement(t);
                x += largeurs[j];
            }
            if (total) {
                b.addElement(trait(0, y, LARGEUR));
            }
            y += 12;
        }
        b.addElement(trait(0, y, LARGEUR));

        // Conclusion
        y += 6;
        b.addElement(texte(0, y, LARGEUR, 12, "$P{deduire}", 7.5f, false, false));
        b.addElement(texte(0, y + 14, LARGEUR, 24, "$P{valeurs}", 7.5f, false, false));
        b.addElement(texte(0, y + 40, LARGEUR, 12, "$P{tva}", 7.5f, false, false));
        JRDesignTextField indemnisation = texte(0, y + 58, LARGEUR, 18, "$P{indemnisation}", 11, true, false,
                HorizontalTextAlignEnum.CENTER);
        fond(indemnisation);
        b.addElement(indemnisation);
        b.addElement(statique(0, y + 82, LARGEUR, 12, "Arrêté le présent rapport à la somme de :", 7.5f, false, false));
        b.addElement(texte(0, y + 95, LARGEUR, 24, "$P{montantLettres}", 8, true, false));
        b.addElement(trait(0, y + 124, LARGEUR));
        b.addElement(texte(0, y + 128, 275, 12, "\"Expert : \" + $P{expert}", 7.5f, false, false));
        b.addElement(texte(280, y + 128, 275, 12, "$P{faitA}", 7.5f, false, true));
        b.addElement(texte(0, y + 146, LARGEUR, 22, "$P{mention}", 6.5f, false, false));
        return b;
    }

    private static JRDesignBand enTeteDetail() {
        JRDesignBand b = bande(58);
        b.addElement(texte(0, 0, 330, 12, "\"CABINET : \" + $P{cabinet}", 8, false, false));
        b.addElement(texte(330, 0, 225, 12, "\"EXPERT : \" + $P{expert}", 8, false, true));
        b.addElement(texte(0, 14, 275, 12, "\"Réf sinistre : \" + $P{refSinistre}", 7.5f, false, false));
        b.addElement(texte(280, 14, 275, 12, "\"Date sinistre : \" + $P{dateSinistre}", 7.5f, false, false));
        int fournitures = COLONNES.subList(0, 8).stream().mapToInt(Colonne::largeur).sum();
        b.addElement(bandeau(0, 30, fournitures - 2, "Détail Fourniture"));
        b.addElement(bandeau(fournitures, 30, LARGEUR - fournitures, "Travaux de réparation"));
        int x = 0;
        for (Colonne c : COLONNES) {
            JRDesignStaticText t = statique(x, 45, c.largeur(), 12, c.titre(), 6.5f, true, c.droite());
            t.getLineBox().setLeftPadding(MARGE_CELLULE);
            t.getLineBox().setRightPadding(MARGE_CELLULE);
            fond(t);
            b.addElement(t);
            x += c.largeur();
        }
        return b;
    }

    private static JRDesignBand resume() {
        JRDesignBand b = bande(70);
        b.addElement(trait(0, 2, LARGEUR));
        b.addElement(statique(0, 5, LARGEUR, 11, "O: Pièce d'origine A: Pièce adaptable et R: Pièce réemploi", 7, false, false));
        b.addElement(statique(0, 22, LARGEUR, 12, "COMMENTAIRE RAPPORT :", 8, true, false));
        JRDesignTextField commentaire = texte(0, 36, LARGEUR, 14, "$P{commentaire}", 7.5f, false, false);
        commentaire.setTextAdjust(TextAdjustEnum.STRETCH_HEIGHT);
        b.addElement(commentaire);
        JRDesignTextField fait = texte(280, 54, 275, 12, "$P{faitA}", 7.5f, false, true);
        fait.setPositionType(PositionTypeEnum.FLOAT);
        b.addElement(fait);
        return b;
    }

    private static JRDesignBand piedDePage() {
        JRDesignBand b = bande(14);
        b.addElement(texte(0, 2, 300, 11, "\"Expert : \" + $P{expert} + \" · \" + $P{faitA}", 6.5f, false, false));
        // La page de garde (page 1) n'a pas de pied de page : le total compte toutes les pages
        b.addElement(texte(400, 2, 122, 11, "\"Page \" + $V{PAGE_NUMBER} + \" / \"", 6.5f, false, true));
        JRDesignTextField total = texte(522, 2, 33, 11, "String.valueOf($V{PAGE_NUMBER})", 6.5f, false, false);
        total.setEvaluationTime(EvaluationTimeEnum.REPORT);
        b.addElement(total);
        return b;
    }

    // ------------------------------------------------------------------ outils de dessin

    /** Nom du paramètre portant la cellule (i, j) d'un tableau : « evaluation_3_5 ». */
    static String nomCellule(String tableau, int i, int j) {
        return tableau + "_" + i + "_" + j;
    }

    private static String cellule(String tableau, int i, int j) {
        return "$P{" + nomCellule(tableau, i, j) + "}";
    }

    private static void cellules(JasperDesign d, String tableau, int lignes, int colonnes) throws JRException {
        for (int i = 0; i < lignes; i++) {
            for (int j = 0; j < colonnes; j++) {
                parametre(d, nomCellule(tableau, i, j), String.class);
            }
        }
    }

    /** Reporte un tableau de texte dans les paramètres de cellule. */
    static void aplatir(Map<String, Object> parametres, String tableau, String[][] valeurs) {
        for (int i = 0; i < valeurs.length; i++) {
            for (int j = 0; j < valeurs[i].length; j++) {
                parametres.put(nomCellule(tableau, i, j), valeurs[i][j]);
            }
        }
    }

    private static void parametre(JasperDesign d, String nom, Class<?> type) throws JRException {
        JRDesignParameter p = new JRDesignParameter();
        p.setName(nom);
        p.setValueClass(type);
        d.addParameter(p);
    }

    private static void champ(JasperDesign d, String nom) throws JRException {
        JRDesignField f = new JRDesignField();
        f.setName(nom);
        f.setValueClass(String.class);
        d.addField(f);
    }

    private static void groupe(JasperDesign d, String nom, String expression, int hauteur,
                               java.util.function.Consumer<JRDesignBand> contenu) throws JRException {
        JRDesignGroup g = new JRDesignGroup();
        g.setName(nom);
        g.setExpression(new JRDesignExpression(expression));
        JRDesignBand b = bande(hauteur);
        contenu.accept(b);
        ((JRDesignSection) g.getGroupHeaderSection()).addBand(b);
        d.addGroup(g);
    }

    private static JRDesignBand bande(int hauteur) {
        JRDesignBand b = new JRDesignBand();
        b.setHeight(hauteur);
        return b;
    }

    private static JRDesignTextField texte(int x, int y, int l, int h, String expression, float taille, boolean gras,
                                           boolean droite) {
        return texte(x, y, l, h, expression, taille, gras, droite,
                droite ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.LEFT);
    }

    private static JRDesignTextField texte(int x, int y, int l, int h, String expression, float taille, boolean gras,
                                           boolean droite, HorizontalTextAlignEnum alignement) {
        JRDesignTextField t = new JRDesignTextField();
        t.setX(x);
        t.setY(y);
        t.setWidth(l);
        t.setHeight(h);
        t.setExpression(new JRDesignExpression(expression));
        t.setBlankWhenNull(true);
        t.setFontName(POLICE);
        t.setFontSize(taille);
        t.setBold(gras);
        t.setHorizontalTextAlign(alignement);
        return t;
    }

    private static JRDesignStaticText statique(int x, int y, int l, int h, String texte, float taille, boolean gras,
                                               boolean droite) {
        JRDesignStaticText t = new JRDesignStaticText();
        t.setX(x);
        t.setY(y);
        t.setWidth(l);
        t.setHeight(h);
        t.setText(texte);
        t.setFontName(POLICE);
        t.setFontSize(taille);
        t.setBold(gras);
        t.setHorizontalTextAlign(droite ? HorizontalTextAlignEnum.RIGHT : HorizontalTextAlignEnum.LEFT);
        return t;
    }

    private static JRDesignStaticText bandeau(int x, int y, int l, String texte) {
        JRDesignStaticText t = statique(x, y, l, 14, texte, 8, true, false);
        t.setHorizontalTextAlign(HorizontalTextAlignEnum.CENTER);
        fond(t);
        return t;
    }

    private static void fond(net.sf.jasperreports.engine.design.JRDesignElement e) {
        e.setMode(ModeEnum.OPAQUE);
        e.setBackcolor(GRIS);
    }

    private static JRDesignLine trait(int x, int y, int l) {
        JRDesignLine t = new JRDesignLine();
        t.setX(x);
        t.setY(y);
        t.setWidth(l);
        t.setHeight(0);
        t.getLinePen().setLineWidth(0.5f);
        return t;
    }
}
