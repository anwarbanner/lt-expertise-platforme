package ma.ltexpertise.dossier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ma.ltexpertise.dossier.DossierDtos.DossierDetail;
import ma.ltexpertise.dossier.DossierDtos.DossierModification;
import ma.ltexpertise.dossier.DossierDtos.DossierResume;
import ma.ltexpertise.dossier.DossierDtos.EvenementHistorique;
import ma.ltexpertise.dossier.DossierDtos.ResultatValidationGroupee;
import ma.ltexpertise.dossier.DossierDtos.ValidationGroupee;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.utilisateur.UtilisateurCourant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dossiers : recherche (F7), fiche (F8), vérification et modification (F5), validation (P3), archivage et annulation
 * d'un import erroné (P6), historique (P7).
 */
@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    /** Colonnes triables ; un tri inconnu revient au tri par défaut plutôt que d'échouer. */
    private static final Set<String> TRIS = Set.of("numeroRapport", "dateSinistre", "dateRapport",
            "montantIndemnisation", "creeLe", "statut", "compagnie");

    private final ServiceDossier service;
    private final UtilisateurCourant utilisateurCourant;

    public DossierController(ServiceDossier service, UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PagedModel<DossierResume> rechercher(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutDossier statut,
            @RequestParam(required = false) String compagnie,
            @RequestParam(required = false) Integer zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) BigDecimal montantMin,
            @RequestParam(required = false) BigDecimal montantMax,
            @RequestParam(required = false) Boolean anomalies,
            @PageableDefault(size = 10, sort = "creeLe", direction = Sort.Direction.DESC) Pageable pagination) {
        Sort tri = Sort.by(pagination.getSort().stream().filter(o -> TRIS.contains(o.getProperty())).toList());
        Pageable sure = PageRequest.of(pagination.getPageNumber(), Math.min(pagination.getPageSize(), 100),
                tri.isSorted() ? tri.and(Sort.by("id")) : Sort.by(Sort.Direction.DESC, "creeLe", "id"));
        return new PagedModel<>(service.rechercher(
                new CriteresRecherche(q, statut, compagnie, zone, du, au, montantMin, montantMax, anomalies), sure));
    }

    /** Nombre de dossiers par statut (onglets rapides de la liste). */
    @GetMapping("/compteurs")
    @PreAuthorize("isAuthenticated()")
    public Map<StatutDossier, Long> compteurs() {
        return service.compteurs();
    }

    /** Compagnies présentes dans les dossiers (filtre de la liste). */
    @GetMapping("/compagnies")
    @PreAuthorize("isAuthenticated()")
    public List<String> compagnies() {
        return service.compagnies();
    }

    /** {@code id} : un import annulé précis, quand un autre dossier porte désormais le même n°. */
    @GetMapping("/{numero}")
    @PreAuthorize("isAuthenticated()")
    public DossierDetail detail(@PathVariable String numero, @RequestParam(required = false) Long id) {
        return service.detail(numero, id).orElseThrow(DossierIntrouvableException::new);
    }

    @PutMapping("/{numero}")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMINISTRATEUR')")
    public DossierDetail modifier(@PathVariable String numero, @Valid @RequestBody DossierModification modification) {
        return service.modifier(numero, modification, utilisateurCourant.obtenir());
    }

    @PostMapping("/{numero}/validation")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMINISTRATEUR')")
    public DossierDetail valider(@PathVariable String numero) {
        return service.valider(numero, utilisateurCourant.obtenir());
    }

    /** Archivage d'un dossier validé (P6). */
    @PostMapping("/{numero}/archivage")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMINISTRATEUR')")
    public DossierDetail archiver(@PathVariable String numero) {
        return service.archiver(numero, utilisateurCourant.obtenir());
    }

    /** Annulation d'un import erroné (P6), réservée à l'administrateur, motif obligatoire. */
    @PostMapping("/{numero}/annulation")
    @PreAuthorize("hasRole('ADMINISTRATEUR')")
    public DossierDetail annuler(@PathVariable String numero, @Valid @RequestBody Annulation annulation) {
        return service.annuler(numero, annulation.motif(), utilisateurCourant.obtenir());
    }

    public record Annulation(@NotBlank @Size(max = 500) String motif) {
    }

    /** Validation en lot (P3) : dossiers cochés, ou tous les prêts à valider répondant aux filtres. */
    @PostMapping("/validation-groupee")
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMINISTRATEUR')")
    public ResultatValidationGroupee validerEnLot(@RequestBody ValidationGroupee demande) {
        return service.validerEnLot(demande, utilisateurCourant.obtenir());
    }

    @GetMapping("/{numero}/historique")
    @PreAuthorize("isAuthenticated()")
    public List<EvenementHistorique> historique(@PathVariable String numero, @RequestParam(required = false) Long id) {
        return service.historique(numero, id);
    }

    @ExceptionHandler(DossierIntrouvableException.class)
    ResponseEntity<Map<String, String>> introuvable(DossierIntrouvableException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ValidationRefuseeException.class)
    ResponseEntity<Map<String, String>> validationRefusee(ValidationRefuseeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Map<String, String>> modifieEntreTemps(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Le dossier a été modifié entre-temps : rechargez-le avant d'enregistrer."));
    }
}
