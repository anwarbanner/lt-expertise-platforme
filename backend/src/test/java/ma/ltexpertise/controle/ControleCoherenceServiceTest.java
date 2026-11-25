package ma.ltexpertise.controle;

import ma.ltexpertise.dossier.referentiel.PosteMainOeuvre;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.extraction.FabriqueRapportExtrait;
import ma.ltexpertise.extraction.RapportExtrait;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ControleCoherenceServiceTest {

    private final ControleCoherenceService service = new ControleCoherenceService();

    @Test
    void unRapportCoherentPasseLes28Controles() {
        List<Controle> controles = service.controler(FabriqueRapportExtrait.coherent());

        assertThat(controles).hasSize(28).allSatisfy(c -> assertThat(c.ok()).as(c.toString()).isTrue());
        assertThat(controles).extracting(Controle::code).doesNotHaveDuplicates()
                .contains("MO_1_TOLERIE_HEURES_TAUX", "SOMME_MO_TTC", "TOTAL_GENERAL_HT", "INDEMNISATION",
                        "MONTANT_EN_LETTRES", "PAGE2_MAIN_OEUVRE", "PAGE2_FOURNITURES", "PAGE2_LIGNES_PRIX");
    }

    @Test
    void chaqueControleRenvoieNomAttenduObtenuEtOk() {
        Controle tolerie = controle(FabriqueRapportExtrait.coherent(), "MO_1_TOLERIE_HEURES_TAUX");

        assertThat(tolerie.nom()).isEqualTo("MO 1 Tôlerie : heures × taux = HT");
        assertThat(tolerie.attendu()).isEqualByComparingTo("720.00");
        assertThat(tolerie.obtenu()).isEqualByComparingTo("720.00");
        assertThat(tolerie.ok()).isTrue();
    }

    @Test
    void detecteHeuresFoisTauxDifferentDuHt() {
        Controle c = controleApres(m -> m.posteMainOeuvre(PosteMainOeuvre.MO_1_TOLERIE,
                "12.00", "60.00", "700.00", "144.00", "844.00"), "MO_1_TOLERIE_HEURES_TAUX");

        assertThat(c.ok()).isFalse();
        assertThat(c.attendu()).isEqualByComparingTo("720.00");
        assertThat(c.obtenu()).isEqualByComparingTo("700.00");
    }

    @Test
    void detecteHtPlusTvaDifferentDuTtc() {
        assertThat(controleApres(m -> m.posteMainOeuvre(PosteMainOeuvre.MO_PEINTURE,
                "10.00", "60.00", "600.00", "120.00", "740.00"), "MO_PEINTURE_HT_TVA").ok()).isFalse();
    }

    @Test
    void detecteUneSommeDePostesDifferenteDuTotal() {
        List<Controle> controles = service.controler(FabriqueRapportExtrait.coherentAvec(m -> m.posteMainOeuvre(
                PosteMainOeuvre.MO_2_TOLERIE, "1.00", "60.00", "60.00", "12.00", "72.00")));

        assertThat(controles).filteredOn(c -> !c.ok()).extracting(Controle::code)
                .containsExactly("SOMME_MO_HT", "SOMME_MO_TVA", "SOMME_MO_TTC");
    }

    @Test
    void detecteUnTotalGeneralDifferentDeMoPlusFournitures() {
        assertThat(controleApres(m -> m.totalGeneral("1933.33", "386.67", "2420.00"), "TOTAL_GENERAL_TTC").ok())
                .isFalse();
    }

    @Test
    void indemnisationEgaleTotalTtcMoinsVetuste() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.vetusteTtc("320.00")
                .indemnisation("2000.00", "deux mille Dirhams").totalDerniereLignePage2("0.00", "864.00")
                .ajouterLignePage2("-320.00", "0.00"));

        assertThat(controle(r, "INDEMNISATION").ok()).isTrue();
        assertThat(controle(r, "PAGE2_FOURNITURES").ok()).isTrue();
    }

    @Test
    void p5_tvaRecuperable_indemnisationSurLeHt() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.tvaRecuperable(true).vetusteHt("0.00")
                .indemnisation("1933.33", "mille neuf cent trente trois Dirhams et trente trois centimes"));

        Controle c = controle(r, "INDEMNISATION");

        assertThat(c.ok()).isTrue();
        assertThat(c.nom()).contains("HT");
        assertThat(controle(r, "MONTANT_EN_LETTRES").ok()).isTrue();
    }

    /** « Vétusté HT » imprimée : elle se déduit du total général HT, quelle que soit la TVA. */
    @Test
    void vetusteHt_seDeduitDuTotalHt() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.tvaRecuperable(false).vetusteHt("100.00")
                .indemnisation("1833.33", "mille huit cent trente trois Dirhams et trente trois centimes"));

        Controle c = controle(r, "INDEMNISATION");

        assertThat(c.ok()).isTrue();
        assertThat(c.nom()).isEqualTo("Total général HT − vétusté HT = indemnisation");
        assertThat(c.attendu()).isEqualByComparingTo("1833.33");
    }

    /**
     * « Vétusté HT » sur des pièces à 20 % : la colonne « Total » de la page 2 la retire TVA comprise (rapport réel
     * 258202-2988 : 24 296,40 − 7 978,80 × 1,2 = 14 721,84). Pièce 250 HT, vétusté 40 % : 100 HT, 120 TTC.
     */
    @Test
    void vetusteHt_surPiecesA20_page2DeduitLaVetusteTtc() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.vetusteHt("100.00")
                .ajouterLigneFourniture("1", "250.00", "20.00", "40.00", "180.00"));

        assertThat(ControleCoherenceService.vetusteTtcFournitures(r)).isEqualByComparingTo("120.00");
    }

    /** « Vétusté HT » sur des pièces à 0 % (rapport réel 258202-2427) : retirée telle quelle à la page 2. */
    @Test
    void vetusteHt_surPiecesA0_page2DeduitLaVetusteTelleQuelle() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.vetusteHt("2100.00")
                .ajouterLigneFourniture("1", "3000.00", "0.00", "70.00", "900.00"));

        assertThat(ControleCoherenceService.vetusteTtcFournitures(r)).isEqualByComparingTo("2100.00");
    }

    /** Le contrôle de la page 2 retire la vétusté HT convertie : 736 − 120 = 616. */
    @Test
    void vetusteHt_controlePage2() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.vetusteHt("100.00")
                .indemnisation("1833.33", "mille huit cent trente trois Dirhams et trente trois centimes")
                .totalDerniereLignePage2("0.00", "864.00")
                .ajouterLigneFourniture("1", "250.00", "20.00", "40.00", "-120.00"));

        Controle c = controle(r, "PAGE2_FOURNITURES");

        assertThat(c.obtenu()).isEqualByComparingTo("616.00");
    }

    /** « Vétusté TTC » imprimée : elle se déduit du total général TTC. */
    @Test
    void vetusteTtc_seDeduitDuTotalTtc() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.tvaRecuperable(true).vetusteTtc("320.00")
                .indemnisation("2000.00", "deux mille Dirhams"));

        Controle c = controle(r, "INDEMNISATION");

        assertThat(c.nom()).isEqualTo("Total général TTC − vétusté TTC = indemnisation");
        assertThat(c.attendu()).isEqualByComparingTo("2000.00");
        assertThat(c.ok()).isTrue();
    }

    @Test
    void reforme_indemnisationEgaleValeurVenaleMoinsEpave() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.reforme("80000.00", "23000.00")
                .indemnisation("57000.00", "cinquante sept mille Dirhams"));

        assertThat(controle(r, "INDEMNISATION").ok()).isTrue();
    }

    @Test
    void indemnisationAbsenteEchoue() {
        Controle c = controleApres(m -> m.indemnisation(null, null), "INDEMNISATION");

        assertThat(c.ok()).isFalse();
        assertThat(c.attendu()).isEqualByComparingTo("2320.00");
        assertThat(c.obtenu()).isNull();
    }

    @Test
    void montantEnLettres_deuxMilleQuatreCentsDirhams() {
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.indemnisation("2400.00", "deux mille quatre cents Dirhams")
                .totalGeneral("2000.00", "400.00", "2400.00"));

        Controle c = controle(r, "MONTANT_EN_LETTRES");

        assertThat(c.attendu()).isEqualByComparingTo("2400");
        assertThat(c.obtenu()).isEqualByComparingTo("2400.00");
        assertThat(c.ok()).isTrue();
    }

    @Test
    void detecteUnMontantEnLettresDifferent() {
        assertThat(controleApres(m -> m.indemnisation("2320.00", "deux mille trois cents Dirhams"), "MONTANT_EN_LETTRES")
                .ok()).isFalse();
    }

    @Test
    void toleranceDe5Centimes() {
        assertThat(controleApres(m -> m.indemnisation("2320.05", "deux mille trois cent vingt Dirhams et cinq centimes"),
                "INDEMNISATION").ok()).isTrue();
        assertThat(controleApres(m -> m.indemnisation("2320.06", "deux mille trois cent vingt Dirhams et six centimes"),
                "INDEMNISATION").ok()).isFalse();
    }

    @Test
    void page2_detecteUnEcartAvecLaPage1() {
        Controle c = controleApres(m -> m.totalDerniereLignePage2("100.00", "864.00"), "PAGE2_FOURNITURES");

        assertThat(c.ok()).isFalse();
        assertThat(c.attendu()).isEqualByComparingTo("836.00");
        assertThat(c.obtenu()).isEqualByComparingTo("736.00");
    }

    @Test
    void page2_laToleranceCroitAvecLeNombreDeLignesArrondies() {
        // 41 lignes arrondies au centime : écart de 0,06 toléré (0,005 × 41 ≈ 0,20), comme sur un rapport réel.
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> {
            IntStream.range(0, 37).forEach(i -> m.ajouterLignePage2("0.00", "0.00"));
            m.ajouterLignePage2("0.06", "0.00");
        });

        assertThat(controle(r, "PAGE2_FOURNITURES").ok()).isTrue();
    }

    @Test
    void ligneDePage2_quantiteFoisPrixTvaEtVetusteEgaleLeTotal() {
        // Cas réel : 2 rivets à 20,64, vétusté 5 %, TVA 20 % → 47,06
        RapportExtrait r = FabriqueRapportExtrait.coherentAvec(m -> m.ajouterLigneFourniture("2", "20.64", "20.00", "5.00", "47.06"));

        Controle c = controle(r, "PAGE2_LIGNES_PRIX");

        assertThat(c.ok()).isTrue();
        assertThat(c.attendu()).isEqualByComparingTo("4");
        assertThat(c.obtenu()).isEqualByComparingTo("4");
    }

    @Test
    void ligneDePage2_toleranceSurUnPrixUnitaireArrondi() {
        // Cas réel : 104 × 0,99 × 1,2 = 123,55 ; le prix imprimé est arrondi au centime
        assertThat(controle(FabriqueRapportExtrait.coherentAvec(m -> m.ajouterLigneFourniture("104", "0.99", "20.00",
                "0.00", "123.55")), "PAGE2_LIGNES_PRIX").ok()).isTrue();
    }

    @Test
    void ligneDePage2_detecteUnPrixUnitaireNegatif() {
        // Cas réel : prix imprimé -95,70 pour un total juste de 434,10 (le vrai prix est 28,71)
        Controle c = controleApres(m -> m.ajouterLigneFourniture("14", "-95.70", "20.00", "10.00", "434.10"),
                "PAGE2_LIGNES_PRIX");

        assertThat(c.ok()).isFalse();
        assertThat(c.attendu()).isEqualByComparingTo("4");
        assertThat(c.obtenu()).isEqualByComparingTo("3");
    }

    @Test
    void ligneDePage2_prixFauxBloquantEtLigneNommee() {
        // Le gestionnaire corrige le prix avant de valider ; le libellé désigne la ligne du PDF
        Controle c = controleApres(m -> m.ajouterLigneFourniture("14", "-95.70", "20.00", "10.00", "434.10"),
                "PAGE2_LIGNES_PRIX");

        assertThat(c.bloque()).isTrue();
        assertThat(c.nom()).contains("ligne 4");
    }

    @Test
    void prixDepuisTotal_donneLePrixAttendu() {
        // Cas réels : 14 × ? × 1,2 × 0,9 = 434,10 ; 3 × ? × 1,2 × 0,9 = 88,45
        assertThat(ControleCoherenceService.prixDepuisTotal(new BigDecimal("14"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("434.10"))).isEqualByComparingTo("28.71");
        assertThat(ControleCoherenceService.prixDepuisTotal(new BigDecimal("3"), new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("88.45"))).isEqualByComparingTo("27.30");
        assertThat(ControleCoherenceService.prixCoherent(new BigDecimal("3"), new BigDecimal("27.30"),
                new BigDecimal("20"), new BigDecimal("10"), new BigDecimal("88.45"))).isTrue();
        assertThat(ControleCoherenceService.prixDepuisTotal(BigDecimal.ZERO, new BigDecimal("20"),
                new BigDecimal("10"), new BigDecimal("88.45"))).isNull();
    }

    @Test
    void statut_aVerifierSiUnControleEchoue() {
        List<Controle> controles = List.of(ok(), new Controle("X", "x", BigDecimal.ONE, BigDecimal.TEN, false));

        assertThat(service.statutApresImport(controles, List.of("cabinet"))).isEqualTo(StatutDossier.A_VERIFIER);
    }

    @Test
    void statut_unControleInformatifNeBloquePas() {
        List<Controle> controles = List.of(ok(), new Controle("X", "x", BigDecimal.ONE, BigDecimal.TEN, false, false));

        assertThat(service.statutApresImport(controles, List.of())).isEqualTo(StatutDossier.PRET_A_VALIDER);
    }

    @Test
    void statut_unChampAbsentDuPdfNeBloquePas() {
        assertThat(service.statutApresImport(List.of(ok()), List.of("assure.nomPrenom", "dateApresTravaux")))
                .isEqualTo(StatutDossier.PRET_A_VALIDER);
    }

    @Test
    void statut_aCompleterSIlManqueUneDonneeEssentielle() {
        assertThat(service.statutApresImport(List.of(ok()), List.of("assure.nomPrenom", "montantIndemnisation")))
                .isEqualTo(StatutDossier.A_COMPLETER);
    }

    @Test
    void statut_pretAValiderSinon_jamaisValide() {
        StatutDossier statut = service.statutApresImport(service.controler(FabriqueRapportExtrait.coherent()), List.of());

        assertThat(statut).isEqualTo(StatutDossier.PRET_A_VALIDER).isNotEqualTo(StatutDossier.VALIDE);
    }

    private Controle controleApres(Consumer<FabriqueRapportExtrait.Modification> modification, String code) {
        return controle(FabriqueRapportExtrait.coherentAvec(modification), code);
    }

    private Controle controle(RapportExtrait r, String code) {
        return service.controler(r).stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow();
    }

    private static Controle ok() {
        return new Controle("OK", "ok", BigDecimal.ONE, BigDecimal.ONE, true);
    }
}
