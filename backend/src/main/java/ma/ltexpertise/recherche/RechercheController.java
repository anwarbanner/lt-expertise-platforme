package ma.ltexpertise.recherche;

import ma.ltexpertise.commun.Tableau;
import ma.ltexpertise.recherche.ServiceRecherches.FiltresPieces;
import ma.ltexpertise.recherche.ServiceRecherches.FiltresValeurs;
import ma.ltexpertise.recherche.ServiceRecherches.ResultatPieces;
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
 * Recherche des pièces et des valeurs d'achat / vénales, ouvertes à tous les rôles. Résultats paginés : {@code page},
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

    @GetMapping("/suggestions")
    public Suggestions suggestions() {
        return service.suggestions();
    }

    /**
     * @param zone   zone de choc de la ligne, de 1 à 8
     * @param annee  année de mise en circulation (MEC)
     * @param chassis tout ou partie du numéro de châssis (n° de série)
     */
    @GetMapping("/pieces")
    public ResultatPieces pieces(@RequestParam(required = false) String piece, @RequestParam(required = false) String type,
                                 @RequestParam(required = false) Integer zone, @RequestParam(required = false) String marque,
                                 @RequestParam(required = false) String modele, @RequestParam(required = false) String typeMine,
                                 @RequestParam(required = false) Integer annee, @RequestParam(required = false) String chassis,
                                 @RequestParam Map<String, String> parametres) {
        return service.pieces(new FiltresPieces(piece, type, zone, marque, modele, typeMine, annee, chassis),
                Tableau.depuis(parametres));
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
