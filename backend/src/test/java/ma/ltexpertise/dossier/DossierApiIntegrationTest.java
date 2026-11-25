package ma.ltexpertise.dossier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ma.ltexpertise.ConfigurationMinioTest;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.importation.RapportPdfFactice;
import ma.ltexpertise.importation.ResultatImport;
import ma.ltexpertise.importation.ServiceImport;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API des dossiers, statistiques, comptes et journal, sur des rapports factices importés de bout en bout.
 * Les comptes « gestionnaire », « consultant » et « admin » sont ceux créés au premier démarrage. Paquets de
 * validation en lot réduits à 2 dossiers pour éprouver le découpage avec quelques rapports.
 */
@SpringBootTest(properties = "lt.dossiers.taille-paquet-validation=2")
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class DossierApiIntegrationTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private ServiceImport imports;
    @Autowired
    private UtilisateurRepository utilisateurs;

    // ------------------------------------------------------------------ dossiers

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void rechercheMulticritere_paginee() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(get("/api/dossiers").param("q", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numeroRapport").value(numero))
                .andExpect(jsonPath("$.content[0].assure").value("ASSURE FICTIF"))
                .andExpect(jsonPath("$.content[0].immatriculation").value("00000-A-00"))
                .andExpect(jsonPath("$.content[0].statut").value("PRET_A_VALIDER"))
                .andExpect(jsonPath("$.content[0].zonesChoc[0]").value(4))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/dossiers").param("q", "assure fictif").param("statut", "PRET_A_VALIDER")
                        .param("zone", "4").param("montantMin", "2000").param("montantMax", "3000")
                        .param("du", "2026-10-01").param("au", "2026-10-31").param("sort", "montantIndemnisation,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].numeroRapport", hasItem(numero)));
        mvc.perform(get("/api/dossiers").param("q", numero).param("statut", "VALIDE"))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/dossiers").param("sort", "colonneInconnue,asc")).andExpect(status().isOk());
        mvc.perform(get("/api/dossiers/compteurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PRET_A_VALIDER", greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void ficheComplete_avecControlesEtPdf() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).cabinetMasque());

        mvc.perform(get("/api/dossiers/{numero}", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PRET_A_VALIDER"))
                .andExpect(jsonPath("$.assure.nomPrenom").value("ASSURE FICTIF"))
                .andExpect(jsonPath("$.vehicule.marque").value("MARQUE"))
                .andExpect(jsonPath("$.mainOeuvre.length()").value(7))
                .andExpect(jsonPath("$.fournitures.length()").value(4))
                .andExpect(jsonPath("$.lignesDetail.length()").value(4))
                .andExpect(jsonPath("$.totalGeneral.ttc").value(2320.00))
                .andExpect(jsonPath("$.champsASaisir[0]").value("cabinet"))
                .andExpect(jsonPath("$.importId").isNumber());
        mvc.perform(get("/api/dossiers/{numero}", "INEXISTANT")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void modifierUnChampManquant_recalculeLeStatut_puisValidation() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).cabinetMasque());
        JsonNode dossier = lire(get("/api/dossiers/{numero}", numero));

        ObjectNode modification = modification(dossier);
        modification.put("cabinet", "CABINET SAISI");
        JsonNode modifie = json.readTree(mvc.perform(put("/api/dossiers/{numero}", numero)
                        .contentType(MediaType.APPLICATION_JSON).content(modification.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cabinet").value("CABINET SAISI"))
                .andExpect(jsonPath("$.statut").value("PRET_A_VALIDER"))
                .andExpect(jsonPath("$.champsASaisir.length()").value(0))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        // Version périmée : modification concurrente refusée
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                        .content(modification.toString()))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/dossiers/{numero}/validation", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"))
                .andExpect(jsonPath("$.validePar").value("gestionnaire"));

        mvc.perform(get("/api/dossiers/{numero}/historique", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].action", hasItem("IMPORT")))
                .andExpect(jsonPath("$[?(@.action == 'MODIFICATION')].champ", hasItem("cabinet")))
                .andExpect(jsonPath("$[?(@.action == 'MODIFICATION')].nouvelleValeur", hasItem("CABINET SAISI")))
                .andExpect(jsonPath("$[*].action", hasItem("VALIDATION")));
        assertThat(modifie.get("version").asInt()).isGreaterThan(dossier.get("version").asInt());
    }

    /** Vétusté HT ou TTC : elle fixe le calcul de l'indemnisation ; jamais les deux à la fois. */
    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void vetusteHt_changeLeCalculDeLIndemnisation() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));
        JsonNode dossier = lire(get("/api/dossiers/{numero}", numero));
        assertThat(dossier.get("vetusteTtc").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(dossier.get("vetusteHt").isNull()).isTrue();

        ObjectNode lesDeux = modification(dossier);
        lesDeux.put("vetusteHt", 0);
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                        .content(lesDeux.toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("soit TTC, soit HT")));

        // Total général HT 1 933,33 − vétusté HT 0 ≠ 2 320 imprimé : le contrôle d'indemnisation échoue
        ObjectNode ht = modification(dossier);
        ht.putNull("vetusteTtc");
        ht.put("vetusteHt", 0);
        JsonNode modifie = lire(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                .content(ht.toString()));
        assertThat(modifie.get("vetusteHt").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(modifie.get("vetusteTtc").isNull()).isTrue();
        assertThat(modifie.get("statut").asText()).isEqualTo("A_VERIFIER");
        JsonNode indemnisation = null;
        for (JsonNode c : modifie.get("controles")) {
            if (c.get("code").asText().equals("INDEMNISATION")) {
                indemnisation = c;
            }
        }
        assertThat(indemnisation).isNotNull();
        assertThat(indemnisation.get("libelle").asText()).isEqualTo("Total général HT − vétusté HT = indemnisation");
        assertThat(indemnisation.get("attendu").decimalValue()).isEqualByComparingTo("1933.33");

        // Retour à la vétusté TTC : le dossier redevient prêt à valider
        ObjectNode ttc = modification(modifie);
        ttc.putNull("vetusteHt");
        ttc.put("vetusteTtc", 0);
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                        .content(ttc.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PRET_A_VALIDER"));
        mvc.perform(get("/api/dossiers/{numero}/historique", numero))
                .andExpect(jsonPath("$[?(@.action == 'MODIFICATION')].champ", hasItem("vetusteHt")));
    }

    /** Épaviste retenu (réforme économique ou technique) : modifiable sur un rapport de réforme seulement. */
    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void reformeEtEpavisteRetenu_seSaisissentSurUnRapportDeReforme() throws Exception {
        String classique = importer(RapportPdfFactice.numero(nouveauNumero()));
        ObjectNode refusee = modification(lire(get("/api/dossiers/{numero}", classique)));
        refusee.put("typeReforme", "TECHNIQUE");
        mvc.perform(put("/api/dossiers/{numero}", classique).contentType(MediaType.APPLICATION_JSON)
                        .content(refusee.toString()))
                .andExpect(status().isConflict());

        String reforme = importer(RapportPdfFactice.numero(nouveauNumero()).titre("RAPPORT D'EXPERTISE REFORME"));
        JsonNode dossier = lire(get("/api/dossiers/{numero}", reforme));
        assertThat(dossier.get("natureRapport").asText()).isEqualTo("REFORME");
        assertThat(dossier.get("typeReforme").isNull()).isTrue();

        ObjectNode modification = modification(dossier);
        modification.put("typeReforme", "ECONOMIQUE");
        modification.put("epavisteNom", "EPAVISTE TEST");
        modification.put("epavisteVille", "CASABLANCA");
        mvc.perform(put("/api/dossiers/{numero}", reforme).contentType(MediaType.APPLICATION_JSON)
                        .content(modification.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeReforme").value("ECONOMIQUE"))
                .andExpect(jsonPath("$.epavisteNom").value("EPAVISTE TEST"))
                .andExpect(jsonPath("$.epavisteVille").value("CASABLANCA"));
        mvc.perform(get("/api/dossiers/{numero}/historique", reforme))
                .andExpect(jsonPath("$[?(@.champ == 'typeReforme')].nouvelleValeur", hasItem("ECONOMIQUE")));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void p3_unDossierEnAnomalieNeSeValidePas() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).indemnisation("9.99"));

        mvc.perform(post("/api/dossiers/{numero}/validation", numero))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("pas prêt à valider")));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void prixUnitaireFaux_bloqueLaValidation_puisCorrigeALaMain() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).prixFaux("200.00"));

        JsonNode dossier = lire(get("/api/dossiers/{numero}", numero));
        assertThat(dossier.get("statut").asText()).isEqualTo("A_VERIFIER");
        // La liste des dossiers donne les libellés des anomalies (contrôles bloquants en échec) de la fiche
        List<String> attendus = new java.util.ArrayList<>();
        dossier.get("controles").forEach(c -> {
            if (!c.get("conforme").asBoolean() && c.get("bloquant").asBoolean()) {
                attendus.add(c.get("libelle").asText());
            }
        });
        assertThat(attendus).isNotEmpty();
        JsonNode resume = lire(get("/api/dossiers").param("q", numero)).get("content").get(0);
        assertThat(resume.get("anomalies").asInt()).isEqualTo(attendus.size());
        assertThat(resume.get("libellesAnomalies")).extracting(JsonNode::asText).containsExactlyElementsOf(attendus);
        JsonNode ligne = dossier.get("lignesDetail").get(1);
        assertThat(ligne.get("designation").asText()).isEqualTo("PP ET ING");
        assertThat(ligne.get("prixCoherent").asBoolean()).isFalse();
        assertThat(ligne.get("prixCalcule").decimalValue()).isEqualByComparingTo("613.33");
        assertThat(dossier.get("lignesDetail").get(0).get("prixCoherent").asBoolean()).isTrue();
        mvc.perform(post("/api/dossiers/{numero}/validation", numero)).andExpect(status().isConflict());

        ObjectNode modification = modification(dossier);
        modification.putArray("prixLignes").addObject().put("ordre", ligne.get("ordre").asInt()).put("prixUnitaire", "613.33");
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                        .content(modification.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("PRET_A_VALIDER"))
                .andExpect(jsonPath("$.lignesDetail[1].prixUnitaire").value(613.33))
                .andExpect(jsonPath("$.lignesDetail[1].prixCoherent").value(true));
        mvc.perform(get("/api/dossiers/{numero}/historique", numero))
                .andExpect(jsonPath("$[?(@.action == 'MODIFICATION')].champ", hasItem("ligne 2 (PP ET ING) : prix unitaire")));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void validationEnLot_valideLesPrets_etListeLesAutres() throws Exception {
        String pret1 = importer(RapportPdfFactice.numero(nouveauNumero()));
        String pret2 = importer(RapportPdfFactice.numero(nouveauNumero()));
        String enAnomalie = importer(RapportPdfFactice.numero(nouveauNumero()).indemnisation("9.99"));

        mvc.perform(post("/api/dossiers/validation-groupee").contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().set("numeros", json.valueToTree(
                                List.of(pret1, pret2, enAnomalie, "INCONNU-1"))).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valides").value(2))
                .andExpect(jsonPath("$.paquets").value(2))
                .andExpect(jsonPath("$.ignores.length()").value(2))
                .andExpect(jsonPath("$.ignores[?(@.numeroRapport == '" + enAnomalie + "')].motif",
                        hasItem(org.hamcrest.Matchers.containsString("contrôle bloquant"))));

        mvc.perform(get("/api/dossiers/{numero}", pret1)).andExpect(jsonPath("$.statut").value("VALIDE"));
        mvc.perform(get("/api/dossiers/{numero}", pret2)).andExpect(jsonPath("$.validePar").value("gestionnaire"));
        mvc.perform(get("/api/dossiers/{numero}", enAnomalie)).andExpect(jsonPath("$.statut").value("A_VERIFIER"));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void validationEnLot_tousLesPretsDesFiltres_parPaquets() throws Exception {
        // Préfixe commun : seuls les dossiers de ce test répondent au filtre, même si d'autres tests en ont importé
        String prefixe = "LOT-" + System.nanoTime() % 1_000_000_000L;
        List<String> prets = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            prets.add(importer(RapportPdfFactice.numero(prefixe + "-" + i)));
        }
        String enAnomalie = importer(RapportPdfFactice.numero(prefixe + "-9").indemnisation("9.99"));

        // 5 dossiers prêts en paquets de 2 : 2 + 2 + 1 ; le dossier en anomalie n'est pas visé
        mvc.perform(post("/api/dossiers/validation-groupee").contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().put("tousLesPrets", true).put("q", prefixe).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valides").value(5))
                .andExpect(jsonPath("$.paquets").value(3))
                .andExpect(jsonPath("$.ignores.length()").value(0));
        for (String numero : prets) {
            mvc.perform(get("/api/dossiers/{numero}", numero)).andExpect(jsonPath("$.statut").value("VALIDE"));
        }
        mvc.perform(get("/api/dossiers/{numero}", enAnomalie)).andExpect(jsonPath("$.statut").value("A_VERIFIER"));
        mvc.perform(get("/api/dossiers/{numero}/historique", prets.getLast()))
                .andExpect(jsonPath("$[*].action", hasItem("VALIDATION")));
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void p9_unConsultantNeModifieNiNeValide() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(post("/api/dossiers/{numero}/validation", numero)).andExpect(status().isForbidden());
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/dossiers/validation-groupee").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tousLesPrets\":true}"))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ statistiques

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void tableauDeBordEtAnalyseDesPrix() throws Exception {
        importer(RapportPdfFactice.numero(nouveauNumero()));
        importer(RapportPdfFactice.numero(nouveauNumero()).indemnisation("9.99"));

        mvc.perform(get("/api/statistiques/tableau-de-bord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossiers", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.montantTotal").value(nullValue()))
                .andExpect(jsonPath("$.coutMoyen").value(nullValue()))
                .andExpect(jsonPath("$.mensuel[*].montant", everyItem(nullValue())))
                .andExpect(jsonPath("$.derniersDossiers[*].montantIndemnisation", everyItem(nullValue())))
                .andExpect(jsonPath("$.mensuel.length()").value(12))
                .andExpect(jsonPath("$.parZone[*].cle", hasItem("4")))
                .andExpect(jsonPath("$.parCompagnie[*].cle", hasItem("COMPAGNIE TEST")))
                .andExpect(jsonPath("$.alertes.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.derniersDossiers.length()", greaterThanOrEqualTo(2)));
        mvc.perform(get("/api/statistiques/tableau-de-bord").param("periode", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periode").value("2026-10"))
                .andExpect(jsonPath("$.mensuel[11].mois").value("2026-10"));
        mvc.perform(get("/api/statistiques/prix").param("designation", "pp et").param("marque", "marq")
                        .param("modele", "MODELE").param("typeMine", "modele"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").isNumber())
                .andExpect(jsonPath("$.lignes").isArray());
        mvc.perform(get("/api/statistiques/prix").param("taille", "1").param("tri", "prix,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taille").value(1))
                .andExpect(jsonPath("$.lignes.length()", lessThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void p9_leGestionnaireNeVoitPasLesMontantsDuTableauDeBord() throws Exception {
        importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(get("/api/statistiques/tableau-de-bord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTotal").value(nullValue()))
                .andExpect(jsonPath("$.derniersDossiers[*].montantIndemnisation", everyItem(nullValue())))
                .andExpect(jsonPath("$.alertes").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATEUR")
    void lAdministrateurVoitLesMontantsEtValide() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(get("/api/statistiques/tableau-de-bord"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTotal").isNumber())
                .andExpect(jsonPath("$.derniersDossiers[0].montantIndemnisation").isNumber());
        mvc.perform(post("/api/dossiers/{numero}/validation", numero))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"))
                .andExpect(jsonPath("$.validePar").value("admin"));
    }

    // ------------------------------------------------------------------ annulation et archivage (P6)

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATEUR")
    void lAdministrateurAnnuleUnImportErrone_puisLeBonPdfSeReimporte() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).cabinetMasque());
        long idErrone = lire(get("/api/dossiers/{numero}", numero)).get("id").asLong();

        mvc.perform(post("/api/dossiers/{numero}/annulation", numero).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"  \"}"))
                .andExpect(status().isBadRequest());
        JsonNode annule = lire(post("/api/dossiers/{numero}/annulation", numero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"Mauvais fichier déposé\"}"));
        assertThat(annule.get("statut").asText()).isEqualTo("ANNULE");
        assertThat(annule.get("annulePar").asText()).isEqualTo("admin");
        assertThat(annule.get("motifAnnulation").asText()).isEqualTo("Mauvais fichier déposé");
        assertThat(annule.get("importId").isNumber()).as("le PDF original reste consultable (P1)").isTrue();

        // Hors des listes sans filtre ; visible dans l'onglet des annulés
        mvc.perform(get("/api/dossiers").param("q", numero)).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/dossiers").param("q", numero).param("statut", "ANNULE"))
                .andExpect(jsonPath("$.content[0].numeroRapport").value(numero));
        // Clos : ni modification, ni validation, ni nouvelle annulation
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                .content(modification(annule).toString())).andExpect(status().isConflict());
        mvc.perform(post("/api/dossiers/{numero}/validation", numero)).andExpect(status().isConflict());
        mvc.perform(post("/api/dossiers/{numero}/annulation", numero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"encore\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/dossiers/{numero}/historique", numero))
                .andExpect(jsonPath("$[?(@.action == 'ANNULATION')].nouvelleValeur", hasItem("ANNULE")));

        // Le bon PDF, même n°, est accepté : il devient le dossier actif ; l'import annulé reste consultable par son id
        importer(RapportPdfFactice.numero(numero));
        mvc.perform(get("/api/dossiers/{numero}", numero))
                .andExpect(jsonPath("$.statut").value("PRET_A_VALIDER"));
        mvc.perform(get("/api/dossiers/{numero}", numero).param("id", String.valueOf(idErrone)))
                .andExpect(jsonPath("$.statut").value("ANNULE"));
        mvc.perform(get("/api/dossiers/{numero}/historique", numero).param("id", String.valueOf(idErrone)))
                .andExpect(jsonPath("$[*].action", hasItem("ANNULATION")));
        mvc.perform(post("/api/dossiers/validation-groupee").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeros\":[\"" + numero + "\"]}"))
                .andExpect(jsonPath("$.valides").value(1))
                .andExpect(jsonPath("$.ignores.length()").value(0));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void p9_seulLAdministrateurAnnule_leGestionnaireArchiveUnDossierValide() throws Exception {
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(post("/api/dossiers/{numero}/annulation", numero).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motif\":\"erreur\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/dossiers/{numero}/archivage", numero)).andExpect(status().isConflict());

        mvc.perform(post("/api/dossiers/{numero}/validation", numero)).andExpect(status().isOk());
        JsonNode archive = lire(post("/api/dossiers/{numero}/archivage", numero));
        assertThat(archive.get("statut").asText()).isEqualTo("ARCHIVE");
        assertThat(archive.get("validePar").asText()).as("la trace de la validation reste").isEqualTo("gestionnaire");

        mvc.perform(post("/api/dossiers/{numero}/archivage", numero)).andExpect(status().isConflict());
        mvc.perform(put("/api/dossiers/{numero}", numero).contentType(MediaType.APPLICATION_JSON)
                .content(modification(archive).toString())).andExpect(status().isConflict());
        mvc.perform(get("/api/dossiers").param("q", numero))
                .andExpect(jsonPath("$.content[0].statut").value("ARCHIVE"));
        mvc.perform(get("/api/dossiers/{numero}/historique", numero))
                .andExpect(jsonPath("$[?(@.action == 'ARCHIVAGE')].nouvelleValeur", hasItem("ARCHIVE")));
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void p9_unConsultantNArchivePas() throws Exception {
        mvc.perform(post("/api/dossiers/{numero}/archivage", "X")).andExpect(status().isForbidden());
        mvc.perform(post("/api/dossiers/archivage-groupe").contentType(MediaType.APPLICATION_JSON)
                .content("{\"numeros\":[\"X\"]}")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void archivageEnLot_archiveLesValides_etListeLesAutres() throws Exception {
        String prefixe = nouveauNumero();
        String valide1 = importer(RapportPdfFactice.numero(prefixe + "-A"));
        String valide2 = importer(RapportPdfFactice.numero(prefixe + "-B"));
        String valide3 = importer(RapportPdfFactice.numero(prefixe + "-C"));
        String nonValide = importer(RapportPdfFactice.numero(prefixe + "-D"));
        for (String n : List.of(valide1, valide2, valide3)) {
            mvc.perform(post("/api/dossiers/{numero}/validation", n)).andExpect(status().isOk());
        }

        // Dossiers cochés : un non validé et un inconnu sont ignorés avec leur motif
        mvc.perform(post("/api/dossiers/archivage-groupe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeros\":[\"" + valide1 + "\",\"" + nonValide + "\",\"INCONNU-" + prefixe + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archives").value(1))
                .andExpect(jsonPath("$.ignores.length()").value(2))
                .andExpect(jsonPath("$.ignores[?(@.numeroRapport == '" + nonValide + "')].motif",
                        hasItem("Pas encore validé : seul un dossier validé s'archive.")));

        // Tous les validés répondant au filtre : les 2 restants, par paquets de 2
        mvc.perform(post("/api/dossiers/archivage-groupe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tousLesValides\":true,\"q\":\"" + prefixe + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archives").value(2))
                .andExpect(jsonPath("$.ignores.length()").value(0));
        mvc.perform(get("/api/dossiers").param("q", prefixe).param("statut", "ARCHIVE"))
                .andExpect(jsonPath("$.page.totalElements").value(3));
        mvc.perform(get("/api/dossiers/{numero}/historique", valide2))
                .andExpect(jsonPath("$[*].action", hasItem("ARCHIVAGE")));
    }

    // ------------------------------------------------------------------ comptes et journal

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRATEUR")
    void lAdministrateurGereLesComptesEtConsulteLeJournal() throws Exception {
        String identifiant = "compte" + COMPTEUR.incrementAndGet() + System.nanoTime() % 10000;
        String creation = "{\"identifiant\":\"" + identifiant + "\",\"nom\":\"Test\",\"prenom\":\"Compte\","
                + "\"email\":\"\",\"role\":\"CONSULTATION\",\"motDePasse\":\"MotDePasse-2026\"}";

        JsonNode compte = json.readTree(mvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content(creation))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CONSULTATION"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        mvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(creation))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON)
                        .content(creation.replace("MotDePasse-2026", "court").replace(identifiant, identifiant + "x")))
                .andExpect(status().isBadRequest());

        long id = compte.get("id").asLong();
        mvc.perform(put("/api/utilisateurs/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Test\",\"prenom\":\"Compte\",\"email\":null,\"role\":\"GESTIONNAIRE\",\"actif\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("GESTIONNAIRE"))
                .andExpect(jsonPath("$.actif").value(false));
        mvc.perform(post("/api/utilisateurs/{id}/mot-de-passe", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motDePasse\":\"Nouveau-MotDePasse-1\"}"))
                .andExpect(status().isNoContent());

        long admin = utilisateurs.findByIdentifiant("admin").orElseThrow().getId();
        mvc.perform(put("/api/utilisateurs/{id}", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Administrateur\",\"prenom\":\"LT Expertise\",\"role\":\"CONSULTATION\",\"actif\":true}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/utilisateurs")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].identifiant", hasItem(identifiant)));
        mvc.perform(get("/api/journal-audit").param("action", "GESTION_UTILISATEUR").param("q", identifiant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content[0].utilisateur").value("admin"));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void p9_seulLAdministrateurGereLesComptes() throws Exception {
        mvc.perform(get("/api/utilisateurs")).andExpect(status().isForbidden());
        mvc.perform(get("/api/journal-audit")).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ outils

    private String importer(RapportPdfFactice rapport) {
        ResultatImport r = imports.importer("rapport.pdf", rapport.pdf(),
                utilisateurs.findByIdentifiant("gestionnaire").orElseThrow(), null);
        assertThat(r.issue().estAccepte()).as(r.toString()).isTrue();
        return r.numeroRapport();
    }

    private JsonNode lire(org.springframework.test.web.servlet.RequestBuilder requete) throws Exception {
        return json.readTree(mvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }

    /** Reprend les champs modifiables du dossier lu, comme le formulaire de vérification. */
    private ObjectNode modification(JsonNode d) {
        ObjectNode m = json.createObjectNode();
        for (String champ : new String[]{"version", "typeProcedure", "cabinet", "expert", "compagnie", "refSinistre",
                "dateSinistre", "dateMission", "dateExpertise", "dateAvantReparation", "dateEnCoursTravaux",
                "dateApresTravaux", "dateRapport", "lieuRapport", "natureContrat", "natureEvenement", "zonesChoc",
                "reparateur", "accordReparateur", "assure", "adversaire", "vehicule", "vetusteTtc", "vetusteHt",
                "depannage", "typeReforme", "epavisteNom", "epavisteVille",
                "valeurNeuf", "valeurAssuree", "valeurVenale", "valeurEpave", "tvaRecuperable", "montantIndemnisation",
                "montantLettres", "commentaire"}) {
            m.set(champ, d.get(champ));
        }
        return m;
    }

    private static String nouveauNumero() {
        return "API-" + System.nanoTime() % 1_000_000_000L + "-" + COMPTEUR.incrementAndGet();
    }
}
