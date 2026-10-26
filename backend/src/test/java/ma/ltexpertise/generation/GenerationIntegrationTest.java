package ma.ltexpertise.generation;

import ma.ltexpertise.ConfigurationMinioTest;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.dossier.ServiceDossier;
import ma.ltexpertise.importation.RapportPdfFactice;
import ma.ltexpertise.importation.ResultatImport;
import ma.ltexpertise.importation.ServiceImport;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot 6 (F9, P10) : rapport PDF régénéré depuis un dossier validé, exports Excel, traces EXPORT_* (P7). Contexte
 * (donc base) propre : l'import d'un rapport réel de samples/ ne doit pas en faire un doublon pour les autres tests.
 */
@SpringBootTest(properties = "spring.application.name=lt-expertise-generation")
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class GenerationIntegrationTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();
    private static final Path ECHANTILLONS = Path.of("..", "samples");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ServiceImport imports;
    @Autowired
    private ServiceDossier dossiers;
    @Autowired
    private UtilisateurRepository utilisateurs;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void rapportPdf_fideleAuxDonneesValidees_etTrace() throws Exception {
        String numero = importerEtValider(RapportPdfFactice.numero(nouveauNumero()));

        MockHttpServletResponse reponse = mvc.perform(get("/api/rapports/{numero}/pdf", numero))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("rapport-" + numero + ".pdf")))
                .andReturn().getResponse();

        byte[] pdf = reponse.getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String texte;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(2);
            texte = new PDFTextStripper().getText(document);
        }
        assertThat(texte).contains("RAPPORT D'EXPERTISE : " + numero, "ASSURE FICTIF", "EVALUATION DES DOMMAGES",
                "MO 1 Tôlerie", "2320.00", "Montant de l'indemnisation : 2320.00 DH", "PP ET ING", "613.33",
                "COMMENTAIRE RAPPORT", "- DOSSIER DE TEST.", "Rapport régénéré le", "consultant");
        assertThat(texte).containsPattern("Page 2 /\\s*2");

        mvc.perform(get("/api/rapports/historique"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroRapport").value(numero))
                .andExpect(jsonPath("$[0].generePar").value("consultant"))
                .andExpect(jsonPath("$[0].assure").value("ASSURE FICTIF"));
    }

    /**
     * Rapport réel de samples/ : import, validation, régénération. Le PDF et l'image de ses pages sont écrits dans
     * target/rapport-regenere/ pour comparaison visuelle avec l'original (jamais commités).
     */
    @Test
    @EnabledIf("echantillonsPresents")
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void rapportPdf_dUnRapportReel() throws Exception {
        Path source = ECHANTILLONS.resolve("A_rapport1_CLASSIQUE_2026.pdf");
        ResultatImport r = imports.importer(source.getFileName().toString(), Files.readAllBytes(source),
                utilisateurs.findByIdentifiant("gestionnaire").orElseThrow(), null);
        assumeTrue(r.issue().estAccepte(), "déjà importé par un autre test");
        dossiers.valider(r.numeroRapport(), utilisateurs.findByIdentifiant("gestionnaire").orElseThrow());

        byte[] pdf = mvc.perform(get("/api/rapports/{numero}/pdf", r.numeroRapport()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        Path sortie = Files.createDirectories(Path.of("target", "rapport-regenere"));
        Files.write(sortie.resolve("rapport.pdf"), pdf);
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDFRenderer rendu = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                ImageIO.write(rendu.renderImageWithDPI(i, 90), "png", sortie.resolve("page-" + (i + 1) + ".png").toFile());
            }
            String texte = new PDFTextStripper().getText(document);
            assertThat(texte).contains(r.numeroRapport(), "EVALUATION DES DOMMAGES", "COMMENTAIRE RAPPORT",
                    "LA VALEUR A NEUF DU VEHICULE");
        }
    }

    static boolean echantillonsPresents() {
        return Files.isDirectory(ECHANTILLONS);
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void rapportPdf_refusePourUnDossierNonValide() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(get("/api/rapports/{numero}/pdf", numero))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("dossier validé")));
        mvc.perform(get("/api/rapports/{numero}/pdf", "INCONNU-0")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void exportExcel_dUnDossier() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).prixFaux("200.00"));

        byte[] xlsx = mvc.perform(get("/api/exports/dossiers/{numero}", numero))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("dossier-" + numero + ".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(nomsDesFeuilles(classeur)).containsExactly("Dossier", "Évaluation", "Détail des pièces", "Contrôles");
            Sheet fiche = classeur.getSheet("Dossier");
            assertThat(fiche.getRow(1).getCell(1).getStringCellValue()).isEqualTo(numero);
            Sheet pieces = classeur.getSheet("Détail des pièces");
            assertThat(pieces.getRow(2).getCell(3).getStringCellValue()).isEqualTo("PP ET ING");
            assertThat(pieces.getRow(2).getCell(5).getNumericCellValue()).isEqualTo(200.00);
            assertThat(pieces.getRow(2).getCell(13).getStringCellValue()).isEqualTo("Non");
        }
        assertThat(jdbc.queryForObject("select count(*) from journal_audit j join rapport_expertise r on r.id = j.rapport_id "
                + "where j.action = 'EXPORT_EXCEL' and r.numero_rapport = ?", Integer.class, numero)).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void exportExcel_deLaListeFiltree_toutesLesLignes() throws Exception {
        String prefixe = "XLS-" + System.nanoTime() % 1_000_000_000L;
        for (int i = 1; i <= 3; i++) {
            importer(RapportPdfFactice.numero(prefixe + "-" + i));
        }

        byte[] xlsx = mvc.perform(get("/api/exports/dossiers").param("q", prefixe))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet f = classeur.getSheet("Dossiers");
            assertThat(f.getLastRowNum()).isEqualTo(3);
            assertThat(f.getRow(0).getCell(0).getStringCellValue()).isEqualTo("N° de rapport");
            assertThat(f.getRow(1).getCell(0).getStringCellValue()).startsWith(prefixe);
            assertThat(f.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Prêt à valider");
        }
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void exportExcel_deLAnalyseDesPrix() throws Exception {
        importer(RapportPdfFactice.numero(nouveauNumero()));

        byte[] xlsx = mvc.perform(get("/api/exports/prix"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(nomsDesFeuilles(classeur)).containsExactly("Analyse des prix", "Synthèse");
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATEUR")
    void exportDuJournal_parLAdministrateur() throws Exception {
        byte[] xlsx = mvc.perform(get("/api/journal-audit/export").param("action", "CONNEXION"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("journal-audit-")))
                .andReturn().getResponse().getContentAsByteArray();

        try (Workbook classeur = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(classeur.getSheet("Journal d'audit").getRow(0).getCell(2).getStringCellValue()).isEqualTo("Action");
        }
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void exportDuJournal_refuseHorsAdministrateur() throws Exception {
        mvc.perform(get("/api/journal-audit/export")).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ outils

    private String importer(RapportPdfFactice rapport) {
        ResultatImport r = imports.importer("rapport.pdf", rapport.pdf(),
                utilisateurs.findByIdentifiant("gestionnaire").orElseThrow(), null);
        assertThat(r.issue().estAccepte()).as(r.toString()).isTrue();
        return r.numeroRapport();
    }

    private String importerEtValider(RapportPdfFactice rapport) {
        String numero = importer(rapport);
        dossiers.valider(numero, utilisateurs.findByIdentifiant("gestionnaire").orElseThrow());
        return numero;
    }

    private static List<String> nomsDesFeuilles(Workbook classeur) {
        List<String> noms = new ArrayList<>();
        classeur.forEach(f -> noms.add(f.getSheetName()));
        return noms;
    }

    private static String nouveauNumero() {
        return "GEN-" + System.nanoTime() % 1_000_000_000L + "-" + COMPTEUR.incrementAndGet();
    }
}
