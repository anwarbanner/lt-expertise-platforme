package ma.ltexpertise.recherche;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recherche des pièces et des valeurs d'achat / vénales, sur des rapports factices importés de bout en bout : véhicule
 * MARQUE MODELE (type mine MODELE), châssis VF0000000000000000, mis en circulation le 07/10/2022, sinistre du
 * 05/10/2026, pièce de réemploi « PORTE AV D » en zone 4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class RechercheIntegrationTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ServiceImport imports;
    @Autowired
    private UtilisateurRepository utilisateurs;

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void rechercheDesPieces_parTousLesFiltres() throws Exception {
        importer(RapportPdfFactice.numero(nouveauNumero()));

        mvc.perform(get("/api/recherche/pieces").param("piece", "porte av").param("type", "REEMPLOI").param("zone", "4")
                        .param("marque", "marq").param("modele", "modele").param("typeMine", "MODELE")
                        .param("annee", "2022").param("chassis", "vf0000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.lignes[0].designation").value("PORTE AV D"))
                .andExpect(jsonPath("$.lignes[0].typePiece").value("REEMPLOI"))
                .andExpect(jsonPath("$.lignes[0].zoneChoc").value(4))
                .andExpect(jsonPath("$.lignes[0].numeroChassis").value("VF0000000000000000"))
                .andExpect(jsonPath("$.lignes[0].dateMiseCirculation").value("2022-10-07"));
        mvc.perform(get("/api/recherche/pieces").param("piece", "porte av").param("annee", "1990"))
                .andExpect(jsonPath("$.nombre").value(0));
        mvc.perform(get("/api/recherche/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marques", hasItem("MARQUE")))
                .andExpect(jsonPath("$.typesMine", hasItem("MODELE")));
    }

    @Test
    @WithMockUser(username = "gestionnaire", roles = "GESTIONNAIRE")
    void valeursAchatEtVenale_luesDansLeCommentaire_avecAge() throws Exception {
        // Valeur d'achat propre à ce test : seul son dossier répond au filtre
        String achat = (100_000 + COMPTEUR.incrementAndGet() + System.nanoTime() % 50_000) + ".00";
        String numero = importer(RapportPdfFactice.numero(nouveauNumero()).commentaire(
                "- LA VALEUR A NEUF DU VEHICULE EST DE : " + achat + " DH - LA VALEUR VENALE DU VEHICULE EST DE : 65000.00 DH."));

        // 07/10/2022 → 05/10/2026 : 3 ans et 11 mois, soit 47 mois
        mvc.perform(get("/api/recherche/valeurs").param("achatMin", achat).param("achatMax", achat)
                        .param("marque", "MARQUE").param("modele", "mod").param("typeMine", "MODELE")
                        .param("mecDu", "2022-01-01").param("mecAu", "2022-12-31")
                        .param("ageMin", "47").param("ageMax", "47").param("venaleMin", "65000").param("venaleMax", "65000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(1))
                .andExpect(jsonPath("$.lignes[0].numeroRapport").value(numero))
                .andExpect(jsonPath("$.lignes[0].ageMois").value(47))
                .andExpect(jsonPath("$.lignes[0].dateSinistre").value("2026-10-05"))
                .andExpect(jsonPath("$.lignes[0].valeurAchat").value(Double.parseDouble(achat)))
                .andExpect(jsonPath("$.lignes[0].valeurVenale").value(65000.00))
                .andExpect(jsonPath("$.moyenneVenale").value(65000.00));
        mvc.perform(get("/api/recherche/valeurs").param("achatMin", achat).param("achatMax", achat).param("ageMin", "48"))
                .andExpect(jsonPath("$.nombre").value(0));
    }

    @Test
    @WithMockUser(username = "consultant", roles = "CONSULTATION")
    void piecesEtValeurs_pagineesTrieesEtFiltreesParColonne() throws Exception {
        // Préfixe commun : seuls les dossiers de ce test répondent au filtre de la colonne « dossier »
        String prefixe = "PAG" + System.nanoTime() % 1_000_000L;
        String commentaire = "- LA VALEUR A NEUF DU VEHICULE EST DE : %s DH - LA VALEUR VENALE DU VEHICULE EST DE : 1000.00 DH.";
        for (String achat : new String[]{"300000.00", "100000.00", "200000.00"}) {
            importer(RapportPdfFactice.numero(prefixe + "-" + achat.charAt(0)).commentaire(commentaire.formatted(achat)));
        }

        mvc.perform(get("/api/recherche/pieces").param("f_dossier", prefixe).param("taille", "2").param("tri", "dossier,desc"))
                .andExpect(jsonPath("$.nombre").value(3))
                .andExpect(jsonPath("$.taille").value(2))
                .andExpect(jsonPath("$.lignes.length()").value(2))
                .andExpect(jsonPath("$.lignes[0].numeroRapport").value(prefixe + "-3"))
                .andExpect(jsonPath("$.lignes[1].numeroRapport").value(prefixe + "-2"));
        mvc.perform(get("/api/recherche/pieces").param("f_dossier", prefixe).param("taille", "2").param("page", "1")
                        .param("tri", "dossier,desc"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.lignes.length()").value(1))
                .andExpect(jsonPath("$.lignes[0].numeroRapport").value(prefixe + "-1"));
        // Filtres de colonne sur le texte affiché, sans accents ni casse ni espaces
        mvc.perform(get("/api/recherche/pieces").param("f_dossier", prefixe).param("f_type", "reemploi")
                        .param("f_zone", "central/lateral droit").param("f_mec", "07/10/2022").param("f_prix", "0,00"))
                .andExpect(jsonPath("$.nombre").value(3));
        mvc.perform(get("/api/recherche/pieces").param("f_dossier", prefixe).param("f_type", "origine"))
                .andExpect(jsonPath("$.nombre").value(0));

        mvc.perform(get("/api/recherche/valeurs").param("f_dossier", prefixe).param("tri", "achat,desc"))
                .andExpect(jsonPath("$.nombre").value(3))
                .andExpect(jsonPath("$.lignes[*].valeurAchat", contains(300000.0, 200000.0, 100000.0)));
        mvc.perform(get("/api/recherche/valeurs").param("f_dossier", prefixe).param("f_achat", "200 000")
                        .param("f_age", "3 ans 11 mois").param("f_sinistre", "05/10/2026"))
                .andExpect(jsonPath("$.nombre").value(1))
                .andExpect(jsonPath("$.lignes[0].numeroRapport").value(prefixe + "-2"));
    }

    private String importer(RapportPdfFactice rapport) {
        ResultatImport r = imports.importer("rapport.pdf", rapport.pdf(),
                utilisateurs.findByIdentifiant("gestionnaire").orElseThrow(), null);
        assertThat(r.issue().estAccepte()).as(r.toString()).isTrue();
        return r.numeroRapport();
    }

    private static String nouveauNumero() {
        return "RCH-" + System.nanoTime() % 1_000_000_000L + "-" + COMPTEUR.incrementAndGet();
    }
}
