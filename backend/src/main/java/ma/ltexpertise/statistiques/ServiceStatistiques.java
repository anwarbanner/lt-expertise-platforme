package ma.ltexpertise.statistiques;

import ma.ltexpertise.commun.Tableau;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tableau de bord (F10) et analyse des prix, calculés en SQL sur les dossiers enregistrés. La date de référence
 * d'un dossier est celle du rapport, à défaut celle de son import.
 */
@Service
public class ServiceStatistiques {

    private static final String DATE_DOSSIER = "coalesce(r.date_rapport, cast(r.cree_le as date))";
    private static final int MOIS_AFFICHES = 12;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock horloge;

    public ServiceStatistiques(NamedParameterJdbcTemplate jdbc, Clock horloge) {
        this.jdbc = jdbc;
        this.horloge = horloge;
    }

    /** @param periode mois sur lequel portent les indicateurs ; {@code null} = tous les dossiers */
    @Transactional(readOnly = true)
    public TableauDeBord tableauDeBord(YearMonth periode) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String filtre = "";
        if (periode != null) {
            filtre = " and " + DATE_DOSSIER + " between :debut and :fin";
            p.addValue("debut", Date.valueOf(periode.atDay(1))).addValue("fin", Date.valueOf(periode.atEndOfMonth()));
        }

        Map<String, Object> kpi = jdbc.queryForMap("select count(*) as nombre, coalesce(sum(r.montant_indemnisation), 0) as total, "
                + "avg(r.montant_indemnisation) as moyenne, "
                + "count(*) filter (where r.statut = 'PRET_A_VALIDER') as en_attente "
                + "from rapport_expertise r where r.statut <> 'EN_COURS'" + filtre, p);

        Map<String, Long> parStatut = new LinkedHashMap<>();
        jdbc.query("select r.statut, count(*) as n from rapport_expertise r where true" + filtre + " group by r.statut", p,
                rs -> { parStatut.put(rs.getString("statut"), rs.getLong("n")); });

        // Un dossier à plusieurs zones compte dans chacune
        List<Repartition> parZone = jdbc.query("select z.zone_choc as cle, count(*) as n from rapport_expertise r "
                + "join rapport_zone_choc z on z.rapport_id = r.id where true" + filtre
                + " group by z.zone_choc order by n desc", p,
                (rs, i) -> new Repartition(String.valueOf(rs.getInt("cle")), rs.getLong("n")));

        List<Repartition> parCompagnie = jdbc.query("select coalesce(r.compagnie, 'Non renseignée') as cle, count(*) as n "
                + "from rapport_expertise r where r.statut <> 'EN_COURS'" + filtre
                + " group by coalesce(r.compagnie, 'Non renseignée') order by n desc limit 5", p,
                (rs, i) -> new Repartition(rs.getString("cle"), rs.getLong("n")));

        YearMonth fin = periode != null ? periode : YearMonth.now(horloge);
        YearMonth debut = fin.minusMonths(MOIS_AFFICHES - 1);
        Map<String, Mois> mensuel = new LinkedHashMap<>();
        for (YearMonth m = debut; !m.isAfter(fin); m = m.plusMonths(1)) {
            mensuel.put(m.toString(), new Mois(m.toString(), 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        jdbc.query("select to_char(" + DATE_DOSSIER + ", 'YYYY-MM') as mois, count(*) as n, "
                        + "coalesce(sum(r.montant_indemnisation), 0) as montant, coalesce(sum(r.total_mo_ttc), 0) as mo, "
                        + "coalesce(sum(r.total_fournitures_ttc), 0) as fournitures from rapport_expertise r "
                        + "where r.statut <> 'EN_COURS' and " + DATE_DOSSIER + " between :debutMois and :finMois group by 1",
                new MapSqlParameterSource().addValue("debutMois", Date.valueOf(debut.atDay(1)))
                        .addValue("finMois", Date.valueOf(fin.atEndOfMonth())),
                rs -> {
                    mensuel.put(rs.getString("mois"), new Mois(rs.getString("mois"), rs.getLong("n"),
                            rs.getBigDecimal("montant"), rs.getBigDecimal("mo"), rs.getBigDecimal("fournitures")));
                });

        List<Alerte> alertes = jdbc.query("select r.numero_rapport, "
                + "count(*) filter (where c.type = 'COHERENCE') as incoherences, "
                + "count(*) filter (where c.type = 'CHAMP_MANQUANT') as manquants, "
                + "min(c.libelle) filter (where c.type = 'COHERENCE') as premier "
                + "from rapport_expertise r join resultat_controle c on c.rapport_id = r.id and not c.conforme and c.bloquant "
                + "where r.statut not in ('VALIDE', 'ARCHIVE')" + filtre
                + " group by r.id, r.numero_rapport order by max(r.cree_le) desc limit 6", p,
                (rs, i) -> new Alerte(rs.getString("numero_rapport"), rs.getLong("incoherences"),
                        rs.getLong("manquants"), rs.getString("premier")));
        long dossiersEnAnomalie = jdbc.queryForObject("select count(distinct r.id) from rapport_expertise r "
                + "join resultat_controle c on c.rapport_id = r.id and not c.conforme and c.bloquant "
                + "where r.statut not in ('VALIDE', 'ARCHIVE')" + filtre, p, Long.class);

        List<DernierDossier> derniers = jdbc.query("select r.numero_rapport, r.cree_le, r.compagnie, "
                + "r.montant_indemnisation, r.statut from rapport_expertise r where r.numero_rapport is not null"
                + filtre + " order by r.cree_le desc limit 5", p,
                (rs, i) -> new DernierDossier(rs.getString("numero_rapport"),
                        rs.getObject("cree_le", OffsetDateTime.class), rs.getString("compagnie"),
                        rs.getBigDecimal("montant_indemnisation"), rs.getString("statut")));

        BigDecimal moyenne = (BigDecimal) kpi.get("moyenne");
        return new TableauDeBord(periode == null ? null : periode.toString(), ((Number) kpi.get("nombre")).longValue(),
                (BigDecimal) kpi.get("total"), moyenne == null ? null : moyenne.setScale(2, RoundingMode.HALF_UP),
                ((Number) kpi.get("en_attente")).longValue(), parStatut, new ArrayList<>(mensuel.values()), parZone,
                parCompagnie, alertes, dossiersEnAnomalie, derniers);
    }

    /** Libellés affichés des types de pièce, filtrés par les colonnes « Type » des tableaux. */
    public static final Map<String, String> LIBELLES_TYPE_PIECE = Map.of(
            "ORIGINE", "Origine", "ADAPTABLE", "Adaptable", "REEMPLOI", "Réemploi");

    /** Colonnes du tableau de l'analyse des prix (clés partagées avec l'écran). */
    static final Map<String, Tableau.Colonne> COLONNES_PRIX = Map.of(
            "dossier", Tableau.Colonne.simple("r.numero_rapport"),
            "designation", Tableau.Colonne.simple("l.designation"),
            "type", Tableau.Colonne.libelles("l.type_piece", LIBELLES_TYPE_PIECE),
            "marque", Tableau.Colonne.simple("v.marque"),
            "modele", Tableau.Colonne.simple("v.modele"),
            "typeMine", Tableau.Colonne.simple("v.type_mine"),
            "prix", Tableau.Colonne.montant("l.prix_unitaire"),
            "date", Tableau.Colonne.date(DATE_DOSSIER));

    /**
     * Prix unitaires des pièces (lignes de détail typées O, A ou R, prix positif), filtrables par désignation, type
     * de pièce et véhicule (analyse des prix), puis par colonne du tableau. Les indicateurs portent sur toutes les
     * lignes filtrées, le détail sur la page demandée. Les filtres texte cherchent la valeur saisie n'importe où.
     */
    @Transactional(readOnly = true)
    public AnalysePrix prix(FiltresPrix f, Tableau tableau) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder filtre = new StringBuilder(" where l.prix_unitaire > 0 and l.type_piece is not null");
        contient(filtre, p, "l.designation", "designation", f.designation());
        if (f.type() != null && !f.type().isBlank()) {
            filtre.append(" and l.type_piece = :type");
            p.addValue("type", f.type());
        }
        contient(filtre, p, "v.marque", "marque", f.marque());
        contient(filtre, p, "v.modele", "modele", f.modele());
        contient(filtre, p, "v.type_mine", "typeMine", f.typeMine());
        tableau.filtrer(filtre, p, COLONNES_PRIX);
        String depuis = " from ligne_detail l join rapport_expertise r on r.id = l.rapport_id "
                + "left join vehicule v on v.rapport_id = r.id";

        Map<String, Object> agregats = jdbc.queryForMap("select count(*) as n, min(l.prix_unitaire) as mini, "
                + "avg(l.prix_unitaire) as moyen, max(l.prix_unitaire) as maxi" + depuis + filtre, p);
        Map<String, BigDecimal> moyenneParType = new LinkedHashMap<>();
        jdbc.query("select l.type_piece, avg(l.prix_unitaire) as moyen" + depuis + filtre + " group by l.type_piece "
                + "order by l.type_piece", p, rs -> {
            moyenneParType.put(rs.getString("type_piece"), rs.getBigDecimal("moyen").setScale(2, RoundingMode.HALF_UP));
        });
        List<LignePrix> lignes = jdbc.query("select r.numero_rapport, l.designation, l.type_piece, v.marque, v.modele, "
                + "v.type_mine, l.prix_unitaire, " + DATE_DOSSIER + " as date_dossier" + depuis + filtre
                + tableau.ordre(COLONNES_PRIX, DATE_DOSSIER + " desc, l.designation, l.id") + tableau.limite(p), p,
                (rs, i) -> new LignePrix(rs.getString("numero_rapport"), rs.getString("designation"),
                        rs.getString("type_piece"), rs.getString("marque"), rs.getString("modele"),
                        rs.getString("type_mine"), rs.getBigDecimal("prix_unitaire"),
                        rs.getObject("date_dossier", LocalDate.class)));

        return new AnalysePrix(((Number) agregats.get("n")).longValue(), arrondi(agregats.get("mini")),
                arrondi(agregats.get("moyen")), arrondi(agregats.get("maxi")), moyenneParType, lignes,
                tableau.page(), tableau.taille());
    }

    /** Ajoute « colonne contient valeur » (sans casse) si la valeur est renseignée. */
    public static void contient(StringBuilder filtre, MapSqlParameterSource p, String colonne, String nom, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            filtre.append(" and ").append(colonne).append(" ilike :").append(nom);
            p.addValue(nom, "%" + valeur.strip() + "%");
        }
    }

    private static BigDecimal arrondi(Object valeur) {
        return valeur == null ? null : ((BigDecimal) valeur).setScale(2, RoundingMode.HALF_UP);
    }

    public record Repartition(String cle, long nombre) {
    }

    public record Mois(String mois, long nombre, BigDecimal montant, BigDecimal mainOeuvreTtc, BigDecimal fournituresTtc) {
    }

    public record Alerte(String numeroRapport, long incoherences, long champsManquants, String premierControle) {
    }

    public record DernierDossier(String numeroRapport, OffsetDateTime importeLe, String compagnie,
                                 BigDecimal montantIndemnisation, String statut) {
    }

    /** Les montants valent {@code null} dans la vue sans montants (rôles autres qu'administrateur, P9). */
    public record TableauDeBord(String periode, long dossiers, BigDecimal montantTotal, BigDecimal coutMoyen,
                                long enAttenteValidation, Map<String, Long> parStatut, List<Mois> mensuel,
                                List<Repartition> parZone, List<Repartition> parCompagnie, List<Alerte> alertes,
                                long dossiersEnAnomalie, List<DernierDossier> derniersDossiers) {

        /** Même tableau, sans aucun montant : seuls les nombres de dossiers restent. */
        public TableauDeBord sansMontants() {
            return new TableauDeBord(periode, dossiers, null, null, enAttenteValidation, parStatut,
                    mensuel.stream().map(m -> new Mois(m.mois(), m.nombre(), null, null, null)).toList(),
                    parZone, parCompagnie, alertes, dossiersEnAnomalie,
                    derniersDossiers.stream().map(d -> new DernierDossier(d.numeroRapport(), d.importeLe(),
                            d.compagnie(), null, d.statut())).toList());
        }
    }

    /** @param date date du prix : celle du rapport, à défaut celle de l'import */
    public record LignePrix(String numeroRapport, String designation, String typePiece, String marque, String modele,
                            String typeMine, BigDecimal prixUnitaire, LocalDate date) {
    }

    public record FiltresPrix(String designation, String type, String marque, String modele, String typeMine) {
    }

    /** @param lignes page {@code page} (à partir de 0) de {@code taille} lignes sur les {@code nombre} filtrées */
    public record AnalysePrix(long nombre, BigDecimal minimum, BigDecimal moyenne, BigDecimal maximum,
                              Map<String, BigDecimal> moyenneParType, List<LignePrix> lignes, int page, int taille) {
    }
}
