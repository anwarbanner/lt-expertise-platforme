package ma.ltexpertise.statistiques;

import ma.ltexpertise.commun.RechercheTexte;
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
import java.util.Comparator;
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

    /** Condition SQL (alias {@code r}) qui écarte les imports annulés de toutes les statistiques et recherches (P6). */
    public static final String SANS_ANNULES = " and r.statut <> 'ANNULE'";

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
        // Un import annulé n'entre dans aucun indicateur (P6)
        String filtre = SANS_ANNULES;
        if (periode != null) {
            filtre += " and " + DATE_DOSSIER + " between :debut and :fin";
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
                        + "where r.statut <> 'EN_COURS'" + SANS_ANNULES + " and " + DATE_DOSSIER
                        + " between :debutMois and :finMois group by 1",
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

    /** Type de pièce non renseigné sur la ligne (ni O, ni A, ni R) : catégorie « Vide » de l'analyse. */
    public static final String TYPE_VIDE = "VIDE";
    private static final Map<String, String> LIBELLES_TYPE_OU_VIDE = Map.of(
            "ORIGINE", "Origine", "ADAPTABLE", "Adaptable", "REEMPLOI", "Réemploi", TYPE_VIDE, "Non renseigné");
    private static final String TYPE_OU_VIDE = "coalesce(l.type_piece, '" + TYPE_VIDE + "')";
    private static final List<String> ORDRE_TYPES = List.of("ORIGINE", "ADAPTABLE", "REEMPLOI", TYPE_VIDE);

    /** Colonnes du tableau de l'analyse des prix (clés partagées avec l'écran). */
    static final Map<String, Tableau.Colonne> COLONNES_PRIX = Map.of(
            "dossier", Tableau.Colonne.simple("r.numero_rapport"),
            "designation", Tableau.Colonne.simple("l.designation"),
            "type", Tableau.Colonne.libelles(TYPE_OU_VIDE, LIBELLES_TYPE_OU_VIDE),
            "marque", Tableau.Colonne.simple("v.marque"),
            "modele", Tableau.Colonne.simple("v.modele"),
            "typeMine", Tableau.Colonne.simple("v.type_mine"),
            "prix", Tableau.Colonne.montant("l.prix_unitaire"),
            "date", Tableau.Colonne.date(DATE_DOSSIER));

    /**
     * Analyse des prix des pièces relevées dans les rapports (lignes de détail typées O, A ou R, ou portant un prix
     * unitaire sans type). Filtres : désignation, type (ou type vide), zones de choc (une ou plusieurs), marque,
     * modèle, type mine, année de MEC et n° de châssis ; chaque champ du véhicule peut être demandé « vide »
     * ({@link RechercheTexte#VIDE}). Les filtres texte cherchent la valeur saisie n'importe où, sans casse ; le modèle
     * et le type mine, choisis dans la liste des valeurs connues, sont comparés exactement (sans casse).
     *
     * <p>Les indicateurs (dossiers trouvés, prix minimum / moyen / maximum, répartition par type) portent sur toutes
     * les lignes filtrées, les prix sur les prix unitaires positifs seulement ; le détail porte sur la page demandée.
     */
    @Transactional(readOnly = true)
    public AnalysePrix prix(FiltresPrix f, Tableau tableau) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder filtre = new StringBuilder(" where (l.type_piece is not null or l.prix_unitaire > 0)" + SANS_ANNULES);
        RechercheTexte.contient(filtre, p, "l.designation", "designation", f.designation());
        if (RechercheTexte.vide(f.type()) || TYPE_VIDE.equals(f.type())) {
            filtre.append(" and l.type_piece is null");
        } else if (RechercheTexte.renseigne(f.type())) {
            filtre.append(" and l.type_piece = :type");
            p.addValue("type", f.type().strip());
        }
        if (f.zones() != null && !f.zones().isEmpty()) {
            filtre.append(" and l.zone_choc in (:zones)");
            p.addValue("zones", f.zones());
        }
        RechercheTexte.contient(filtre, p, "v.marque", "marque", f.marque());
        RechercheTexte.egal(filtre, p, "v.modele", "modele", f.modele());
        RechercheTexte.egal(filtre, p, "v.type_mine", "typeMine", f.typeMine());
        if (RechercheTexte.vide(f.annee())) {
            filtre.append(" and v.date_mise_circulation is null");
        } else if (RechercheTexte.renseigne(f.annee()) && f.annee().strip().matches("\\d{1,4}")) {
            filtre.append(" and extract(year from v.date_mise_circulation) = :annee");
            p.addValue("annee", Integer.parseInt(f.annee().strip()));
        }
        RechercheTexte.contient(filtre, p, "v.numero_serie", "chassis", f.chassis());
        tableau.filtrer(filtre, p, COLONNES_PRIX);
        String depuis = " from ligne_detail l join rapport_expertise r on r.id = l.rapport_id "
                + "left join vehicule v on v.rapport_id = r.id";
        String prixPositif = " filter (where l.prix_unitaire > 0)";

        Map<String, Object> agregats = jdbc.queryForMap("select count(*) as n, count(distinct r.id) as dossiers, "
                + "min(l.prix_unitaire)" + prixPositif + " as mini, avg(l.prix_unitaire)" + prixPositif + " as moyen, "
                + "max(l.prix_unitaire)" + prixPositif + " as maxi" + depuis + filtre, p);
        List<RepartitionType> parType = jdbc.query("select " + TYPE_OU_VIDE + " as type, count(*) as n, "
                        + "avg(l.prix_unitaire)" + prixPositif + " as moyen" + depuis + filtre + " group by 1", p,
                (rs, i) -> new RepartitionType(rs.getString("type"), rs.getLong("n"), arrondi(rs.getBigDecimal("moyen"))))
                .stream().sorted(Comparator.comparingInt(t -> ORDRE_TYPES.indexOf(t.type()))).toList();
        List<LignePrix> lignes = jdbc.query("select r.numero_rapport, l.designation, l.type_piece, v.marque, v.modele, "
                + "v.type_mine, l.prix_unitaire, " + DATE_DOSSIER + " as date_dossier" + depuis + filtre
                + tableau.ordre(COLONNES_PRIX, DATE_DOSSIER + " desc, r.numero_rapport, l.ordre") + tableau.limite(p), p,
                (rs, i) -> new LignePrix(rs.getString("numero_rapport"), rs.getString("designation"),
                        rs.getString("type_piece"), rs.getString("marque"), rs.getString("modele"),
                        rs.getString("type_mine"), rs.getBigDecimal("prix_unitaire"),
                        rs.getObject("date_dossier", LocalDate.class)));

        return new AnalysePrix(((Number) agregats.get("n")).longValue(), ((Number) agregats.get("dossiers")).longValue(),
                arrondi(agregats.get("mini")), arrondi(agregats.get("moyen")), arrondi(agregats.get("maxi")), parType,
                lignes, tableau.page(), tableau.taille());
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

    /**
     * @param typePiece {@code null} si le type n'est pas renseigné
     * @param date      date du prix : celle du rapport, à défaut celle de l'import
     */
    public record LignePrix(String numeroRapport, String designation, String typePiece, String marque, String modele,
                            String typeMine, BigDecimal prixUnitaire, LocalDate date) {
    }

    /**
     * Filtres de l'analyse des prix ; {@link RechercheTexte#VIDE} demande les lignes où le champ n'est pas renseigné.
     *
     * @param type  ORIGINE, ADAPTABLE, REEMPLOI, ou VIDE (type non renseigné)
     * @param zones zones de choc de la ligne (codes 1 à 8) : une ligne répond si sa zone est l'une d'elles
     * @param annee année de mise en circulation (MEC)
     */
    public record FiltresPrix(String designation, String type, List<Integer> zones, String marque, String modele,
                              String typeMine, String annee, String chassis) {
    }

    /** @param type ORIGINE, ADAPTABLE, REEMPLOI ou VIDE ; {@code moyenne} : prix unitaire moyen de ce type */
    public record RepartitionType(String type, long nombre, BigDecimal moyenne) {
    }

    /**
     * @param nombre   lignes de pièces filtrées
     * @param dossiers dossiers distincts auxquels elles appartiennent
     * @param lignes   page {@code page} (à partir de 0) de {@code taille} lignes sur les {@code nombre} filtrées
     */
    public record AnalysePrix(long nombre, long dossiers, BigDecimal minimum, BigDecimal moyenne, BigDecimal maximum,
                              List<RepartitionType> parType, List<LignePrix> lignes, int page, int taille) {
    }
}
