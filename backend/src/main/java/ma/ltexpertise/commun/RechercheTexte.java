package ma.ltexpertise.commun;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * Recherche par correspondance partielle : le texte saisi est cherché n'importe où dans la valeur
 * ({@code ILIKE '%texte%'}), sans tenir compte de la casse. Les caractères {@code %}, {@code _} et {@code \} saisis
 * sont cherchés tels quels (échappés), pas interprétés comme jokers.
 */
public final class RechercheTexte {

    /**
     * Valeur d'un filtre qui demande les lignes où le champ n'est pas renseigné (« vide ») plutôt qu'une valeur
     * particulière.
     */
    public static final String VIDE = "__VIDE__";

    /** Caractère d'échappement des motifs (celui de PostgreSQL par défaut). */
    public static final char ECHAPPEMENT = '\\';

    private RechercheTexte() {
    }

    /** Motif {@code %texte%} échappé, pour {@code LIKE} / {@code ILIKE} (caractère d'échappement {@code \}). */
    public static String motif(String valeur) {
        return "%" + echapper(valeur.strip()) + "%";
    }

    /** Échappe les jokers de {@code LIKE} : le texte est cherché littéralement. */
    public static String echapper(String valeur) {
        return valeur.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static boolean renseigne(String valeur) {
        return valeur != null && !valeur.isBlank();
    }

    public static boolean vide(String valeur) {
        return VIDE.equals(valeur);
    }

    /**
     * Ajoute « colonne contient valeur » (sans casse) si la valeur est renseignée, ou « colonne non renseignée » si
     * elle vaut {@link #VIDE}.
     */
    public static void contient(StringBuilder filtre, MapSqlParameterSource p, String colonne, String nom, String valeur) {
        if (vide(valeur)) {
            filtre.append(" and ").append(nonRenseigne(colonne));
        } else if (renseigne(valeur)) {
            filtre.append(" and ").append(colonne).append(" ilike :").append(nom);
            p.addValue(nom, motif(valeur));
        }
    }

    /** Ajoute « colonne égale à valeur » (sans casse), ou « colonne non renseignée » pour {@link #VIDE}. */
    public static void egal(StringBuilder filtre, MapSqlParameterSource p, String colonne, String nom, String valeur) {
        if (vide(valeur)) {
            filtre.append(" and ").append(nonRenseigne(colonne));
        } else if (renseigne(valeur)) {
            filtre.append(" and lower(").append(colonne).append(") = lower(:").append(nom).append(")");
            p.addValue(nom, valeur.strip());
        }
    }

    /** Condition SQL « valeur absente ou blanche ». */
    public static String nonRenseigne(String colonne) {
        return "nullif(btrim(" + colonne + "), '') is null";
    }
}
