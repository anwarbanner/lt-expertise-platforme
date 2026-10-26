package ma.ltexpertise.securite;

import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Premier démarrage : si aucun compte n'existe, crée un compte par rôle (admin, gestionnaire, consultant) avec le
 * mot de passe {@code lt.securite.mot-de-passe-initial}, à changer avant la mise en service.
 */
@Component
public class InitialisationComptes implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(InitialisationComptes.class);

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder encodeur;
    private final ProprietesSecurite proprietes;

    public InitialisationComptes(UtilisateurRepository utilisateurs, PasswordEncoder encodeur,
                                 ProprietesSecurite proprietes) {
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
        this.proprietes = proprietes;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (utilisateurs.count() > 0) {
            return;
        }
        String hache = encodeur.encode(proprietes.motDePasseInitial());
        utilisateurs.save(new Utilisateur("admin", "Administrateur", "LT Expertise", hache, Role.ADMINISTRATEUR));
        utilisateurs.save(new Utilisateur("gestionnaire", "Gestionnaire", "LT Expertise", hache, Role.GESTIONNAIRE));
        utilisateurs.save(new Utilisateur("consultant", "Consultation", "LT Expertise", hache, Role.CONSULTATION));
        LOG.warn("Comptes initiaux créés (admin, gestionnaire, consultant) avec lt.securite.mot-de-passe-initial : "
                + "changer ces mots de passe avant la mise en service.");
    }
}
