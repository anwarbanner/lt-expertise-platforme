package ma.ltexpertise.recherche;

import ma.ltexpertise.commun.RechercheTexte;
import ma.ltexpertise.commun.Tableau;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static ma.ltexpertise.statistiques.ServiceStatistiques.SANS_ANNULES;
import static ma.ltexpertise.commun.RechercheTexte.contient;

/**
 * Recherches transverses sur les dossiers enregistrés : valeurs d'achat (valeur à neuf) et vénales lues dans le
 * commentaire de l'expert (F12), et suggestions des filtres du véhicule. Les filtres texte cherchent la valeur saisie
 * n'importe où, sans casse ({@link RechercheTexte}) ; les résultats sont paginés, triés et filtrés par colonne en base ({@link Tableau}).
 */
@Service
public class ServiceRecherches {

    private static final String DATE_DOSSIER = "coalesce(r.date_rapport, cast(r.cree_le as date))";
    /** Âge du véhicule en mois révolus, de la mise en circulation au sinistre. */
    private static final String AGE_MOIS = "(extract(year from age(r.date_sinistre, v.date_mise_circulation)) * 12 "
            + "+ extract(month from age(r.date_sinistre, v.date_mise_circulation)))::int";
    /** Âge affiché « 3 ans 11 mois », comme à l'écran. */
    private static final String AGE_TEXTE = "concat_ws(' ', case when " + AGE_MOIS + " >= 12 then (" + AGE_MOIS
            + " / 12) || case when " + AGE_MOIS + " >= 24 then ' ans' else ' an' end end, case when " + AGE_MOIS
            + " % 12 > 0 or " + AGE_MOIS + " < 12 then (" + AGE_MOIS + " % 12) || ' mois' end)";

    private static final Map<String, Tableau.Colonne> COLONNES_VALEURS = Map.ofEntries(
            Map.entry("dossier", Tableau.Colonne.simple("r.numero_rapport")),
            Map.entry("marque", Tableau.Colonne.simple("v.marque")),
            Map.entry("modele", Tableau.Colonne.simple("v.modele")),
            Map.entry("typeMine", Tableau.Colonne.simple("v.type_mine")),
            Map.entry("dmec", Tableau.Colonne.date("v.date_mise_circulation")),
            Map.entry("sinistre", Tableau.Colonne.date("r.date_sinistre")),
            Map.entry("age", new Tableau.Colonne(AGE_MOIS, AGE_TEXTE)),
            Map.entry("achat", Tableau.Colonne.montant("achat.valeur_montant")),
            Map.entry("venale", Tableau.Colonne.montant("venale.valeur_montant")));

    private final NamedParameterJdbcTemplate jdbc;

    public ServiceRecherches(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Valeurs déjà rencontrées (dossiers non annulés), proposées dans les filtres du véhicule. Les modèles se limitent
     * à la marque saisie (correspondance partielle) ; les types mine sont ceux du modèle choisi, ou tous les types mine
     * connus si aucun modèle n'est choisi.
     */
    @Transactional(readOnly = true)
    public Suggestions suggestions(String marque, String modele) {
        String depuis = " from vehicule v join rapport_expertise r on r.id = v.rapport_id where true" + SANS_ANNULES;
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder parMarque = new StringBuilder();
        RechercheTexte.contient(parMarque, p, "v.marque", "marque", marque);
        StringBuilder parModele = new StringBuilder();
        RechercheTexte.egal(parModele, p, "v.modele", "modele", modele);
        return new Suggestions(
                distinctes("v.marque", depuis, p),
                distinctes("v.modele", depuis + parMarque, p),
                distinctes("v.type_mine", depuis + parModele, p));
    }

    private List<String> distinctes(String colonne, String depuis, MapSqlParameterSource p) {
        return jdbc.queryForList("select distinct btrim(" + colonne + ")" + depuis + " and "
                + "nullif(btrim(" + colonne + "), '') is not null order by 1", p, String.class);
    }

    /**
     * Dossiers portant une valeur d'achat (valeur à neuf) ou une valeur vénale dans le commentaire. L'âge va de la
     * date de mise en circulation à la date du sinistre ; inconnu si l'une des deux manque.
     */
    @Transactional(readOnly = true)
    public ResultatValeurs valeurs(FiltresValeurs f, Tableau tableau) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder filtre = new StringBuilder(" where (achat.valeur_montant is not null or venale.valeur_montant is not null)"
                + SANS_ANNULES);
        contient(filtre, p, "v.marque", "marque", f.marque());
        // Modèle et type mine choisis dans la liste des valeurs connues (ou « non renseigné ») : égalité sans casse
        RechercheTexte.egal(filtre, p, "v.modele", "modele", f.modele());
        RechercheTexte.egal(filtre, p, "v.type_mine", "typeMine", f.typeMine());
        entre(filtre, p, "v.date_mise_circulation", "mec", f.mecDu() == null ? null : Date.valueOf(f.mecDu()),
                f.mecAu() == null ? null : Date.valueOf(f.mecAu()));
        entre(filtre, p, AGE_MOIS, "age", f.ageMinMois(), f.ageMaxMois());
        entre(filtre, p, "achat.valeur_montant", "achat", f.achatMin(), f.achatMax());
        entre(filtre, p, "venale.valeur_montant", "venale", f.venaleMin(), f.venaleMax());
        tableau.filtrer(filtre, p, COLONNES_VALEURS);
        String depuis = " from rapport_expertise r left join vehicule v on v.rapport_id = r.id "
                + "left join valeur_commentaire achat on achat.rapport_id = r.id and achat.cle = 'VALEUR_NEUF_CATALOGUE' "
                + "left join valeur_commentaire venale on venale.rapport_id = r.id and venale.cle = 'VALEUR_VENALE_FMA'";

        Map<String, Object> agregats = jdbc.queryForMap("select count(*) as n, avg(achat.valeur_montant) as achat, "
                + "avg(venale.valeur_montant) as venale" + depuis + filtre, p);
        List<LigneValeur> lignes = jdbc.query("select r.numero_rapport, v.marque, v.modele, v.type_mine, "
                        + "v.date_mise_circulation, r.date_sinistre, " + AGE_MOIS + " as age_mois, "
                        + "achat.valeur_montant as achat, venale.valeur_montant as venale" + depuis + filtre
                        + tableau.ordre(COLONNES_VALEURS, DATE_DOSSIER + " desc, r.numero_rapport") + tableau.limite(p), p,
                (rs, i) -> new LigneValeur(rs.getString("numero_rapport"), rs.getString("marque"),
                        rs.getString("modele"), rs.getString("type_mine"),
                        rs.getObject("date_mise_circulation", LocalDate.class),
                        rs.getObject("date_sinistre", LocalDate.class), (Integer) rs.getObject("age_mois", Integer.class),
                        rs.getBigDecimal("achat"), rs.getBigDecimal("venale")));
        return new ResultatValeurs(((Number) agregats.get("n")).longValue(), arrondi(agregats.get("achat")),
                arrondi(agregats.get("venale")), lignes, tableau.page(), tableau.taille());
    }

    private static void entre(StringBuilder filtre, MapSqlParameterSource p, String expression, String nom,
                              Object minimum, Object maximum) {
        if (minimum != null) {
            filtre.append(" and ").append(expression).append(" >= :").append(nom).append("Min");
            p.addValue(nom + "Min", minimum);
        }
        if (maximum != null) {
            filtre.append(" and ").append(expression).append(" <= :").append(nom).append("Max");
            p.addValue(nom + "Max", maximum);
        }
    }

    private static BigDecimal arrondi(Object valeur) {
        return valeur == null ? null : ((BigDecimal) valeur).setScale(2, RoundingMode.HALF_UP);
    }

    public record Suggestions(List<String> marques, List<String> modeles, List<String> typesMine) {
    }

    /** Âges en mois révolus (le formulaire saisit des années et des mois). */
    public record FiltresValeurs(String marque, String modele, String typeMine, LocalDate mecDu, LocalDate mecAu,
                                 Integer ageMinMois, Integer ageMaxMois, BigDecimal achatMin, BigDecimal achatMax,
                                 BigDecimal venaleMin, BigDecimal venaleMax) {
    }

    /** @param valeurAchat valeur à neuf du véhicule lue dans le commentaire */
    public record LigneValeur(String numeroRapport, String marque, String modele, String typeMine,
                              LocalDate dateMiseCirculation, LocalDate dateSinistre, Integer ageMois,
                              BigDecimal valeurAchat, BigDecimal valeurVenale) {
    }

    public record ResultatValeurs(long nombre, BigDecimal moyenneAchat, BigDecimal moyenneVenale, List<LigneValeur> lignes,
                                  int page, int taille) {
    }
}
