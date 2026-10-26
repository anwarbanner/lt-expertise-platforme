package ma.ltexpertise.utilisateur;

import jakarta.validation.Valid;
import ma.ltexpertise.utilisateur.ServiceUtilisateurs.CompteUtilisateur;
import ma.ltexpertise.utilisateur.ServiceUtilisateurs.CreationCompte;
import ma.ltexpertise.utilisateur.ServiceUtilisateurs.ModificationCompte;
import ma.ltexpertise.utilisateur.ServiceUtilisateurs.NouveauMotDePasse;
import ma.ltexpertise.utilisateur.ServiceUtilisateurs.RegleCompteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Comptes utilisateurs (F11) : réservé à l'administrateur. */
@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class UtilisateurController {

    private final ServiceUtilisateurs service;
    private final UtilisateurCourant utilisateurCourant;

    public UtilisateurController(ServiceUtilisateurs service, UtilisateurCourant utilisateurCourant) {
        this.service = service;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public List<CompteUtilisateur> lister() {
        return service.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompteUtilisateur creer(@Valid @RequestBody CreationCompte creation) {
        return service.creer(creation, utilisateurCourant.obtenir());
    }

    @PutMapping("/{id}")
    public CompteUtilisateur modifier(@PathVariable Long id, @Valid @RequestBody ModificationCompte modification) {
        return service.modifier(id, modification, utilisateurCourant.obtenir());
    }

    @PostMapping("/{id}/mot-de-passe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reinitialiser(@PathVariable Long id, @Valid @RequestBody NouveauMotDePasse demande) {
        service.reinitialiserMotDePasse(id, demande.motDePasse(), utilisateurCourant.obtenir());
    }

    @ExceptionHandler(RegleCompteException.class)
    ResponseEntity<Map<String, String>> refus(RegleCompteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
    }
}
