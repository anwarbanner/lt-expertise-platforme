package ma.ltexpertise.securite;

import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.Empreintes;
import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Connexion par identifiant et mot de passe haché (BCrypt), verrouillage temporaire après plusieurs échecs,
 * jetons de rafraîchissement à usage unique et trace de chaque connexion ou échec (P7, §8).
 */
@Service
public class ServiceAuthentification {

    private final UtilisateurRepository utilisateurs;
    private final JetonRafraichissementRepository jetons;
    private final JournalAuditRepository journal;
    private final PasswordEncoder encodeur;
    private final ServiceJetons serviceJetons;
    private final ProprietesSecurite proprietes;
    private final Clock horloge;
    private final SecureRandom aleatoire = new SecureRandom();
    /** Haché vérifié pour un identifiant inconnu : même temps de réponse que pour un compte existant. */
    private final String hacheFactice;

    public ServiceAuthentification(UtilisateurRepository utilisateurs, JetonRafraichissementRepository jetons,
                                   JournalAuditRepository journal, PasswordEncoder encodeur, ServiceJetons serviceJetons,
                                   ProprietesSecurite proprietes, Clock horloge) {
        this.utilisateurs = utilisateurs;
        this.jetons = jetons;
        this.journal = journal;
        this.encodeur = encodeur;
        this.serviceJetons = serviceJetons;
        this.proprietes = proprietes;
        this.horloge = horloge;
        this.hacheFactice = encodeur.encode("identifiant-inconnu");
    }

    /**
     * @throws CompteVerrouilleException      si le compte est temporairement verrouillé
     * @throws IdentifiantsInvalidesException identifiant inconnu, mot de passe faux ou compte désactivé (même
     *                                        message dans tous les cas)
     */
    @Transactional(noRollbackFor = {IdentifiantsInvalidesException.class, CompteVerrouilleException.class})
    public Session connecter(String identifiant, String motDePasse, String adresseIp) {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        Optional<Utilisateur> trouve = utilisateurs.findByIdentifiant(identifiant);
        if (trouve.isEmpty()) {
            encodeur.matches(motDePasse, hacheFactice);
            tracer(ActionAudit.ECHEC_CONNEXION, null, maintenant, adresseIp, Map.of("identifiantSaisi", identifiant,
                    "motif", "identifiant inconnu"));
            throw new IdentifiantsInvalidesException();
        }
        Utilisateur utilisateur = trouve.get();
        if (utilisateur.getVerrouilleJusquA() != null && maintenant.isBefore(utilisateur.getVerrouilleJusquA())) {
            tracer(ActionAudit.ECHEC_CONNEXION, utilisateur, maintenant, adresseIp, Map.of("motif", "compte verrouillé"));
            throw new CompteVerrouilleException(utilisateur.getVerrouilleJusquA());
        }
        if (!utilisateur.isActif() || !encodeur.matches(motDePasse, utilisateur.getMotDePasseHash())) {
            enregistrerEchec(utilisateur, maintenant, adresseIp);
            throw new IdentifiantsInvalidesException();
        }

        utilisateur.setTentativesEchouees((short) 0);
        utilisateur.setVerrouilleJusquA(null);
        utilisateur.setDerniereConnexion(maintenant);
        tracer(ActionAudit.CONNEXION, utilisateur, maintenant, adresseIp, Map.of());
        return ouvrirSession(utilisateur, maintenant, maintenant.plus(proprietes.dureeSessionMax()));
    }

    /**
     * Remplace le jeton de rafraîchissement par un nouveau (usage unique) et émet un jeton d'accès.
     *
     * @throws SessionExpireeException jeton inconnu, déjà utilisé, expiré par inactivité, ou compte désactivé
     */
    @Transactional(noRollbackFor = SessionExpireeException.class)
    public Session rafraichir(String jetonRafraichissement) {
        OffsetDateTime maintenant = OffsetDateTime.now(horloge);
        JetonRafraichissement jeton = trouver(jetonRafraichissement).orElseThrow(SessionExpireeException::new);
        if (!jeton.estValide(maintenant) || !jeton.getUtilisateur().isActif()) {
            jeton.revoquer(maintenant);
            throw new SessionExpireeException();
        }
        jeton.revoquer(maintenant);
        return ouvrirSession(jeton.getUtilisateur(), maintenant, jeton.getFinSessionLe());
    }

    @Transactional
    public void deconnecter(String jetonRafraichissement) {
        trouver(jetonRafraichissement).ifPresent(j -> j.revoquer(OffsetDateTime.now(horloge)));
    }

    @Transactional(readOnly = true)
    public Optional<UtilisateurConnecte> utilisateur(String identifiant) {
        return utilisateurs.findByIdentifiant(identifiant).filter(Utilisateur::isActif).map(UtilisateurConnecte::de);
    }

    private Session ouvrirSession(Utilisateur utilisateur, OffsetDateTime maintenant, OffsetDateTime finSession) {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String jeton = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        OffsetDateTime expiration = maintenant.plus(proprietes.dureeInactivite());
        if (expiration.isAfter(finSession)) {
            expiration = finSession;
        }
        jetons.save(new JetonRafraichissement(utilisateur, empreinte(jeton), maintenant, expiration, finSession));
        return new Session(serviceJetons.jetonAcces(utilisateur), serviceJetons.dureeJetonAccesSecondes(),
                UtilisateurConnecte.de(utilisateur), jeton, Duration.between(maintenant, expiration));
    }

    private void enregistrerEchec(Utilisateur utilisateur, OffsetDateTime maintenant, String adresseIp) {
        int tentatives = utilisateur.getTentativesEchouees() + 1;
        if (tentatives >= proprietes.tentativesMax()) {
            utilisateur.setVerrouilleJusquA(maintenant.plus(proprietes.dureeVerrouillage()));
            utilisateur.setTentativesEchouees((short) 0);
            tracer(ActionAudit.ECHEC_CONNEXION, utilisateur, maintenant, adresseIp,
                    Map.of("motif", "mot de passe incorrect", "verrouillage", "oui"));
        } else {
            utilisateur.setTentativesEchouees((short) tentatives);
            tracer(ActionAudit.ECHEC_CONNEXION, utilisateur, maintenant, adresseIp,
                    Map.of("motif", utilisateur.isActif() ? "mot de passe incorrect" : "compte désactivé"));
        }
    }

    private Optional<JetonRafraichissement> trouver(String jeton) {
        return jeton == null || jeton.isBlank() ? Optional.empty() : jetons.findByEmpreinte(empreinte(jeton));
    }

    private static String empreinte(String jeton) {
        return Empreintes.sha256(jeton.getBytes(StandardCharsets.US_ASCII));
    }

    private void tracer(ActionAudit action, Utilisateur utilisateur, OffsetDateTime maintenant, String adresseIp,
                        Map<String, Object> details) {
        journal.save(new JournalAudit(action, utilisateur, maintenant)
                .surEntite(Utilisateur.class.getSimpleName(), utilisateur == null ? null : utilisateur.getId(), null)
                .avecDetails(details.isEmpty() ? null : details)
                .depuisAdresse(adresseIp));
    }

    /** Résultat d'une connexion ou d'un rafraîchissement. */
    public record Session(String jetonAcces, long expireDans, UtilisateurConnecte utilisateur,
                          String jetonRafraichissement, Duration dureeRafraichissement) {
    }
}
