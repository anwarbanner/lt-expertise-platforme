package ma.ltexpertise.statistiques;

import ma.ltexpertise.commun.Tableau;
import ma.ltexpertise.statistiques.ServiceStatistiques.AnalysePrix;
import ma.ltexpertise.statistiques.ServiceStatistiques.FiltresPrix;
import ma.ltexpertise.statistiques.ServiceStatistiques.TableauDeBord;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Map;

/** Statistiques du tableau de bord (F10) et analyse des prix des pièces. */
@RestController
@RequestMapping("/api/statistiques")
public class StatistiquesController {

    private final ServiceStatistiques service;

    public StatistiquesController(ServiceStatistiques service) {
        this.service = service;
    }

    /**
     * Seul l'administrateur voit les montants ; le gestionnaire et la consultation reçoivent le même tableau sans
     * aucun montant (P9).
     *
     * @param periode mois « AAAA-MM » ; absent = tous les dossiers
     */
    @GetMapping("/tableau-de-bord")
    @PreAuthorize("isAuthenticated()")
    public TableauDeBord tableauDeBord(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth periode,
                                       Authentication authentification) {
        TableauDeBord tableau = service.tableauDeBord(periode);
        boolean administrateur = authentification.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRATEUR".equals(a.getAuthority()));
        return administrateur ? tableau : tableau.sansMontants();
    }

    /**
     * Filtres de l'analyse des prix : désignation de la pièce, type O/A/R, marque, modèle et type mine ; tableau
     * paginé ({@code page}, {@code taille}, {@code tri}, filtres de colonne {@code f_<cle>}, voir {@link Tableau}).
     */
    @GetMapping("/prix")
    @PreAuthorize("isAuthenticated()")
    public AnalysePrix prix(@RequestParam(required = false) String designation, @RequestParam(required = false) String type,
                            @RequestParam(required = false) String marque, @RequestParam(required = false) String modele,
                            @RequestParam(required = false) String typeMine, @RequestParam Map<String, String> parametres) {
        return service.prix(new FiltresPrix(designation, type, marque, modele, typeMine), Tableau.depuis(parametres));
    }
}
