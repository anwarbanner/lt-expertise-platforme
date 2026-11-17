package ma.ltexpertise.commun;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TableauTest {

    private static final Map<String, Tableau.Colonne> COLONNES = Map.of(
            "piece", Tableau.Colonne.simple("l.designation"),
            "prix", Tableau.Colonne.montant("l.prix_unitaire"));

    @Test
    void litPageTailleTriEtFiltresDeColonne() {
        Tableau t = Tableau.depuis(Map.of("page", "2", "taille", "50", "tri", "prix,desc", "f_piece", " phare ",
                "f_vide", " ", "marque", "AUDI"));

        assertThat(t.page()).isEqualTo(2);
        assertThat(t.taille()).isEqualTo(50);
        assertThat(t.tri()).isEqualTo("prix");
        assertThat(t.descendant()).isTrue();
        assertThat(t.filtres()).containsExactly(Map.entry("piece", "phare"));
    }

    @Test
    void bornesLaTailleEtIgnoreLesValeursInvalides() {
        assertThat(Tableau.depuis(Map.of("taille", "100000", "page", "-3")))
                .extracting(Tableau::taille, Tableau::page).containsExactly(Tableau.TAILLE_MAX, 0);
        assertThat(Tableau.depuis(Map.of("taille", "abc")).taille()).isEqualTo(Tableau.TAILLE_DEFAUT);
    }

    @Test
    void trieEtFiltreSeulementLesColonnesConnues_sansInjecterLaSaisie() {
        Tableau t = Tableau.depuis(Map.of("tri", "1;drop table x", "f_inconnue", "x", "f_piece", "porte' or 1=1"));
        StringBuilder where = new StringBuilder();
        MapSqlParameterSource p = new MapSqlParameterSource();

        t.filtrer(where, p, COLONNES);

        assertThat(t.ordre(COLONNES, "l.id")).isEqualTo(" order by l.id");
        assertThat(where.toString()).contains("l.designation").doesNotContain("porte").doesNotContain("inconnue");
        assertThat(p.getValues()).containsValue("porte' or 1=1");
    }

    @Test
    void trieLaColonneDemandeePuisLOrdreParDefaut() {
        assertThat(Tableau.depuis(Map.of("tri", "prix,asc")).ordre(COLONNES, "l.id"))
                .isEqualTo(" order by l.prix_unitaire asc nulls last, l.id");
    }
}
