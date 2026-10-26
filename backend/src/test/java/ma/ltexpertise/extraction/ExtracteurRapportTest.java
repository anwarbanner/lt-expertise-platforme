package ma.ltexpertise.extraction;

import ma.ltexpertise.controle.ControleCoherenceService;
import ma.ltexpertise.dossier.referentiel.CategorieFourniture;
import ma.ltexpertise.dossier.referentiel.FamilleTravaux;
import ma.ltexpertise.dossier.referentiel.NatureContrat;
import ma.ltexpertise.dossier.referentiel.NatureEvenement;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.TypeOperation;
import ma.ltexpertise.dossier.referentiel.TypePiece;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.dossier.referentiel.ZoneChoc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rapports de référence du prototype (n° 268202-0231 et 268202-0284), déjà anonymisés (champs « XXXX »).
 * Comme tous les PDF de samples/, ils ne sont pas commités : le test est ignoré s'ils sont absents.
 */
@EnabledIf("rapportsPresents")
class ExtracteurRapportTest {

    private static final Path RAPPORT4 = Path.of("..", "samples", "rapport4.pdf");
    private static final Path RAPPORT5 = Path.of("..", "samples", "rapport5.pdf");

    private final ExtracteurRapport extracteur = new ExtracteurRapport();

    static boolean rapportsPresents() {
        return Files.exists(RAPPORT4) && Files.exists(RAPPORT5);
    }

    @Test
    void rapport4_enTeteEtMontants() throws IOException {
        RapportExtrait r = extraire(RAPPORT4);

        assertThat(r.getGabarit()).isEqualTo(ParseurTcpdfExpertiseV1.GABARIT);
        assertThat(r.getNumeroRapport()).isEqualTo("268202-0231");
        assertThat(r.getNatureRapport()).isEqualTo(NatureRapport.DEFINITIF);
        assertThat(r.getTypeProcedure()).isEqualTo(TypeProcedure.PROCEDURE_CLASSIQUE);
        assertThat(r.getNatureContrat()).isEqualTo(NatureContrat.RC);
        assertThat(r.getAccordReparateur()).isTrue();
        assertThat(r.getNatureEvenement()).isEqualTo(NatureEvenement.CIRCULATION_HORS_STATIONNEMENT);
        assertThat(r.getZonesChoc()).containsExactly(ZoneChoc.CENTRAL_LATERAL_DROIT);

        assertThat(r.getTotalGeneral().getHt()).isEqualTo(dh("1933.33"));
        assertThat(r.getTotalGeneral().getTva()).isEqualTo(dh("386.67"));
        assertThat(r.getTotalGeneral().getTtc()).isEqualTo(dh("2320.00"));
        assertThat(r.getTotalMainOeuvre().getTtc()).isEqualTo(dh("1584.00"));
        assertThat(r.getTotalFournitures().getTtc()).isEqualTo(dh("736.00"));
        assertThat(r.getFournitures().get(CategorieFourniture.INGREDIENT_PEINTURE).getHt()).isEqualTo(dh("613.33"));

        LigneMainOeuvreExtraite tolerie = r.getMainOeuvre().get(PosteMainOeuvre.MO_1_TOLERIE);
        assertThat(tolerie.nombreHeures()).isEqualTo(dh("12.00"));
        assertThat(tolerie.tauxHoraire()).isEqualTo(dh("60.00"));
        assertThat(tolerie.montants().getTtc()).isEqualTo(dh("864.00"));
        assertThat(r.getMainOeuvre()).hasSize(7);

        assertThat(r.getVetusteTtc()).isEqualTo(dh("0.00"));
        assertThat(r.getTvaRecuperable()).isFalse();
        assertThat(r.getValeurVenale()).isNull();
        assertThat(r.getMontantIndemnisation()).isEqualTo(dh("2320.00"));
        assertThat(r.getMontantLettres()).isEqualTo("deux mille trois cent vingt Dirhams");
        assertThat(r.getLieuRapport()).isEqualTo("Casablanca");

        assertThat(r.getVehicule().isKilometrageNonEstimable()).isTrue();
        assertThat(r.getVehicule().getUsurePneuAvg()).isEqualByComparingTo("25");
        assertThat(r.getVehicule().getUsurePneuArd()).isEqualByComparingTo("25");
    }

    @Test
    void rapport4_quatreLignesEnPage2() throws IOException {
        RapportExtrait r = extraire(RAPPORT4);

        assertThat(r.getLignesDetail()).hasSize(4)
                .extracting(LigneDetailExtraite::getDesignation)
                .containsExactly("MOP", "PP ET ING", "PORTE AV D", "MOT");
        assertThat(r.getLignesDetail()).allSatisfy(l -> {
            assertThat(l.getZoneLibelle()).isEqualTo("Central/Lateral droit");
            assertThat(l.getZoneChoc()).isEqualTo(ZoneChoc.CENTRAL_LATERAL_DROIT);
            assertThat(l.getOperation()).isEqualTo(TypeOperation.REPARATION);
            assertThat(l.getQuantite()).isEqualByComparingTo("1");
        });
        assertThat(r.getLignesDetail()).extracting(LigneDetailExtraite::getFamille).containsExactly(
                FamilleTravaux.PEINTURE, FamilleTravaux.PEINTURE, FamilleTravaux.TOLERIE, FamilleTravaux.TOLERIE);

        LigneDetailExtraite ingredients = r.getLignesDetail().get(1);
        assertThat(ingredients.getPrixUnitaire()).isEqualTo(dh("613.33"));
        assertThat(ingredients.getTvaPct()).isEqualTo(dh("20.00"));
        assertThat(ingredients.getTotalFourniture()).isEqualTo(dh("736.00"));
        LigneDetailExtraite mot = r.getLignesDetail().get(3);
        assertThat(mot.getNombreHeures()).isEqualTo(dh("12.00"));
        assertThat(mot.getTarifHoraire()).isEqualTo(dh("60.00"));
        assertThat(mot.getMontantMoTtc()).isEqualTo(dh("864.00"));
    }

    @Test
    void rapport4_champsMasquesASaisir() throws IOException {
        RapportExtrait r = extraire(RAPPORT4);

        assertThat(r.getCabinet()).isNull();
        assertThat(r.getAssure().getNomPrenom()).isNull();
        assertThat(r.getDateSinistre()).isNull();
        assertThat(r.getChampsASaisir()).contains(
                "cabinet", "expert", "refSinistre", "dateSinistre", "dateMission", "compagnie",
                "assure.nomPrenom", "assure.immatriculation", "assure.numeroPolice",
                "adversaire.nomPrenom", "reparateur", "vehicule.marque", "vehicule.numeroSerie",
                "dateExpertise", "dateRapport", "commentaire");
        // Lus dans le PDF, donc pas à saisir
        assertThat(r.getChampsASaisir()).doesNotContain(
                "natureContrat", "montantIndemnisation", "montantLettres", "lieuRapport", "vehicule.kilometrage");
    }

    @Test
    void rapport5_enTeteMontantsEtSixLignes() throws IOException {
        RapportExtrait r = extraire(RAPPORT5);

        assertThat(r.getNumeroRapport()).isEqualTo("268202-0284");
        assertThat(r.getTotalGeneral().getTtc()).isEqualTo(dh("2400.00"));
        assertThat(r.getTotalGeneral().getTva()).isEqualTo(dh("0.00"));
        assertThat(r.getMontantIndemnisation()).isEqualTo(dh("2400.00"));

        assertThat(r.getLignesDetail()).hasSize(6)
                .extracting(LigneDetailExtraite::getDesignation)
                .containsExactly("MOP", "PP ET ING", "JUPE", "MOT", "PARE CHOC AR", "RENFORT");
        assertThat(r.getLignesDetail()).extracting(LigneDetailExtraite::getTypePiece)
                .containsExactly(null, null, TypePiece.REEMPLOI, null, TypePiece.REEMPLOI, TypePiece.REEMPLOI);
        assertThat(r.getLignesDetail()).allSatisfy(l -> assertThat(l.getZoneChoc()).isEqualTo(ZoneChoc.ARRIERE));
        assertThat(r.getLignesDetail().get(1).getOperation()).isEqualTo(TypeOperation.ECHANGE);
        assertThat(r.getLignesDetail().get(4).getTotalFourniture()).isEqualTo(dh("400.00"));
    }

    @Test
    void lesDeuxRapportsPassentLes28Controles() throws IOException {
        for (Path rapport : new Path[]{RAPPORT4, RAPPORT5}) {
            assertThat(new ControleCoherenceService().controler(extraire(rapport)))
                    .hasSize(28)
                    .allSatisfy(c -> assertThat(c.ok()).as("%s : %s", rapport.getFileName(), c).isTrue());
        }
    }

    private RapportExtrait extraire(Path pdf) throws IOException {
        return extracteur.extraire(Files.readAllBytes(pdf));
    }

    private static BigDecimal dh(String montant) {
        return new BigDecimal(montant);
    }
}
