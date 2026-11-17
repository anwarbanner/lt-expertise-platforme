package ma.ltexpertise.importation;

import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurCourant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Dépôt des PDF (F1) et récapitulatif des lots, historique des imports, fichiers refusés et consultation du PDF
 * original (P1).
 */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

    private final ServiceImport service;
    private final ServiceLotImport lots;
    private final TraitementLotImport traitement;
    private final UtilisateurCourant utilisateurCourant;

    public ImportController(ServiceImport service, ServiceLotImport lots, TraitementLotImport traitement,
                            UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.lots = lots;
        this.traitement = traitement;
        this.utilisateurCourant = utilisateurCourant;
    }

    /**
     * Dépôt d'un ou plusieurs PDF : contrôle du type et de la taille, doublons (P2), stockage de l'original (P1),
     * extraction, contrôles (P4, P5) et enregistrement avec le statut calculé.
     *
     * @return un seul fichier : 200 et le récapitulatif complet ; plusieurs : 202, traitement en arrière-plan,
     *         récapitulatif à suivre sur l'adresse de l'en-tête Location
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('GESTIONNAIRE', 'ADMINISTRATEUR')")
    public ResponseEntity<RecapitulatifLot> importer(@RequestParam("fichiers") List<MultipartFile> fichiers) {
        if (fichiers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun fichier déposé.");
        }
        Utilisateur auteur = utilisateurCourant.obtenir();
        List<FichierDepose> depots = fichiers.stream().map(ImportController::lire).toList();
        UUID lot = lots.ouvrir(depots.size(), auteur);
        URI adresse = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{lot}").buildAndExpand(lot).toUri();

        if (depots.size() == 1) {
            traitement.traiter(lot, depots, auteur);
            return ResponseEntity.created(adresse).body(recapitulatif(lot));
        }
        traitement.lancer(lot, depots, auteur);
        return ResponseEntity.accepted().location(adresse).body(recapitulatif(lot));
    }

    /** Récapitulatif d'un lot (réussis, à compléter, en erreur…), consultable pendant et après le traitement. */
    @GetMapping("/{lot:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    @PreAuthorize("isAuthenticated()")
    public RecapitulatifLot lot(@PathVariable UUID lot) {
        return recapitulatif(lot);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PagedModel<ImportFichierDto> historique(
            @RequestParam(defaultValue = "false") boolean refuses,
            @PageableDefault(size = 20, sort = "importeLe", direction = Sort.Direction.DESC) Pageable pagination) {
        return new PagedModel<>(service.historique(refuses, pagination));
    }

    @GetMapping(value = "/{id:\\d+}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> pdfOriginal(@PathVariable Long id) {
        DocumentOriginal document;
        try {
            document = service.lireOriginal(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import introuvable"));
        } catch (EmpreinteInvalideException e) {
            LOG.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le PDF original ne correspond plus à son empreinte : contacter l'administrateur.");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.nomFichier(), StandardCharsets.UTF_8).build().toString())
                .body(document.contenu());
    }

    private RecapitulatifLot recapitulatif(UUID lot) {
        return lots.recapitulatif(lot)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lot introuvable"));
    }

    private static FichierDepose lire(MultipartFile fichier) {
        try {
            return new FichierDepose(fichier.getOriginalFilename(), fichier.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier déposé « " + fichier.getOriginalFilename() + " »", e);
        }
    }
}
