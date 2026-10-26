package ma.ltexpertise.importation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.IsObjectLegalHoldEnabledArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import ma.ltexpertise.ConfigurationMinioTest;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.audit.ActionAudit;
import ma.ltexpertise.audit.JournalAudit;
import ma.ltexpertise.audit.JournalAuditRepository;
import ma.ltexpertise.commun.Empreintes;
import ma.ltexpertise.dossier.RapportExpertise;
import ma.ltexpertise.dossier.RapportExpertiseRepository;
import ma.ltexpertise.dossier.referentiel.NatureRapport;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import ma.ltexpertise.dossier.referentiel.TypeProcedure;
import ma.ltexpertise.extraction.ExtracteurRapport;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import de bout en bout sur PostgreSQL et MinIO réels (Testcontainers) : stockage verrouillé, extraction,
 * contrôles, statut calculé, journal d'audit, API REST et lots asynchrones.
 * Les tests partagent la base : chacun utilise ses propres n° de rapport.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class ImportPdfIntegrationTest {

    private static final String GESTIONNAIRE = "gestionnaire.import";
    private static final String CONSULTANT = "consultant.import";
    private static final AtomicInteger COMPTEUR = new AtomicInteger();
    private static final Path ECHANTILLONS = Path.of("..", "samples");

    @Autowired
    private ServiceImport service;
    @Autowired
    private ImportFichierRepository imports;
    @Autowired
    private RapportExpertiseRepository rapports;
    @Autowired
    private JournalAuditRepository journal;
    @Autowired
    private UtilisateurRepository utilisateurs;
    @Autowired
    private MinioClient minio;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private Utilisateur gestionnaire;

    @BeforeEach
    void comptes() {
        gestionnaire = compte(GESTIONNAIRE, Role.GESTIONNAIRE);
        compte(CONSULTANT, Role.CONSULTATION);
    }

    // ------------------------------------------------------------------ service : PDF accepté

    @Test
    void importeUnRapportCoherent_pretAValider() throws Exception {
        String numero = nouveauNumero();
        byte[] pdf = GenerateurPdf.rapport(numero);

        ResultatImport resultat = service.importer("rapport.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.ACCEPTE);
        assertThat(resultat.numeroRapport()).isEqualTo(numero);
        assertThat(resultat.statutDossier()).isEqualTo(StatutDossier.PRET_A_VALIDER);
        assertThat(resultat.controlesConformes()).isEqualTo(28);
        assertThat(resultat.controlesTotal()).isEqualTo(28);
        assertThat(resultat.champsASaisir()).isZero();

        ImportFichier fichier = imports.findById(resultat.importId()).orElseThrow();
        assertThat(fichier.getStatutImport()).isEqualTo(StatutImport.ACCEPTE);
        assertThat(fichier.getHashSha256()).isEqualTo(Empreintes.sha256(pdf));
        assertThat(fichier.getNombrePages()).isEqualTo((short) 2);
        assertThat(fichier.getGabaritDetecte()).isEqualTo("tcpdf-expertise-v1");

        // P1 : l'original est dans MinIO, identique octet pour octet, et verrouillé
        try (var objet = minio.getObject(GetObjectArgs.builder().bucket(fichier.getBucket())
                .object(fichier.getCleObjet()).versionId(fichier.getVersionObjet()).build())) {
            assertThat(objet.readAllBytes()).isEqualTo(pdf);
        }
        assertThat(minio.isObjectLegalHoldEnabled(IsObjectLegalHoldEnabledArgs.builder().bucket(fichier.getBucket())
                .object(fichier.getCleObjet()).versionId(fichier.getVersionObjet()).build())).isTrue();
        assertThatThrownBy(() -> minio.removeObject(RemoveObjectArgs.builder().bucket(fichier.getBucket())
                .object(fichier.getCleObjet()).versionId(fichier.getVersionObjet()).build()))
                .isInstanceOf(ErrorResponseException.class);

        // Dossier rempli par l'extraction
        RapportExpertise rapport = rapports.findById(resultat.rapportId()).orElseThrow();
        assertThat(rapport.getStatut()).isEqualTo(StatutDossier.PRET_A_VALIDER);
        assertThat(rapport.getNatureRapport()).isEqualTo(NatureRapport.DEFINITIF);
        assertThat(rapport.getTypeProcedure()).isEqualTo(TypeProcedure.PROCEDURE_CLASSIQUE);
        assertThat(rapport.getCabinet()).isEqualTo("CABINET TEST");
        assertThat(rapport.getTotalGeneral().getTtc()).isEqualByComparingTo("2320.00");
        assertThat(rapport.getMontantIndemnisation()).isEqualByComparingTo("2320.00");
        assertThat(rapport.getValidePar()).isNull();
        assertThat(rapport.getImportFichier().getId()).isEqualTo(fichier.getId());
        assertThat(compter("partie", rapport)).isEqualTo(1);
        assertThat(compter("vehicule", rapport)).isEqualTo(1);
        assertThat(compter("evaluation_main_oeuvre", rapport)).isEqualTo(7);
        assertThat(compter("fourniture", rapport)).isEqualTo(4);
        assertThat(compter("ligne_detail", rapport)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(*) from resultat_controle where rapport_id = ? and type = 'COHERENCE' "
                + "and conforme", Integer.class, rapport.getId())).isEqualTo(28);
        assertThat(jdbc.queryForObject("select count(*) from resultat_controle where rapport_id = ? "
                + "and type = 'CHAMP_MANQUANT'", Integer.class, rapport.getId())).isZero();

        // P7
        List<JournalAudit> traces = journal.findByRapportIdOrderByHorodatageAscIdAsc(rapport.getId());
        assertThat(traces).singleElement().satisfies(t -> {
            assertThat(t.getAction()).isEqualTo(ActionAudit.IMPORT);
            assertThat(t.getIdentifiantUtilisateur()).isEqualTo(GESTIONNAIRE);
            assertThat(t.getDetails()).containsEntry("statutDossier", "PRET_A_VALIDER")
                    .containsEntry("controlesConformes", "28/28");
        });
    }

    @Test
    void unChampMasque_signaleSansBloquerLaValidation() {
        byte[] pdf = RapportPdfFactice.numero(nouveauNumero()).cabinetMasque().pdf();

        ResultatImport resultat = service.importer("masque.pdf", pdf, gestionnaire, null);

        assertThat(resultat.statutDossier()).isEqualTo(StatutDossier.PRET_A_VALIDER);
        assertThat(resultat.champsASaisir()).isEqualTo(1);
        assertThat(jdbc.queryForList("select champ from resultat_controle where rapport_id = ? and type = 'CHAMP_MANQUANT'",
                String.class, resultat.rapportId())).containsExactly("cabinet");
        assertThat(jdbc.queryForObject("select bloquant from resultat_controle where rapport_id = ? "
                + "and type = 'CHAMP_MANQUANT'", Boolean.class, resultat.rapportId())).isFalse();
        assertThat(rapports.findById(resultat.rapportId()).orElseThrow().getCabinet()).isNull();
    }

    @Test
    void unControleEnEchec_aVerifier() {
        byte[] pdf = RapportPdfFactice.numero(nouveauNumero()).indemnisation("2400.00").pdf();

        ResultatImport resultat = service.importer("incoherent.pdf", pdf, gestionnaire, null);

        assertThat(resultat.statutDossier()).isEqualTo(StatutDossier.A_VERIFIER);
        assertThat(resultat.controlesConformes()).isEqualTo(26);
        assertThat(jdbc.queryForList("select code from resultat_controle where rapport_id = ? and not conforme "
                + "and type = 'COHERENCE'", String.class, resultat.rapportId()))
                .containsExactlyInAnyOrder("INDEMNISATION", "MONTANT_EN_LETTRES");
    }

    @Test
    void detecteLaNatureReforme() {
        byte[] pdf = RapportPdfFactice.numero(nouveauNumero()).titre("RAPPORT D'EXPERTISE REFORME").pdf();

        ResultatImport resultat = service.importer("reforme.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.ACCEPTE);
        assertThat(rapports.findById(resultat.rapportId()).orElseThrow().getNatureRapport())
                .isEqualTo(NatureRapport.REFORME);
    }

    @Test
    void f14_lePdfCompleteLeDossierEnCoursDuMemeNumero() {
        String numero = nouveauNumero();
        Long enCours = rapports.save(new RapportExpertise(numero, StatutDossier.EN_COURS)).getId();

        ResultatImport resultat = service.importer("final.pdf", GenerateurPdf.rapport(numero), gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.RATTACHE_DOSSIER_EN_COURS);
        assertThat(resultat.rapportId()).isEqualTo(enCours);
        RapportExpertise rapport = rapports.findById(enCours).orElseThrow();
        assertThat(rapport.getStatut()).isEqualTo(StatutDossier.PRET_A_VALIDER);
        assertThat(rapport.getMontantIndemnisation()).isEqualByComparingTo("2320.00");
        assertThat(rapport.getImportFichier().getId()).isEqualTo(resultat.importId());
    }

    // ------------------------------------------------------------------ service : refus

    @Test
    void p2_leMemeFichierNEstPasImporteDeuxFois() {
        byte[] pdf = GenerateurPdf.rapport(nouveauNumero());
        ResultatImport premier = service.importer("rapport.pdf", pdf, gestionnaire, null);

        ResultatImport second = service.importer("copie-du-rapport.pdf", pdf, gestionnaire, null);

        assertThat(second.issue()).isEqualTo(IssueImport.DOUBLON_FICHIER);
        assertThat(second.importId()).isEqualTo(premier.importId());
        assertThat(second.rapportId()).isNull();
        assertThat(tracesRefus("copie-du-rapport.pdf")).isEqualTo(1);
    }

    @Test
    void p2_unAutreFichierAuMemeNumeroEstRefuseEtConserve() {
        String numero = nouveauNumero();
        service.importer("v1.pdf", GenerateurPdf.rapport(numero), gestionnaire, null);

        ResultatImport resultat = service.importer("v2.pdf", GenerateurPdf.autreVersion(numero), gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.REFUSE_DOUBLON_NUMERO);
        assertThat(resultat.rapportId()).isNull();
        ImportFichier refuse = imports.findById(resultat.importId()).orElseThrow();
        assertThat(refuse.getStatutImport()).isEqualTo(StatutImport.REFUSE_DOUBLON_NUMERO);
        assertThat(refuse.getMotifRefus()).contains(numero);
        assertThat(service.lireOriginal(refuse.getId())).isPresent();
        assertThat(jdbc.queryForObject("select count(*) from rapport_expertise where numero_rapport = ?",
                Integer.class, numero)).isEqualTo(1);
    }

    @Test
    void unModeleInconnuEstRefuseMaisLePdfEstConserve() {
        byte[] pdf = GenerateurPdf.pdf("DEVIS GARAGE " + nouveauNumero(), 2, "page 2");

        ResultatImport resultat = service.importer("devis.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.REFUSE_GABARIT);
        assertThat(resultat.rapportId()).isNull();
        assertThat(imports.findById(resultat.importId()).orElseThrow().getMotifRefus()).contains("Modèle non reconnu");
        assertThat(service.lireOriginal(resultat.importId()).orElseThrow().contenu()).isEqualTo(pdf);
    }

    @Test
    void unTitreSansLesTableauxDuGabaritEstRefuseParLExtraction() {
        String numero = nouveauNumero();

        ResultatImport resultat = service.importer("incomplet.pdf", GenerateurPdf.titreSeul(numero), gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.REFUSE_GABARIT);
        assertThat(resultat.motif()).contains("Évaluation des dommages");
        assertThat(jdbc.queryForObject("select count(*) from rapport_expertise where numero_rapport = ?",
                Integer.class, numero)).isZero();
    }

    @Test
    void unPdfIllisibleEstConserveAvecLeStatutErreurDeLecture() {
        byte[] pdf = ("%PDF-1.7 tronqué " + nouveauNumero()).getBytes(StandardCharsets.UTF_8);

        ResultatImport resultat = service.importer("corrompu.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.ERREUR_LECTURE);
        assertThat(imports.findById(resultat.importId()).orElseThrow().getStatutImport())
                .isEqualTo(StatutImport.ERREUR_LECTURE);
    }

    @Test
    void unFichierQuiNEstPasUnPdfNEstPasEnregistre() {
        byte[] texte = ("pas un pdf " + nouveauNumero()).getBytes(StandardCharsets.UTF_8);

        ResultatImport resultat = service.importer("notes.pdf", texte, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.FICHIER_INVALIDE);
        assertThat(resultat.importId()).isNull();
        assertThat(imports.existsByHashSha256(Empreintes.sha256(texte))).isFalse();
    }

    // ------------------------------------------------------------------ API REST

    @Test
    @WithMockUser(username = GESTIONNAIRE, roles = "GESTIONNAIRE")
    void api_unFichierEstTraiteImmediatement() throws Exception {
        String numero = nouveauNumero();

        mvc.perform(multipart("/api/imports").file(fichier("a.pdf", GenerateurPdf.rapport(numero))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/api/imports/")))
                .andExpect(jsonPath("$.statut").value("TERMINE"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.reussis").value(1))
                .andExpect(jsonPath("$.pretsAValider").value(1))
                .andExpect(jsonPath("$.resultats[0].numeroRapport").value(numero))
                .andExpect(jsonPath("$.resultats[0].statutDossier").value("PRET_A_VALIDER"));
    }

    @Test
    @WithMockUser(username = GESTIONNAIRE, roles = "GESTIONNAIRE")
    void api_unLotEstTraiteEnArrierePlan_recapitulatifConsultable() throws Exception {
        MvcResult reponse = mvc.perform(multipart("/api/imports")
                        .file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero())))
                        .file(fichier("b.pdf", RapportPdfFactice.numero(nouveauNumero()).cabinetMasque().pdf()))
                        .file(fichier("c.pdf", RapportPdfFactice.numero(nouveauNumero()).indemnisation("9.99").pdf()))
                        .file(fichier("d.txt", "texte".getBytes(StandardCharsets.UTF_8)))
                        .with(csrf()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.total").value(4))
                .andReturn();
        String adresse = reponse.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(adresse).isNotNull();

        // Interrogé dans le fil du test, qui porte l'utilisateur de @WithMockUser.
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).pollInSameThread().untilAsserted(() ->
                assertThat(lireJson(adresse).get("statut").asText()).isEqualTo("TERMINE"));

        JsonNode recap = lireJson(adresse);
        assertThat(recap.get("traites").asInt()).isEqualTo(4);
        assertThat(recap.get("reussis").asInt()).isEqualTo(3);
        // Le cabinet masqué de b.pdf est signalé sans bloquer : prêt à valider
        assertThat(recap.get("pretsAValider").asInt()).isEqualTo(2);
        assertThat(recap.get("aCompleter").asInt()).isEqualTo(0);
        assertThat(recap.get("aVerifier").asInt()).isEqualTo(1);
        assertThat(recap.get("refuses").asInt()).isEqualTo(1);
        assertThat(recap.get("enErreur").asInt()).isZero();
        assertThat(recap.get("resultats").findValuesAsText("nomFichier")).containsExactly("a.pdf", "b.pdf", "c.pdf", "d.txt");
        assertThat(recap.get("resultats").get(3).get("issue").asText()).isEqualTo("FICHIER_INVALIDE");

        // Les fichiers acceptés sont rattachés au lot dans l'historique
        String lot = recap.get("lot").asText();
        assertThat(jdbc.queryForObject("select count(*) from import_fichier where lot_import = ?::uuid",
                Integer.class, lot)).isEqualTo(3);
    }

    @Test
    @WithMockUser(username = CONSULTANT, roles = "CONSULTATION")
    void api_unConsultantNePeutPasImporter() throws Exception {
        mvc.perform(multipart("/api/imports").file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero()))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "inconnu", roles = "GESTIONNAIRE")
    void api_unUtilisateurSansCompteApplicatifEstRefuse() throws Exception {
        mvc.perform(multipart("/api/imports").file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero()))).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = CONSULTANT, roles = "CONSULTATION")
    void api_unLotInconnuRepond404() throws Exception {
        mvc.perform(get("/api/imports/{lot}", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = CONSULTANT, roles = "CONSULTATION")
    void api_unConsultantLitLePdfOriginal() throws Exception {
        byte[] pdf = GenerateurPdf.rapport(nouveauNumero());
        Long importId = service.importer("original.pdf", pdf, gestionnaire, null).importId();

        mvc.perform(get("/api/imports/{id}/pdf", importId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
        mvc.perform(get("/api/imports/{id}/pdf", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = CONSULTANT, roles = "CONSULTATION")
    void api_laListeDesFichiersRefusesNeContientQueDesRefus() throws Exception {
        service.importer("refuse.pdf", GenerateurPdf.pdf("AUTRE DOCUMENT " + nouveauNumero(), 2, "x"),
                gestionnaire, null);
        service.importer("accepte.pdf", GenerateurPdf.rapport(nouveauNumero()), gestionnaire, null);

        mvc.perform(get("/api/imports").param("refuses", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].statutImport", everyItem(not("ACCEPTE"))))
                .andExpect(jsonPath("$.content[0].nomFichier").value("refuse.pdf"));
    }

    // ------------------------------------------------------------------ rapports réels

    /**
     * Les rapports réels de samples/ passent par tout l'import (stockage, extraction, contrôles, base) : aucun ne
     * doit échouer. Deux rapports ont des prix unitaires faux imprimés par le logiciel de l'expert sur la petite
     * visserie : le contrôle ligne à ligne de la page 2 les fait passer « à vérifier », pour correction à la main ;
     * tous les autres sont prêts à valider.
     */
    @Test
    @EnabledIf("echantillonsPresents")
    void lesRapportsReelsSontImportesSansErreur() throws Exception {
        List<Path> pdfs;
        try (Stream<Path> fichiers = Files.list(ECHANTILLONS)) {
            pdfs = fichiers.filter(p -> p.toString().toLowerCase().endsWith(".pdf")).sorted().toList();
        }
        ExtracteurRapport extracteur = new ExtracteurRapport();
        List<String> anomalies = new ArrayList<>();
        int prixFaux = 0;
        for (Path pdf : pdfs) {
            byte[] contenu = Files.readAllBytes(pdf);
            ResultatImport r = service.importer(pdf.getFileName().toString(), contenu, gestionnaire, null);
            if (!r.issue().estAccepte()) {
                anomalies.add(pdf.getFileName() + " : " + r.issue());
                continue;
            }
            List<String> echecs = jdbc.queryForList("select code from resultat_controle where rapport_id = ? "
                    + "and type = 'COHERENCE' and not conforme", String.class, r.rapportId());
            if (echecs.equals(List.of("PAGE2_LIGNES_PRIX")) && r.statutDossier() == StatutDossier.A_VERIFIER) {
                prixFaux++;
            } else if (!echecs.isEmpty() || r.statutDossier() != StatutDossier.PRET_A_VALIDER) {
                anomalies.add(pdf.getFileName() + " : statut " + r.statutDossier() + ", contrôles en échec " + echecs);
            }
            int lignes = jdbc.queryForObject("select count(*) from ligne_detail where rapport_id = ?", Integer.class,
                    r.rapportId());
            if (lignes != extracteur.extraire(contenu).getLignesDetail().size()) {
                anomalies.add(pdf.getFileName() + " : lignes de détail perdues à l'enregistrement");
            }
        }
        assertThat(anomalies).as("rapports réels mal importés").isEmpty();
        assertThat(prixFaux).as("rapports aux prix unitaires faux").isEqualTo(2);
    }

    static boolean echantillonsPresents() {
        return Files.isDirectory(ECHANTILLONS);
    }

    // ------------------------------------------------------------------ outils

    private Utilisateur compte(String identifiant, Role role) {
        return utilisateurs.findByIdentifiant(identifiant)
                .orElseGet(() -> utilisateurs.save(new Utilisateur(identifiant, "Test", "Import",
                        "$2a$10$hachefictif", role)));
    }

    private JsonNode lireJson(String adresse) throws Exception {
        String corps = mvc.perform(get(adresse)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(corps);
    }

    private int compter(String table, RapportExpertise rapport) {
        return jdbc.queryForObject("select count(*) from " + table + " where rapport_id = ?", Integer.class, rapport.getId());
    }

    private static String nouveauNumero() {
        return "IMP-" + System.nanoTime() % 1_000_000_000_000L + "-" + COMPTEUR.incrementAndGet();
    }

    private static MockMultipartFile fichier(String nom, byte[] contenu) {
        return new MockMultipartFile("fichiers", nom, MediaType.APPLICATION_PDF_VALUE, contenu);
    }

    private int tracesRefus(String nomFichier) {
        return jdbc.queryForObject("select count(*) from journal_audit where action = 'IMPORT_REFUSE' "
                + "and details ->> 'nomFichier' = ?", Integer.class, nomFichier);
    }
}
