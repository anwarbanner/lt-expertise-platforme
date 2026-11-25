package ma.ltexpertise.audit;

import ma.ltexpertise.commun.RechercheTexte;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.ltexpertise.generation.ClasseurExcel;
import ma.ltexpertise.generation.GenerationController;
import ma.ltexpertise.utilisateur.UtilisateurCourant;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Consultation du journal d'audit (P7) par l'administrateur : recherche, filtre par action, pagination, export. */
@RestController
@RequestMapping("/api/journal-audit")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class JournalController {

    /** Au-delà, l'export du journal est tronqué aux entrées les plus récentes. */
    static final int EXPORT_MAX = 200_000;

    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper json;
    private final JournalAuditRepository journal;
    private final UtilisateurCourant utilisateurCourant;
    private final Clock horloge;

    public JournalController(NamedParameterJdbcTemplate jdbc, ObjectMapper json, JournalAuditRepository journal,
                             UtilisateurCourant utilisateurCourant, Clock horloge) {
        this.jdbc = jdbc;
        this.json = json;
        this.journal = journal;
        this.utilisateurCourant = utilisateurCourant;
        this.horloge = horloge;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public PagedModel<EntreeJournal> rechercher(@RequestParam(required = false) String q,
                                                @RequestParam(required = false) ActionAudit action,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        int taille = Math.max(1, Math.min(size, 100));
        Filtre f = filtre(q, action);
        long total = jdbc.queryForObject("select count(*)" + DEPUIS + f.sql(), f.parametres(), Long.class);
        List<EntreeJournal> entrees = entrees(f, taille, (long) Math.max(page, 0) * taille);
        return new PagedModel<>(new PageImpl<>(entrees, PageRequest.of(Math.max(page, 0), taille), total));
    }

    /** Export Excel des entrées répondant aux filtres (les plus récentes d'abord), tracé lui-même au journal. */
    @GetMapping("/export")
    @Transactional
    public ResponseEntity<byte[]> exporter(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) ActionAudit action) {
        List<EntreeJournal> entrees = entrees(filtre(q, action), EXPORT_MAX, 0);
        byte[] xlsx;
        try (ClasseurExcel x = new ClasseurExcel()) {
            ClasseurExcel.Feuille f = x.feuille("Journal d'audit", "Date et heure", "Utilisateur", "Action",
                    "N° de rapport", "Champ", "Ancienne valeur", "Nouvelle valeur", "Détails", "Adresse IP");
            for (EntreeJournal e : entrees) {
                f.ligne(e.horodatage(), e.utilisateur(), e.action(), e.numeroRapport(), e.champ(), e.ancienneValeur(),
                        e.nouvelleValeur(), e.details() == null ? null : e.details().toString(), e.adresseIp());
            }
            f.ajuster();
            xlsx = x.octets();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("export", "journal d'audit");
        details.put("lignes", entrees.size());
        if (action != null) {
            details.put("action", action.name());
        }
        journal.save(new JournalAudit(ActionAudit.EXPORT_EXCEL, utilisateurCourant.obtenir(), OffsetDateTime.now(horloge))
                .avecDetails(details));
        return GenerationController.fichier(xlsx, "journal-audit-"
                + LocalDateTime.now(horloge).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx",
                GenerationController.XLSX);
    }

    private static final String DEPUIS = " from journal_audit j left join rapport_expertise r on r.id = j.rapport_id";

    private record Filtre(String sql, MapSqlParameterSource parametres) {
    }

    private static Filtre filtre(String q, ActionAudit action) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(" where true");
        if (q != null && !q.isBlank()) {
            sql.append(" and (j.identifiant_utilisateur ilike :q or j.champ ilike :q or j.adresse_ip ilike :q "
                    + "or cast(j.details as text) ilike :q or r.numero_rapport ilike :q)");
            p.addValue("q", RechercheTexte.motif(q));
        }
        if (action != null) {
            sql.append(" and j.action = :action");
            p.addValue("action", action.name());
        }
        return new Filtre(sql.toString(), p);
    }

    private List<EntreeJournal> entrees(Filtre f, int limite, long decalage) {
        MapSqlParameterSource p = new MapSqlParameterSource(f.parametres().getValues())
                .addValue("limite", limite).addValue("decalage", decalage);
        return jdbc.query("select j.id, j.horodatage, j.identifiant_utilisateur, j.action, "
                + "j.type_entite, j.entite_id, r.numero_rapport, j.champ, j.ancienne_valeur, j.nouvelle_valeur, "
                + "cast(j.details as text) as details, j.adresse_ip" + DEPUIS + f.sql()
                + " order by j.horodatage desc, j.id desc limit :limite offset :decalage", p,
                (rs, i) -> new EntreeJournal(rs.getLong("id"), rs.getObject("horodatage", OffsetDateTime.class),
                        rs.getString("identifiant_utilisateur"), rs.getString("action"), rs.getString("type_entite"),
                        rs.getObject("entite_id", Long.class), rs.getString("numero_rapport"), rs.getString("champ"),
                        rs.getString("ancienne_valeur"), rs.getString("nouvelle_valeur"), lire(rs.getString("details")),
                        rs.getString("adresse_ip")));
    }

    private Map<String, Object> lire(String details) {
        if (details == null) {
            return null;
        }
        try {
            return json.readValue(details, new TypeReference<>() { });
        } catch (Exception e) {
            return Map.of("brut", details);
        }
    }

    public record EntreeJournal(long id, OffsetDateTime horodatage, String utilisateur, String action,
                                String typeEntite, Long entiteId, String numeroRapport, String champ,
                                String ancienneValeur, String nouvelleValeur, Map<String, Object> details,
                                String adresseIp) {
    }
}
