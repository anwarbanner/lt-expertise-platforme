package ma.ltexpertise.importation;

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
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import de bout en bout sur PostgreSQL et MinIO réels (Testcontainers) : service, stockage verrouillé,
 * base, journal d'audit et API REST. Les tests partagent la base : chacun utilise ses propres n° de rapport.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class ImportPdfIntegrationTest {

    private static final String GESTIONNAIRE = "gestionnaire.import";
    private static final String CONSULTANT = "consultant.import";
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

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

    private Utilisateur gestionnaire;

    @BeforeEach
    void comptes() {
        gestionnaire = compte(GESTIONNAIRE, Role.GESTIONNAIRE);
        compte(CONSULTANT, Role.CONSULTATION);
    }

    // ------------------------------------------------------------------ service

    @Test
    void importeUnPdfValide_conserveLOriginalVerrouilleEtCreeLeDossier() throws Exception {
        String numero = nouveauNumero();
        byte[] pdf = GenerateurPdf.rapport(numero);

        ResultatImport resultat = service.importer("rapport.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.ACCEPTE);
        assertThat(resultat.numeroRapport()).isEqualTo(numero);

        ImportFichier fichier = imports.findById(resultat.importId()).orElseThrow();
        assertThat(fichier.getStatutImport()).isEqualTo(StatutImport.ACCEPTE);
        assertThat(fichier.getHashSha256()).isEqualTo(Empreintes.sha256(pdf));
        assertThat(fichier.getTailleOctets()).isEqualTo(pdf.length);
        assertThat(fichier.getNombrePages()).isEqualTo((short) 2);
        assertThat(fichier.getGabaritDetecte()).isEqualTo(AnalyseurGabarit.GABARIT_V1);
        assertThat(fichier.getCleObjet()).isEqualTo(ServiceImport.cleObjet(fichier.getHashSha256()));
        assertThat(fichier.getVersionObjet()).isNotBlank();

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

        RapportExpertise rapport = rapports.findById(resultat.rapportId()).orElseThrow();
        assertThat(rapport.getNumeroRapport()).isEqualTo(numero);
        assertThat(rapport.getStatut()).isEqualTo(StatutDossier.IMPORTE);
        assertThat(rapport.getNatureRapport()).isEqualTo(NatureRapport.DEFINITIF);
        assertThat(rapport.getGabaritVersion()).isEqualTo(AnalyseurGabarit.GABARIT_V1);
        assertThat(rapport.getImportFichier().getId()).isEqualTo(fichier.getId());

        // P7
        List<JournalAudit> traces = journal.findByRapportIdOrderByHorodatageAscIdAsc(rapport.getId());
        assertThat(traces).singleElement().satisfies(t -> {
            assertThat(t.getAction()).isEqualTo(ActionAudit.IMPORT);
            assertThat(t.getIdentifiantUtilisateur()).isEqualTo(GESTIONNAIRE);
            assertThat(t.getEntiteId()).isEqualTo(fichier.getId());
            assertThat(t.getDetails()).containsEntry("issue", "ACCEPTE");
        });
    }

    @Test
    void detecteLaNatureReforme() {
        String numero = nouveauNumero();

        ResultatImport resultat = service.importer("reforme.pdf",
                GenerateurPdf.pdf("RAPPORT D'EXPERTISE REFORME : " + numero, 2, "page 2"), gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.ACCEPTE);
        assertThat(rapports.findById(resultat.rapportId()).orElseThrow().getNatureRapport())
                .isEqualTo(NatureRapport.REFORME);
    }

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
        assertThat(tracesRefus("v2.pdf")).isEqualTo(1);
    }

    @Test
    void unModeleInconnuEstRefuseMaisLePdfEstConserve() {
        byte[] pdf = GenerateurPdf.pdf("DEVIS GARAGE " + nouveauNumero(), 2, "page 2");

        ResultatImport resultat = service.importer("devis.pdf", pdf, gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.REFUSE_GABARIT);
        assertThat(resultat.rapportId()).isNull();
        ImportFichier refuse = imports.findById(resultat.importId()).orElseThrow();
        assertThat(refuse.getStatutImport()).isEqualTo(StatutImport.REFUSE_GABARIT);
        assertThat(refuse.getMotifRefus()).contains("Modèle non reconnu");
        assertThat(refuse.getGabaritDetecte()).isNull();
        assertThat(service.lireOriginal(refuse.getId()).orElseThrow().contenu()).isEqualTo(pdf);
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
        assertThat(tracesRefus("notes.pdf")).isEqualTo(1);
    }

    @Test
    void f14_lePdfEstRattacheAuDossierEnCoursDuMemeNumero() {
        String numero = nouveauNumero();
        Long enCours = rapports.save(new RapportExpertise(numero, StatutDossier.EN_COURS)).getId();

        ResultatImport resultat = service.importer("final.pdf", GenerateurPdf.rapport(numero), gestionnaire, null);

        assertThat(resultat.issue()).isEqualTo(IssueImport.RATTACHE_DOSSIER_EN_COURS);
        assertThat(resultat.rapportId()).isEqualTo(enCours);
        RapportExpertise rapport = rapports.findById(enCours).orElseThrow();
        assertThat(rapport.getStatut()).isEqualTo(StatutDossier.IMPORTE);
        assertThat(rapport.getImportFichier().getId()).isEqualTo(resultat.importId());
    }

    // ------------------------------------------------------------------ API REST

    @Test
    @WithMockUser(username = GESTIONNAIRE, roles = "GESTIONNAIRE")
    void api_unGestionnaireDeposeUnLot() throws Exception {
        mvc.perform(multipart("/api/imports")
                        .file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero())))
                        .file(fichier("b.pdf", GenerateurPdf.rapport(nouveauNumero())))
                        .file(fichier("c.txt", "texte".getBytes(StandardCharsets.UTF_8)))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lot", notNullValue()))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.acceptes").value(2))
                .andExpect(jsonPath("$.refuses").value(1))
                .andExpect(jsonPath("$.enErreur").value(0))
                .andExpect(jsonPath("$.resultats[2].issue").value("FICHIER_INVALIDE"));
    }

    @Test
    @WithMockUser(username = CONSULTANT, roles = "CONSULTATION")
    void api_unConsultantNePeutPasImporter() throws Exception {
        mvc.perform(multipart("/api/imports")
                        .file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero())))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "inconnu", roles = "GESTIONNAIRE")
    void api_unUtilisateurSansCompteApplicatifEstRefuse() throws Exception {
        mvc.perform(multipart("/api/imports")
                        .file(fichier("a.pdf", GenerateurPdf.rapport(nouveauNumero())))
                        .with(csrf()))
                .andExpect(status().isForbidden());
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
                .andExpect(jsonPath("$.content[0].nomFichier").value("refuse.pdf"))
                .andExpect(jsonPath("$.content[0].importePar").value(GESTIONNAIRE));
    }

    // ------------------------------------------------------------------ outils

    private Utilisateur compte(String identifiant, Role role) {
        return utilisateurs.findByIdentifiant(identifiant)
                .orElseGet(() -> utilisateurs.save(new Utilisateur(identifiant, "Test", "Import",
                        "$2a$10$hachefictif", role)));
    }

    private static String nouveauNumero() {
        return "IMP-" + System.nanoTime() + "-" + COMPTEUR.incrementAndGet();
    }

    private static MockMultipartFile fichier(String nom, byte[] contenu) {
        return new MockMultipartFile("fichiers", nom, MediaType.APPLICATION_PDF_VALUE, contenu);
    }

    private int tracesRefus(String nomFichier) {
        return jdbc.queryForObject("select count(*) from journal_audit where action = 'IMPORT_REFUSE' "
                + "and details ->> 'nomFichier' = ?", Integer.class, nomFichier);
    }
}
