package ma.ltexpertise.recherche;

import ma.ltexpertise.commun.Tableau;
import ma.ltexpertise.recherche.ServiceRecherches.FiltresValeurs;
import ma.ltexpertise.recherche.ServiceRecherches.ResultatValeurs;
import ma.ltexpertise.recherche.ServiceRecherches.Suggestions;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Recherche des valeurs d'achat / vénales et suggestions des filtres du véhicule, ouvertes à tous les rôles. Résultats paginés : {@code page},
 * {@code taille}, {@code tri}, filtres de colonne {@code f_<cle>} (voir {@link Tableau}).
 */
@RestController
@RequestMapping("/api/recherche")
@PreAuthorize("isAuthenticated()")
public class RechercheController {

    private final ServiceRecherches service;

    public RechercheController(ServiceRecherches service) {
        this.service = service;
    }

    /**
     * @param marque marque saisie : limite les modèles et types mine proposés
     * @param modele modèle choisi : limite les types mine proposés
     */
    @GetMapping("/suggestions")
    public Suggestions suggestions(@RequestParam(required = false) String marque,
                                   @RequestParam(required = false) String modele) {
        return service.suggestions(marque, modele);
    }

    /**
     * @param mecDu  mise en circulation au plus tôt
     * @param ageMin âge minimal au sinistre, en mois
     */
    @GetMapping("/valeurs")
    public ResultatValeurs valeurs(@RequestParam(required = false) String marque, @RequestParam(required = false) String modele,
                                   @RequestParam(required = false) String typeMine,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mecDu,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mecAu,
                                   @RequestParam(required = false) Integer ageMin, @RequestParam(required = false) Integer ageMax,
                                   @RequestParam(required = false) BigDecimal achatMin, @RequestParam(required = false) BigDecimal achatMax,
                                   @RequestParam(required = false) BigDecimal venaleMin, @RequestParam(required = false) BigDecimal venaleMax,
                                   @RequestParam Map<String, String> parametres) {
        return service.valeurs(new FiltresValeurs(marque, modele, typeMine, mecDu, mecAu, ageMin, ageMax, achatMin,
                achatMax, venaleMin, venaleMax), Tableau.depuis(parametres));
    }
}
