package ma.ltexpertise.generation;

import ma.ltexpertise.dossier.CriteresRecherche;
import ma.ltexpertise.dossier.DossierIntrouvableException;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.generation.ServiceRapports.Generation;
import ma.ltexpertise.generation.ServiceRapports.RapportGenere;
import ma.ltexpertise.statistiques.ServiceStatistiques.FiltresPrix;
import ma.ltexpertise.utilisateur.UtilisateurCourant;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Génération et exports (F9) : rapport PDF d'un dossier validé, historique des rapports générés, exports Excel
 * d'un dossier, de la liste filtrée et de l'analyse des prix. Ouverts à tous les rôles (la consultation exporte).
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("isAuthenticated()")
public class GenerationController {

    public static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter SUFFIXE = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final ServiceRapports rapports;
    private final ServiceExports exports;
    private final UtilisateurCourant utilisateurCourant;

    public GenerationController(ServiceRapports rapports, ServiceExports exports, UtilisateurCourant utilisateurCourant) {
        this.rapports = rapports;
        this.exports = exports;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping("/rapports/{numero}/pdf")
    public ResponseEntity<byte[]> rapport(@PathVariable String numero) {
        RapportGenere r = rapports.generer(numero, utilisateurCourant.obtenir());
        return fichier(r.contenu(), r.nomFichier(), MediaType.APPLICATION_PDF);
    }

    @GetMapping("/rapports/historique")
    public List<Generation> historique(@RequestParam(defaultValue = "20") int limite) {
        return rapports.historique(limite);
    }

    @GetMapping("/exports/dossiers/{numero}")
    public ResponseEntity<byte[]> dossier(@PathVariable String numero) {
        return fichier(exports.dossier(numero, utilisateurCourant.obtenir()), "dossier-" + numero + ".xlsx", XLSX);
    }

    /** Toute la liste répondant aux filtres de l'écran « Dossiers » (mêmes paramètres que GET /api/dossiers). */
    @GetMapping("/exports/dossiers")
    public ResponseEntity<byte[]> liste(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StatutDossier statut,
            @RequestParam(required = false) String compagnie,
            @RequestParam(required = false) Integer zone,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) BigDecimal montantMin,
            @RequestParam(required = false) BigDecimal montantMax,
            @RequestParam(required = false) Boolean anomalies) {
        byte[] xlsx = exports.liste(new CriteresRecherche(q, statut, compagnie, zone, du, au, montantMin, montantMax,
                anomalies), utilisateurCourant.obtenir());
        return fichier(xlsx, "dossiers-" + LocalDateTime.now().format(SUFFIXE) + ".xlsx", XLSX);
    }

    @GetMapping("/exports/prix")
    public ResponseEntity<byte[]> prix(@RequestParam(required = false) String designation,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String marque,
                                       @RequestParam(required = false) String modele,
                                       @RequestParam(required = false) String typeMine) {
        byte[] xlsx = exports.prix(new FiltresPrix(designation, type, marque, modele, typeMine),
                utilisateurCourant.obtenir());
        return fichier(xlsx, "analyse-prix-" + LocalDateTime.now().format(SUFFIXE) + ".xlsx", XLSX);
    }

    /** Réponse en pièce jointe, nom de fichier compris (Content-Disposition, exposé au navigateur). */
    public static ResponseEntity<byte[]> fichier(byte[] contenu, String nom, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nom, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(contenu);
    }

    @ExceptionHandler(GenerationRefuseeException.class)
    ResponseEntity<Map<String, String>> refus(GenerationRefuseeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DossierIntrouvableException.class)
    ResponseEntity<Map<String, String>> introuvable(DossierIntrouvableException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Dossier introuvable."));
    }
}
