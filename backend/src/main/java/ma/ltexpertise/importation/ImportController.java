package ma.ltexpertise.importation;

import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurCourant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Dépôt des PDF (F1), historique des imports, fichiers refusés et consultation du PDF original (P1). */
@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private static final Logger LOG = LoggerFactory.getLogger(ImportController.class);

    private final ServiceImport service;
    private final UtilisateurCourant utilisateurCourant;

    public ImportController(ServiceImport service, UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Dépôt d'un ou plusieurs PDF ; au-delà d'un fichier, l'ensemble forme un lot. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('GESTIONNAIRE')")
    public RecapitulatifImport importer(@RequestParam("fichiers") List<MultipartFile> fichiers) {
        Utilisateur auteur = utilisateurCourant.obtenir();
        UUID lot = fichiers.size() > 1 ? UUID.randomUUID() : null;
        List<ResultatImport> resultats = new ArrayList<>();
        for (MultipartFile fichier : fichiers) {
            resultats.add(importerUn(fichier, auteur, lot));
        }
        return RecapitulatifImport.de(lot, resultats);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public PagedModel<ImportFichierDto> historique(
            @RequestParam(defaultValue = "false") boolean refuses,
            @PageableDefault(size = 20, sort = "importeLe", direction = Sort.Direction.DESC) Pageable pagination) {
        return new PagedModel<>(service.historique(refuses, pagination));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
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

    private ResultatImport importerUn(MultipartFile fichier, Utilisateur auteur, UUID lot) {
        String nom = ServiceImport.nettoyerNom(fichier.getOriginalFilename());
        try {
            return service.importer(fichier.getOriginalFilename(), fichier.getBytes(), auteur, lot);
        } catch (DataIntegrityViolationException e) {
            // Même fichier ou même n° déposé simultanément par un autre utilisateur.
            return ResultatImport.sansEnregistrement(nom, IssueImport.DOUBLON_FICHIER, null,
                    "Fichier importé au même moment par un autre utilisateur.");
        } catch (IOException | RuntimeException e) {
            LOG.error("Import de « {} » en erreur", nom, e);
            return ResultatImport.sansEnregistrement(nom, IssueImport.ERREUR, null,
                    "Erreur technique, réessayer ou contacter l'administrateur.");
        }
    }
}
