package ma.ltexpertise.recherche;

import ma.ltexpertise.commun.Tableau;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.statistiques.ServiceStatistiques;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ma.ltexpertise.statistiques.ServiceStatistiques.SANS_ANNULES;
import static ma.ltexpertise.statistiques.ServiceStatistiques.contient;

/**
 * Recherches transverses sur les dossiers enregistrés : pièces relevées dans les rapports, valeurs d'achat (valeur à
 * neuf) et vénales lues dans le commentaire de l'expert (F12). Les filtres texte cherchent la valeur saisie n'importe
 * où, sans casse ; les résultats sont paginés, triés et filtrés par colonne en base ({@link Tableau}).
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

    private static final Map<String, Tableau.Colonne> COLONNES_PIECES = Map.ofEntries(
            Map.entry("dossier", Tableau.Colonne.simple("r.numero_rapport")),
            Map.entry("piece", Tableau.Colonne.simple("l.designation")),
            Map.entry("type", Tableau.Colonne.libelles("l.type_piece", ServiceStatistiques.LIBELLES_TYPE_PIECE)),
            Map.entry("zone", Tableau.Colonne.libelles("l.zone_choc", libellesZones())),
            Map.entry("marque", Tableau.Colonne.simple("v.marque")),
            Map.entry("modele", Tableau.Colonne.simple("v.modele")),
            Map.entry("typeMine", Tableau.Colonne.simple("v.type_mine")),
            Map.entry("mec", Tableau.Colonne.date("v.date_mise_circulation")),
            Map.entry("chassis", Tableau.Colonne.simple("v.numero_serie")),
            Map.entry("prix", Tableau.Colonne.montant("l.prix_unitaire")),
            Map.entry("date", Tableau.Colonne.date(DATE_DOSSIER)));

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

    /** Valeurs déjà rencontrées, proposées dans les champs de filtre du véhicule. */
    @Transactional(readOnly = true)
    public Suggestions suggestions() {
        MapSqlParameterSource aucun = new MapSqlParameterSource();
        return new Suggestions(
                jdbc.queryForList("select distinct marque from vehicule where marque is not null order by 1", aucun, String.class),
                jdbc.queryForList("select distinct modele from vehicule where modele is not null order by 1", aucun, String.class),
                jdbc.queryForList("select distinct type_mine from vehicule where type_mine is not null order by 1", aucun, String.class));
    }

    /** Lignes de pièces (détail page 2 typé O, A ou R), quel que soit leur prix. */
    @Transactional(readOnly = true)
    public ResultatPieces pieces(FiltresPieces f, Tableau tableau) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder filtre = new StringBuilder(" where l.type_piece is not null" + SANS_ANNULES);
        contient(filtre, p, "l.designation", "piece", f.piece());
        if (f.type() != null && !f.type().isBlank()) {
            filtre.append(" and l.type_piece = :type");
            p.addValue("type", f.type());
        }
        if (f.zone() != null) {
            filtre.append(" and l.zone_choc = :zone");
            p.addValue("zone", f.zone());
        }
        contient(filtre, p, "v.marque", "marque", f.marque());
        contient(filtre, p, "v.modele", "modele", f.modele());
        contient(filtre, p, "v.type_mine", "typeMine", f.typeMine());
        if (f.anneeMec() != null) {
            filtre.append(" and extract(year from v.date_mise_circulation) = :annee");
            p.addValue("annee", f.anneeMec());
        }
        contient(filtre, p, "v.numero_serie", "chassis", f.chassis());
        tableau.filtrer(filtre, p, COLONNES_PIECES);
        String depuis = " from ligne_detail l join rapport_expertise r on r.id = l.rapport_id "
                + "left join vehicule v on v.rapport_id = r.id";

        long nombre = jdbc.queryForObject("select count(*)" + depuis + filtre, p, Long.class);
        List<LignePiece> lignes = jdbc.query("select r.numero_rapport, l.designation, l.type_piece, l.zone_choc, "
                        + "v.marque, v.modele, v.type_mine, v.date_mise_circulation, v.numero_serie, l.prix_unitaire, "
                        + DATE_DOSSIER + " as date_dossier" + depuis + filtre
                        + tableau.ordre(COLONNES_PIECES, DATE_DOSSIER + " desc, r.numero_rapport, l.ordre")
                        + tableau.limite(p), p,
                (rs, i) -> new LignePiece(rs.getString("numero_rapport"), rs.getString("designation"),
                        rs.getString("type_piece"), (Integer) rs.getObject("zone_choc", Integer.class),
                        rs.getString("marque"), rs.getString("modele"), rs.getString("type_mine"),
                        rs.getObject("date_mise_circulation", LocalDate.class), rs.getString("numero_serie"),
                        rs.getBigDecimal("prix_unitaire"), rs.getObject("date_dossier", LocalDate.class)));
        return new ResultatPieces(nombre, lignes, tableau.page(), tableau.taille());
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
        contient(filtre, p, "v.modele", "modele", f.modele());
        contient(filtre, p, "v.type_mine", "typeMine", f.typeMine());
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

    private static Map<String, String> libellesZones() {
        Map<String, String> libelles = new LinkedHashMap<>();
        for (ZoneChoc zone : ZoneChoc.values()) {
            libelles.put(String.valueOf(zone.getCode()), zone.getCode() + " · " + zone.getLibelle());
        }
        return libelles;
    }

    private static BigDecimal arrondi(Object valeur) {
        return valeur == null ? null : ((BigDecimal) valeur).setScale(2, RoundingMode.HALF_UP);
    }

    public record Suggestions(List<String> marques, List<String> modeles, List<String> typesMine) {
    }

    public record FiltresPieces(String piece, String type, Integer zone, String marque, String modele, String typeMine,
                                Integer anneeMec, String chassis) {
    }

    /** @param date date du prix : celle du rapport, à défaut celle de l'import */
    public record LignePiece(String numeroRapport, String designation, String typePiece, Integer zoneChoc,
                             String marque, String modele, String typeMine, LocalDate dateMiseCirculation,
                             String numeroChassis, BigDecimal prixUnitaire, LocalDate date) {
    }

    /** @param lignes page {@code page} (à partir de 0) de {@code taille} lignes sur les {@code nombre} trouvées */
    public record ResultatPieces(long nombre, List<LignePiece> lignes, int page, int taille) {
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
