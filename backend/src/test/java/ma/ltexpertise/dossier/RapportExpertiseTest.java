package ma.ltexpertise.dossier;

import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypePartie;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static ma.ltexpertise.dossier.JeuDeDonnees.MAINTENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Règles du cycle de vie portées par l'entité (P3, P6), sans base de données. */
class RapportExpertiseTest {

    private final Utilisateur gestionnaire = JeuDeDonnees.utilisateur("g", Role.GESTIONNAIRE);

    @Test
    void unGestionnaireValideLeDossier() {
        RapportExpertise r = new RapportExpertise("268202-0010", StatutDossier.A_VERIFIER);

        r.valider(gestionnaire, MAINTENANT);

        assertThat(r.getStatut()).isEqualTo(StatutDossier.VALIDE);
        assertThat(r.getValidePar()).isSameAs(gestionnaire);
        assertThat(r.getValideLe()).isEqualTo(MAINTENANT);
    }

    @Test
    void unConsultantNePeutPasValider() {
        RapportExpertise r = new RapportExpertise("268202-0011", StatutDossier.A_VERIFIER);

        assertThatThrownBy(() -> r.valider(JeuDeDonnees.utilisateur("u", Role.CONSULTATION), MAINTENANT))
                .isInstanceOf(ValidationRefuseeException.class);
        assertThat(r.getStatut()).isEqualTo(StatutDossier.A_VERIFIER);
    }

    @Test
    void lAdministrateurPeutValider() {
        Utilisateur admin = JeuDeDonnees.utilisateur("admin", Role.ADMINISTRATEUR);
        RapportExpertise r = new RapportExpertise("268202-0013", StatutDossier.A_VERIFIER);

        r.valider(admin, MAINTENANT);

        assertThat(r.getStatut()).isEqualTo(StatutDossier.VALIDE);
        assertThat(r.getValidePar()).isSameAs(admin);
    }

    @Test
    void unGestionnaireDesactiveNePeutPasValider() {
        gestionnaire.setActif(false);
        RapportExpertise r = new RapportExpertise("268202-0012", StatutDossier.A_VERIFIER);

        assertThatThrownBy(() -> r.valider(gestionnaire, MAINTENANT)).isInstanceOf(ValidationRefuseeException.class);
    }

    @Test
    void unRapportNonDefinitifNePeutPasEtreValide() {
        RapportExpertise r = new RapportExpertise("5576wca025", StatutDossier.A_COMPLETER);
        r.setNatureRapport(NatureRapport.NON_DEFINITIF);

        assertThatThrownBy(() -> r.valider(gestionnaire, MAINTENANT))
                .isInstanceOf(ValidationRefuseeException.class)
                .hasMessageContaining("non définitif");
    }

    @ParameterizedTest
    @EnumSource(value = StatutDossier.class, names = {"EN_COURS", "ARCHIVE"})
    void unDossierEnCoursOuArchiveNePeutPasEtreValide(StatutDossier statut) {
        RapportExpertise r = new RapportExpertise("268202-0013", statut);

        assertThatThrownBy(() -> r.valider(gestionnaire, MAINTENANT)).isInstanceOf(ValidationRefuseeException.class);
    }

    @Test
    void leStatutValideNeSObtientPasParChangementDeStatut() {
        RapportExpertise r = new RapportExpertise("268202-0014", StatutDossier.A_VERIFIER);

        assertThatThrownBy(() -> r.changerStatut(StatutDossier.VALIDE)).isInstanceOf(ValidationRefuseeException.class);
        assertThatThrownBy(() -> new RapportExpertise("268202-0015", StatutDossier.VALIDE))
                .isInstanceOf(ValidationRefuseeException.class);
    }

    @Test
    void uneMemePartieNePeutPasEtreAjouteeDeuxFois() {
        RapportExpertise r = new RapportExpertise("268202-0016", StatutDossier.IMPORTE);
        r.ajouterPartie(new Partie(TypePartie.ASSURE));

        assertThatThrownBy(() -> r.ajouterPartie(new Partie(TypePartie.ASSURE)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lesLignesDeDetailSontNumeroteesDansLOrdreDAjout() {
        RapportExpertise r = new RapportExpertise("268202-0017", StatutDossier.IMPORTE);
        r.ajouterLigneDetail(new LigneDetail(JeuDeDonnees.dh("1"), "A"));
        r.ajouterLigneDetail(new LigneDetail(JeuDeDonnees.dh("1"), "B"));

        assertThat(r.getLignesDetail()).extracting(LigneDetail::getOrdre).containsExactly((short) 0, (short) 1);
        assertThat(r.getLignesDetail()).allSatisfy(l -> assertThat(l.getRapport()).isSameAs(r));
    }
}
