package ma.ltexpertise.extraction;

import ma.ltexpertise.dossier.referentiel.TypePiece;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cas limites du gabarit, sur des lignes construites à la main (positions reprises des rapports réels). */
class ParseurTcpdfExpertiseV1Test {

    private final ParseurTcpdfExpertiseV1 parseur = new ParseurTcpdfExpertiseV1();

    @Test
    void refuseUnRapportDUneSeulePage() {
        assertThatThrownBy(() -> parseur.parser(List.of(page1())))
                .isInstanceOf(GabaritNonReconnuException.class)
                .hasMessageContaining("1 page(s)");
    }

    @Test
    void refuseUnePage1SansTitre() {
        List<Ligne> sansTitre = new ArrayList<>(page1());
        sansTitre.removeFirst();

        assertThatThrownBy(() -> parseur.parser(List.of(sansTitre, List.of(entetePage2()))))
                .isInstanceOf(GabaritNonReconnuException.class)
                .hasMessageContaining("titre");
    }

    @Test
    void refuseUnTableauEvaluationIncomplet() {
        List<Ligne> sansTotal = new ArrayList<>(page1());
        sansTotal.removeLast();

        assertThatThrownBy(() -> parseur.parser(List.of(sansTotal, List.of(entetePage2()))))
                .isInstanceOf(GabaritNonReconnuException.class)
                .hasMessageContaining("Évaluation des dommages");
    }

    @Test
    void refuseUnePage2SansEnTeteDeTableau() {
        List<Ligne> page2 = List.of(ligne(109, "1@46", "MOT@63", "0.00@160"));

        assertThatThrownBy(() -> parseur.parser(List.of(page1(), page2)))
                .isInstanceOf(GabaritNonReconnuException.class)
                .hasMessageContaining("en-tête du tableau");
    }

    @Test
    void refuseUnEnTeteDeTableauIncomplet() {
        Ligne enteteTronque = ligne(75, "Qte@46", "Désignation@62", "Type@114", "Prix@138");

        assertThatThrownBy(() -> parseur.parser(List.of(page1(), List.of(enteteTronque))))
                .isInstanceOf(GabaritNonReconnuException.class)
                .hasMessageContaining("vetuste");
    }

    @Test
    void calculeLesColonnesQuandDeuxTitresSontColles() {
        // « DésignationType » : cellules contiguës, sans espace ni écart (rapports de réforme).
        Ligne entete = new Ligne(75, List.of(
                new Mot(46, 58, 75, "Qte"), new Mot(62, 130, 75, "DésignationType"), new Mot(134, 152, 75, "Prix"),
                new Mot(173, 210, 75, "Vétusté%"), new Mot(220, 233, 75, "Tva"), new Mot(248, 268, 75, "Total"),
                new Mot(287, 335, 75, "Désignation"), new Mot(341, 355, 75, "Nbr"), new Mot(365, 385, 75, "Tarif"),
                new Mot(389, 420, 75, "Montant")));

        double[] colonnes = ParseurTcpdfExpertiseV1.colonnes(entete);

        assertThat(colonnes[1]).isEqualTo(62);
        assertThat(colonnes[2]).isBetween(105.0, 115.0); // « Type » estimé dans le mot collé
        assertThat(colonnes[3]).isEqualTo(134);
        assertThat(colonnes[10]).isEqualTo(389);
    }

    @Test
    void litUneLigneDeDetailEtSonType() {
        List<Ligne> page2 = List.of(
                entetePage2(),
                ligne(86, "Avant@46", ".@253"),
                ligne(98, "·@46", "Tolerie@51", ".@225"),
                ligne(109, "1@46", "CAPTEUR@63", "DE@100", "DISTANCE@113", "R@116", "2370.00@151", "10.00@206",
                        "20.00@230", "2559.60@258", "Echange@292", "0.00@346", "0.00@375", "0.00@429"),
                ligne(165, "O:@43", "Pièce@56"));

        RapportExtrait r = parseur.parser(List.of(page1(), page2));

        assertThat(r.getLignesDetail()).singleElement().satisfies(l -> {
            assertThat(l.getDesignation()).isEqualTo("CAPTEUR DE DISTANCE");
            assertThat(l.getTypePiece()).isEqualTo(TypePiece.REEMPLOI);
            assertThat(l.getPrixUnitaire()).isEqualByComparingTo("2370.00");
            assertThat(l.getVetustePct()).isEqualByComparingTo("10");
            assertThat(l.getTotalFourniture()).isEqualByComparingTo("2559.60");
            assertThat(l.getZoneLibelle()).isEqualTo("Avant");
        });
    }

    // ------------------------------------------------------------------ données synthétiques

    /** Page 1 minimale : titre, 7 postes de main d'œuvre et total général (positions du rapport de référence). */
    private static List<Ligne> page1() {
        List<Ligne> lignes = new ArrayList<>();
        lignes.add(ligne(48, "RAPPORT@193", "D'EXPERTISE@252", ":@329", "268202-9999@336"));
        lignes.add(ligne(392, "EVALUATION@244", "DES@299", "DOMMAGES@319"));
        String[][] postes = {{"MO", "1", "Tôlerie"}, {"MO", "2", "Tôlerie"}, {"MO", "3", "Tôlerie"}, {"MO", "Peinture"},
                {"MO", "1", "Mécanique"}, {"MO", "2", "Mécanique"}, {"MO", "3", "Mécanique"}};
        double y = 414;
        for (String[] poste : postes) {
            List<String> mots = new ArrayList<>();
            double x = 49;
            for (String m : poste) {
                mots.add(m + "@" + x);
                x += 16;
            }
            mots.addAll(List.of("0.00@162", "0.00@272", "0.00@363", "0.00@455", "0.00@547"));
            lignes.add(ligne(y, mots.toArray(String[]::new)));
            y += 12.7;
        }
        lignes.add(ligne(580, "Total@49", "général@71", "0.00@363", "0.00@455", "0.00@547"));
        return lignes;
    }

    private static Ligne entetePage2() {
        return ligne(75, "Qte@46", "Désignation@62", "Type@114", "Prix@138", "Vétusté%@177", "Tva@224", "%@241",
                "Total@252", "Désignation@291", "Nbr@345", "H@361", "Tarif@369", "Montant@393", "TTC@427");
    }

    /** Ligne à partir de « texte@x » ; la fin d'un mot est estimée à 5 pt par caractère. */
    private static Ligne ligne(double y, String... mots) {
        List<Mot> liste = new ArrayList<>();
        for (String m : mots) {
            int arobase = m.lastIndexOf('@');
            String texte = m.substring(0, arobase);
            double x = Double.parseDouble(m.substring(arobase + 1));
            liste.add(new Mot(x, x + 5 * texte.length(), y, texte));
        }
        return new Ligne(y, liste);
    }
}
