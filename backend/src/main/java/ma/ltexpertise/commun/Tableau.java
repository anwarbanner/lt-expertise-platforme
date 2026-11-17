package ma.ltexpertise.commun;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Page, tri et filtres par colonne d'un tableau de résultats, lus dans les paramètres de la requête
 * ({@code page}, {@code taille}, {@code tri=cle,asc|desc}, {@code f_<cle>=texte}) et traduits en SQL sur une liste
 * blanche de colonnes : une clé inconnue est ignorée, aucune saisie n'entre dans le texte de la requête.
 *
 * <p>Le filtre d'une colonne cherche le texte saisi dans la valeur telle qu'affichée, sans tenir compte de la casse,
 * des accents ni des espaces (« reemploi » trouve « Réemploi », « 1000 » trouve « 1 000,00 »).
 */
public record Tableau(int page, int taille, String tri, boolean descendant, Map<String, String> filtres) {

    public static final int TAILLE_DEFAUT = 25;
    public static final int TAILLE_MAX = 200;
    private static final String PREFIXE_FILTRE = "f_";
    private static final String AVEC_ACCENTS = "àâäáãéèêëíìîïóòôöõúùûüçñ";
    private static final String SANS_ACCENTS = "aaaaaeeeeiiiiooooouuuucn";

    /**
     * Colonne d'un tableau.
     *
     * @param valeur expression SQL de tri
     * @param texte  expression SQL du texte affiché, sur lequel porte le filtre de la colonne
     */
    public record Colonne(String valeur, String texte) {

        /** Colonne affichée telle quelle (texte). */
        public static Colonne simple(String expression) {
            return new Colonne(expression, expression);
        }

        /** Montant affiché « 1 000,00 » : filtré sur « 1000,00 » (les espaces sont ignorés). */
        public static Colonne montant(String expression) {
            return new Colonne(expression, "replace(to_char(" + expression + ", 'FM999999999990.00'), '.', ',')");
        }

        /** Date affichée « 17/10/2026 ». */
        public static Colonne date(String expression) {
            return new Colonne(expression, "to_char(" + expression + ", 'DD/MM/YYYY')");
        }

        /** Code d'énumération trié par code, filtré sur son libellé affiché. */
        public static Colonne libelles(String expression, Map<String, String> libelleParCode) {
            StringBuilder cas = new StringBuilder("case cast(" + expression + " as text)");
            libelleParCode.forEach((code, libelle) -> cas.append(" when '").append(code.replace("'", "''"))
                    .append("' then '").append(libelle.replace("'", "''")).append("'"));
            return new Colonne(expression, cas.append(" end").toString());
        }
    }

    public static Tableau depuis(Map<String, String> parametres) {
        int page = Math.max(0, entier(parametres.get("page"), 0));
        int taille = Math.clamp(entier(parametres.get("taille"), TAILLE_DEFAUT), 1, TAILLE_MAX);
        String tri = null;
        boolean descendant = false;
        String valeurTri = parametres.get("tri");
        if (valeurTri != null && !valeurTri.isBlank()) {
            String[] morceaux = valeurTri.split(",", 2);
            tri = morceaux[0].strip();
            descendant = morceaux.length > 1 && "desc".equalsIgnoreCase(morceaux[1].strip());
        }
        Map<String, String> filtres = new LinkedHashMap<>();
        parametres.forEach((cle, valeur) -> {
            if (cle.startsWith(PREFIXE_FILTRE) && valeur != null && !valeur.isBlank()) {
                filtres.put(cle.substring(PREFIXE_FILTRE.length()), valeur.strip());
            }
        });
        return new Tableau(page, taille, tri, descendant, filtres);
    }

    /** Toutes les lignes, sans tri ni filtre de colonne : exports. */
    public static Tableau tout(int maximum) {
        return new Tableau(0, maximum, null, false, Map.of());
    }

    /** Ajoute au {@code where} un filtre « contient » par colonne connue renseignée. */
    public void filtrer(StringBuilder where, MapSqlParameterSource p, Map<String, Colonne> colonnes) {
        int i = 0;
        for (Map.Entry<String, String> filtre : filtres.entrySet()) {
            Colonne colonne = colonnes.get(filtre.getKey());
            if (colonne == null) {
                continue;
            }
            String nom = "colonne" + i++;
            where.append(" and ").append(normaliser("cast(" + colonne.texte() + " as text)"))
                    .append(" like '%' || ").append(normaliser(":" + nom)).append(" || '%'");
            p.addValue(nom, filtre.getValue());
        }
    }

    /** {@code order by} : la colonne triée (vides en dernier), puis l'ordre par défaut pour départager. */
    public String ordre(Map<String, Colonne> colonnes, String ordreParDefaut) {
        Colonne colonne = tri == null ? null : colonnes.get(tri);
        if (colonne == null) {
            return " order by " + ordreParDefaut;
        }
        return " order by " + colonne.valeur() + (descendant ? " desc" : " asc") + " nulls last, " + ordreParDefaut;
    }

    /** {@code limit / offset} de la page demandée. */
    public String limite(MapSqlParameterSource p) {
        p.addValue("taillePage", taille).addValue("decalage", (long) page * taille);
        return " limit :taillePage offset :decalage";
    }

    private static String normaliser(String expression) {
        return "translate(replace(lower(" + expression + "), ' ', ''), '" + AVEC_ACCENTS + "', '" + SANS_ACCENTS + "')";
    }

    private static int entier(String valeur, int defaut) {
        try {
            return valeur == null ? defaut : Integer.parseInt(valeur.strip());
        } catch (NumberFormatException e) {
            return defaut;
        }
    }
}
