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
     * Filtres de l'analyse des prix ({@link FiltresPrix}, lus dans la requête) : {@code designation}, {@code type}
     * (ORIGINE, ADAPTABLE, REEMPLOI ou VIDE), {@code zones} (« 1,4 »), {@code marque}, {@code modele},
     * {@code typeMine}, {@code annee} (MEC) et {@code chassis} ; « __VIDE__ » demande un champ non renseigné. Tableau
     * paginé ({@code page}, {@code taille}, {@code tri}, filtres de colonne {@code f_<cle>}, voir {@link Tableau}).
     */
    @GetMapping("/prix")
    @PreAuthorize("isAuthenticated()")
    public AnalysePrix prix(FiltresPrix filtres, @RequestParam Map<String, String> parametres) {
        return service.prix(filtres, Tableau.depuis(parametres));
    }
}
