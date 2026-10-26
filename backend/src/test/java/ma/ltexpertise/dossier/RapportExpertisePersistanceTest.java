package ma.ltexpertise.dossier;

import jakarta.persistence.EntityManager;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.commun.ConfigurationJpa;
import ma.ltexpertise.commun.Montants;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.CleCommentaire;
import ma.ltexpertise.dossier.referentiel.Energie;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import ma.ltexpertise.importation.ImportFichier;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static ma.ltexpertise.dossier.JeuDeDonnees.MAINTENANT;
import static ma.ltexpertise.dossier.JeuDeDonnees.dh;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Enregistre un rapport complet sur un vrai PostgreSQL (schéma créé par Flyway V1, validé par Hibernate),
 * vide le contexte de persistance puis relit tout depuis la base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConfigurationTestcontainers.class, ConfigurationJpa.class})
class RapportExpertisePersistanceTest {

    @Autowired
    private RapportExpertiseRepository rapports;

    @Autowired
    private EntityManager em;

    @Test
    void enregistreEtRelitUnRapportCompletAvecSesLignes() {
        Utilisateur gestionnaire = JeuDeDonnees.utilisateur("gestionnaire.test", Role.GESTIONNAIRE);
        em.persist(gestionnaire);
        ImportFichier source = JeuDeDonnees.importFichier(JeuDeDonnees.hash('a'), gestionnaire);
        em.persist(source);

        RapportExpertise aEnregistrer = JeuDeDonnees.rapportComplet("268202-0001", source);
        aEnregistrer.valider(gestionnaire, MAINTENANT);
        Long id = rapports.saveAndFlush(aEnregistrer).getId();
        em.clear();

        RapportExpertise r = rapports.findByNumeroRapport("268202-0001").orElseThrow();

        // En-tête, cycle de vie et audit technique
        assertThat(r.getId()).isEqualTo(id);
        assertThat(r.getNatureRapport()).isEqualTo(NatureRapport.DEFINITIF);
        assertThat(r.getTypeProcedure()).isEqualTo(TypeProcedure.PROCEDURE_CLASSIQUE);
        assertThat(r.getStatut()).isEqualTo(StatutDossier.VALIDE);
        assertThat(r.getValidePar().getIdentifiant()).isEqualTo("gestionnaire.test");
        assertThat(r.getValideLe()).isAtSameInstantAs(MAINTENANT);
        assertThat(r.getCreeLe()).isNotNull();
        assertThat(r.getCreePar()).isEqualTo(ConfigurationJpa.AUTEUR_SYSTEME);
        assertThat(r.getDateMission()).isEqualTo(LocalDate.of(2026, 10, 6));
        assertThat(r.getDateRapport()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(r.getZonesChoc()).containsExactly(ZoneChoc.AVANT_LATERAL_GAUCHE);
        assertThat(r.getAccordReparateur()).isTrue();
        assertThat(r.getTvaRecuperable()).isFalse();
        assertThat(r.getImportFichier().getHashSha256()).isEqualTo(JeuDeDonnees.hash('a'));
        assertThat(r.getCommentaire()).startsWith("- LA VALEUR A NEUF");

        // Montants : BigDecimal relus à l'échelle 2, sans perte
        assertMontants(r.getTotalMainOeuvre(), "1200.00", "240.00", "1440.00");
        assertMontants(r.getTotalFournitures(), "5600.00", "120.00", "5720.00");
        assertMontants(r.getTotalGeneral(), "6800.00", "360.00", "7160.00");
        assertThat(r.getMontantIndemnisation()).isEqualTo(dh("7160.00"));
        assertThat(r.getVetusteTtc()).isEqualTo(dh("0.00"));
        assertThat(r.getValeurVenale()).isNull();

        // Parties
        assertThat(r.getParties()).hasSize(2);
        Partie assure = r.getPartie(TypePartie.ASSURE).orElseThrow();
        assertThat(assure.getNomPrenom()).isEqualTo("ASSURE FICTIF");
        assertThat(assure.getImmatriculation()).isEqualTo("00000-A-00");
        assertThat(r.getPartie(TypePartie.ADVERSAIRE).orElseThrow().getNomPrenom()).isNull();

        // Véhicule
        Vehicule v = r.getVehicule();
        assertThat(v.getMarque()).isEqualTo("MARQUE");
        assertThat(v.getEnergie()).isEqualTo(Energie.DIESEL);
        assertThat(v.getKilometrage()).isEqualTo(281_277);
        assertThat(v.isKilometrageNonEstimable()).isFalse();
        assertThat(v.getPuissanceFiscale()).isEqualTo((short) 6);
        assertThat(v.getDateMiseCirculation()).isEqualTo(LocalDate.of(2022, 10, 7));
        assertThat(v.getUsurePneuArd()).isEqualTo(dh("20.00"));

        // 7 postes de main d'œuvre, dans l'ordre du PDF
        assertThat(r.getEvaluationsMainOeuvre())
                .extracting(EvaluationMainOeuvre::getPoste)
                .containsExactly(PosteMainOeuvre.values());
        EvaluationMainOeuvre tolerie = r.getEvaluationsMainOeuvre().getFirst();
        assertThat(tolerie.getNombreHeures()).isEqualTo(dh("10.00"));
        assertThat(tolerie.getTauxHoraire()).isEqualTo(dh("60.00"));
        assertMontants(tolerie.getMontants(), "600.00", "120.00", "720.00");
        assertThat(sommeTtcMainOeuvre(r.getEvaluationsMainOeuvre())).isEqualByComparingTo(r.getTotalMainOeuvre().getTtc());

        // 4 fournitures
        assertThat(r.getFournitures())
                .extracting(Fourniture::getCategorie)
                .containsExactly(CategorieFourniture.values());
        assertMontants(r.getFournitures().getFirst().getMontants(), "5000.00", "0.00", "5000.00");

        // Lignes de détail (page 2), ordre conservé
        List<LigneDetail> lignes = r.getLignesDetail();
        assertThat(lignes).extracting(LigneDetail::getDesignation)
                .containsExactly("PP ET ING", "OPTIQUE AVG", "PARE CHOC AV");
        assertThat(lignes).extracting(LigneDetail::getOrdre).containsExactly((short) 0, (short) 1, (short) 2);
        LigneDetail optique = lignes.get(1);
        assertThat(optique.getTypePiece()).isEqualTo(TypePiece.REEMPLOI);
        assertThat(optique.getFamille()).isEqualTo(FamilleTravaux.TOLERIE);
        assertThat(optique.getZoneChoc()).isEqualTo(ZoneChoc.AVANT_LATERAL_GAUCHE);
        assertThat(optique.getOperation()).isEqualTo(TypeOperation.ECHANGE);
        assertThat(optique.getQuantite()).isEqualTo(dh("1.00"));
        assertThat(optique.getPrixUnitaire()).isEqualTo(dh("5000.00"));
        assertThat(optique.getTotalFourniture()).isEqualTo(dh("5000.00"));
        assertThat(optique.getMontantMoTtc()).isEqualTo(dh("720.00"));
        assertThat(lignes.getFirst().getTypePiece()).isNull();
        assertThat(lignes.stream().map(LigneDetail::getMontantMoTtc).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(r.getTotalMainOeuvre().getTtc());

        // Valeurs du commentaire (F12) et contrôles (P4)
        assertThat(r.getValeursCommentaire())
                .extracting(ValeurCommentaire::getCle)
                .containsExactlyInAnyOrder(CleCommentaire.VALEUR_NEUF_CATALOGUE, CleCommentaire.CADRE_EXPERTISE);
        assertThat(r.getResultatsControle()).singleElement()
                .satisfies(c -> {
                    assertThat(c.isConforme()).isTrue();
                    assertThat(c.getValeurObtenue()).isEqualTo(dh("7160.00"));
                });
    }

    private static void assertMontants(Montants m, String ht, String tva, String ttc) {
        assertThat(m.getHt()).isEqualTo(dh(ht));
        assertThat(m.getTva()).isEqualTo(dh(tva));
        assertThat(m.getTtc()).isEqualTo(dh(ttc));
    }

    private static BigDecimal sommeTtcMainOeuvre(List<EvaluationMainOeuvre> evaluations) {
        return evaluations.stream().map(e -> e.getMontants().getTtc()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
