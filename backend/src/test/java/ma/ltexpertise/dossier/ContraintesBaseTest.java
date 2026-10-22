package ma.ltexpertise.dossier;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.ConfigurationJpa;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.importation.ImportFichier;
import ma.ltexpertise.importation.ImportFichierRepository;
import ma.ltexpertise.importation.StatutImport;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

import static ma.ltexpertise.dossier.JeuDeDonnees.MAINTENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les règles P2, P3 et P7 sont aussi garanties par PostgreSQL, même en cas de contournement de l'application. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConfigurationTestcontainers.class, ConfigurationJpa.class})
class ContraintesBaseTest {

    @Autowired
    private RapportExpertiseRepository rapports;

    @Autowired
    private ImportFichierRepository imports;

    @Autowired
    private JournalAuditRepository journal;

    @Autowired
    private EntityManager em;

    private Utilisateur gestionnaire;

    @BeforeEach
    void creerGestionnaire() {
        gestionnaire = JeuDeDonnees.utilisateur("gestionnaire.contraintes", Role.GESTIONNAIRE);
        em.persist(gestionnaire);
    }

    @Test
    void p2_unNumeroDeRapportNeSEnregistreQuUneFois() {
        rapports.saveAndFlush(new RapportExpertise("268202-0002", StatutDossier.IMPORTE));

        assertThat(rapports.existsByNumeroRapport("268202-0002")).isTrue();
        assertThatThrownBy(() -> rapports.saveAndFlush(new RapportExpertise("268202-0002", StatutDossier.IMPORTE)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_rapport_numero");
    }

    @Test
    void p2_unMemeFichierPdfNeSImporteQuUneFois() {
        imports.saveAndFlush(JeuDeDonnees.importFichier(JeuDeDonnees.hash('b'), gestionnaire));

        assertThat(imports.existsByHashSha256(JeuDeDonnees.hash('b'))).isTrue();
        ImportFichier doublon = new ImportFichier("copie.pdf", JeuDeDonnees.hash('b'), 10, "rapports-originaux",
                "originaux/autre-cle.pdf", StatutImport.ACCEPTE, gestionnaire, MAINTENANT);
        assertThatThrownBy(() -> imports.saveAndFlush(doublon))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_import_fichier_hash");
    }

    @Test
    void p3_laBaseRefuseUnStatutValideSansValidationTracee() {
        RapportExpertise r = rapports.saveAndFlush(JeuDeDonnees.rapportComplet("268202-0003", null));

        assertThatThrownBy(() -> em.createNativeQuery("update rapport_expertise set statut = 'VALIDE' where id = :id")
                .setParameter("id", r.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("ck_rapport_validation");
    }

    @Test
    void p7_leJournalDAuditEstEnAjoutSeul() {
        JournalAudit trace = journal.save(new JournalAudit(ActionAudit.MODIFICATION, gestionnaire, MAINTENANT)
                .surEntite("RapportExpertise", 1L, null)
                .avecChangement("montantIndemnisation", "7000.00", "7160.00")
                .avecDetails(Map.of("source", "test")));
        em.flush();
        em.clear();

        JournalAudit relue = journal.findById(trace.getId()).orElseThrow();
        assertThat(relue.getIdentifiantUtilisateur()).isEqualTo("gestionnaire.contraintes");
        assertThat(relue.getNouvelleValeur()).isEqualTo("7160.00");
        assertThat(relue.getDetails()).containsEntry("source", "test");

        assertThatThrownBy(() -> em.createNativeQuery("delete from journal_audit where id = :id")
                .setParameter("id", trace.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class)
                .hasMessageContaining("ajout seul");
    }
}
