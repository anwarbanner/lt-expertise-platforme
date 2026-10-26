package ma.ltexpertise.securite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Connexion (F11). Le jeton d'accès est renvoyé dans le corps et gardé en mémoire par l'interface ; le jeton de
 * rafraîchissement voyage dans un cookie HttpOnly, invisible du JavaScript, limité à /api/auth.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String COOKIE = "lt_rafraichissement";
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private final ServiceAuthentification service;
    private final ProprietesSecurite proprietes;

    public AuthController(ServiceAuthentification service, ProprietesSecurite proprietes) {
        this.service = service;
        this.proprietes = proprietes;
    }

    @PostMapping("/connexion")
    public ResponseEntity<ReponseConnexion> connexion(@Valid @RequestBody DemandeConnexion demande,
                                                      HttpServletRequest requete) {
        return reponse(service.connecter(demande.identifiant().strip(), demande.motDePasse(), requete.getRemoteAddr()));
    }

    @PostMapping("/rafraichir")
    public ResponseEntity<ReponseConnexion> rafraichir(@CookieValue(name = COOKIE, required = false) String jeton) {
        return reponse(service.rafraichir(jeton));
    }

    @PostMapping("/deconnexion")
    public ResponseEntity<Void> deconnexion(@CookieValue(name = COOKIE, required = false) String jeton) {
        service.deconnecter(jeton);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/moi")
    public UtilisateurConnecte moi(Authentication authentification) {
        return service.utilisateur(authentification.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable ou désactivé."));
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    ResponseEntity<Map<String, String>> identifiantsInvalides(IdentifiantsInvalidesException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(CompteVerrouilleException.class)
    ResponseEntity<Map<String, String>> compteVerrouille(CompteVerrouilleException e) {
        String heure = e.getJusqua().atZoneSameInstant(ZoneId.systemDefault()).format(HEURE);
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(Map.of("message", e.getMessage() + " Réessayez après " + heure + ".", "verrouilleJusqua",
                        e.getJusqua().toString()));
    }

    @ExceptionHandler(SessionExpireeException.class)
    ResponseEntity<Map<String, String>> sessionExpiree(SessionExpireeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
                .body(Map.of("message", e.getMessage()));
    }

    private ResponseEntity<ReponseConnexion> reponse(ServiceAuthentification.Session session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie(session.jetonRafraichissement(), session.dureeRafraichissement()).toString())
                .body(new ReponseConnexion(session.jetonAcces(), session.expireDans(), session.utilisateur()));
    }

    private ResponseCookie cookie(String valeur, Duration duree) {
        return ResponseCookie.from(COOKIE, valeur)
                .httpOnly(true)
                .secure(proprietes.cookieSecurise())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(duree)
                .build();
    }

    public record DemandeConnexion(@NotBlank @Size(max = 50) String identifiant,
                                   @NotBlank @Size(max = 200) String motDePasse) {
    }

    public record ReponseConnexion(String jetonAcces, long expireDans, UtilisateurConnecte utilisateur) {
    }
}
