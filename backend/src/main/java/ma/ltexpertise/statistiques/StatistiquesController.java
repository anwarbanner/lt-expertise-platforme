package ma.ltexpertise.statistiques;

import ma.ltexpertise.statistiques.ServiceStatistiques.AnalysePrix;
import ma.ltexpertise.statistiques.ServiceStatistiques.TableauDeBord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

/** Statistiques du tableau de bord (F10) et analyse des prix des pièces. */
@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesController {

    private final ServiceStatistiques service;

    public StatistiquesController(ServiceStatistiques service) {
        this.service = service;
    }

    /** @param periode mois « AAAA-MM » ; absent = tous les dossiers */
    @GetMapping("/tableau-de-bord")
    @PreAuthorize("isAuthenticated()")
    public TableauDeBord tableauDeBord(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periode) {
        return service.tableauDeBord(periode);
    }

    @GetMapping("/prix")
    @PreAuthorize("isAuthenticated()")
    public AnalysePrix prix(@RequestParam(required = false) String q, @RequestParam(required = false) String type,
                            @RequestParam(required = false) String compagnie, @RequestParam(required = false) String marque) {
        return service.prix(q, type, compagnie, marque);
    }
}
