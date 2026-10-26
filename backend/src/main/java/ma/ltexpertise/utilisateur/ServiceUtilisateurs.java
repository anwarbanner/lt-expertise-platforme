package ma.ltexpertise.utilisateur;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Gestion des comptes par l'administrateur (F11) : création, rôle, activation, réinitialisation du mot de passe.
 * Un compte n'est jamais supprimé (journal d'audit lisible) ; chaque action est tracée (P7).
 */
@Service
public class ServiceUtilisateurs {

    private final UtilisateurRepository utilisateurs;
    private final JournalAuditRepository journal;
    private final PasswordEncoder encodeur;
    private final RevocationSessions revocation;
    private final Clock horloge;

    public ServiceUtilisateurs(UtilisateurRepository utilisateurs, JournalAuditRepository journal,
                               PasswordEncoder encodeur, RevocationSessions revocation, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.journal = journal;
        this.encodeur = encodeur;
        this.revocation = revocation;
        this.horloge = horloge;
    }

    @Transactional(readOnly = true)
    public List<CompteUtilisateur> lister() {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        return utilisateurs.findAll().stream()
                .sorted((a, b) -> a.getIdentifiant().compareToIgnoreCase(b.getIdentifiant()))
                .map(u -> CompteUtilisateur.de(u, maintenant)).toList();
    }

    @Transactional
    public CompteUtilisateur creer(CreationCompte c, Utilisateur administrateur) {
        String identifiant = c.identifiant().strip().toLowerCase();
        if (utilisateurs.existsByIdentifiant(identifiant)) {
            throw new RegleCompteException("L'identifiant « " + identifiant + " » existe déjà.");
        }
        Utilisateur u = new Utilisateur(identifiant, c.nom().strip(), c.prenom().strip(),
                encodeur.encode(c.motDePasse()), c.role());
        u.setEmail(vide(c.email()));
        utilisateurs.save(u);
        tracer(administrateur, u, Map.of("operation", "création", "role", c.role().name()));
        return CompteUtilisateur.de(u, OffsetDateTime.now(horloge));
    }

    @Transactional
    public CompteUtilisateur modifier(Long id, ModificationCompte m, Utilisateur administrateur) {
        Utilisateur u = utilisateurs.findById(id).orElseThrow(() -> new RegleCompteException("Compte introuvable."));
        boolean soiMeme = u.getId().equals(administrateur.getId());
        if (soiMeme && (m.role() != Role.ADMINISTRATEUR || !m.actif())) {
            throw new RegleCompteException("Vous ne pouvez ni retirer votre rôle d'administrateur ni désactiver votre compte.");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("operation", "modification");
        if (u.getRole() != m.role()) {
            details.put("role", u.getRole() + " → " + m.role());
            u.setRole(m.role());
        }
        if (u.isActif() != m.actif()) {
            details.put("actif", u.isActif() + " → " + m.actif());
            u.setActif(m.actif());
            if (!m.actif()) {
                revocation.revoquerTout(u, OffsetDateTime.now(horloge));
            }
        }
        if (!Objects.equals(u.getNom(), m.nom().strip()) || !Objects.equals(u.getPrenom(), m.prenom().strip())) {
            details.put("nom", u.getPrenom() + " " + u.getNom() + " → " + m.prenom().strip() + " " + m.nom().strip());
            u.setNom(m.nom().strip());
            u.setPrenom(m.prenom().strip());
        }
        if (!Objects.equals(u.getEmail(), vide(m.email()))) {
            details.put("email", "modifié");
            u.setEmail(vide(m.email()));
        }
        if (details.size() > 1) {
            tracer(administrateur, u, details);
        }
        return CompteUtilisateur.de(u, OffsetDateTime.now(horloge));
    }

    /** Nouveau mot de passe choisi par l'administrateur (pas d'e-mail, P11) ; déverrouille et ferme les sessions. */
    @Transactional
    public void reinitialiserMotDePasse(Long id, String motDePasse, Utilisateur administrateur) {
        Utilisateur u = utilisateurs.findById(id).orElseThrow(() -> new RegleCompteException("Compte introuvable."));
        u.setMotDePasseHash(encodeur.encode(motDePasse));
        u.setTentativesEchouees((short) 0);
        u.setVerrouilleJusquA(null);
        revocation.revoquerTout(u, OffsetDateTime.now(horloge));
        tracer(administrateur, u, Map.of("operation", "réinitialisation du mot de passe"));
    }

    private void tracer(Utilisateur administrateur, Utilisateur cible, Map<String, Object> details) {
        Map<String, Object> complet = new LinkedHashMap<>(details);
        complet.put("compte", cible.getIdentifiant());
        journal.save(new JournalAudit(ActionAudit.GESTION_UTILISATEUR, administrateur, OffsetDateTime.now(horloge))
                .surEntite(Utilisateur.class.getSimpleName(), cible.getId(), null)
                .avecDetails(complet));
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.strip();
    }

    /** Révocation des sessions ouvertes d'un compte (implémentée par le module de sécurité). */
    public interface RevocationSessions {
        void revoquerTout(Utilisateur utilisateur, OffsetDateTime maintenant);
    }

    public record CompteUtilisateur(Long id, String identifiant, String nom, String prenom, String email, Role role,
                                    boolean actif, boolean verrouille, OffsetDateTime derniereConnexion,
                                    OffsetDateTime creeLe) {

        static CompteUtilisateur de(Utilisateur u, OffsetDateTime maintenant) {
            boolean verrouille = u.getVerrouilleJusquA() != null && u.getVerrouilleJusquA().isAfter(maintenant);
            return new CompteUtilisateur(u.getId(), u.getIdentifiant(), u.getNom(), u.getPrenom(), u.getEmail(),
                    u.getRole(), u.isActif(), verrouille, u.getDerniereConnexion(), u.getCreeLe());
        }
    }

    public record CreationCompte(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
                    message = "3 à 50 caractères : lettres, chiffres, point, tiret") String identifiant,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String nom,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String prenom,
            @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 150) String email,
            @jakarta.validation.constraints.NotNull Role role,
            @jakarta.validation.constraints.Size(min = 10, max = 200,
                    message = "10 caractères au moins") @jakarta.validation.constraints.NotNull String motDePasse) {
    }

    public record ModificationCompte(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String nom,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 80) String prenom,
            @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 150) String email,
            @jakarta.validation.constraints.NotNull Role role, boolean actif) {
    }

    public record NouveauMotDePasse(
            @jakarta.validation.constraints.Size(min = 10, max = 200, message = "10 caractères au moins")
            @jakarta.validation.constraints.NotNull String motDePasse) {
    }

    /** Opération refusée sur un compte (identifiant déjà pris, auto-rétrogradation…). */
    public static class RegleCompteException extends RuntimeException {
        public RegleCompteException(String message) {
            super(message);
        }
    }
}
