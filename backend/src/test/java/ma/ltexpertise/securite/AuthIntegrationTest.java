package ma.ltexpertise.securite;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import ma.ltexpertise.ConfigurationMinioTest;
import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.utilisateur.Role;
import ma.ltexpertise.utilisateur.Utilisateur;
import ma.ltexpertise.utilisateur.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentification de bout en bout (F11, P9, §8) : connexion, jetons, rôles, verrouillage, rafraîchissement. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class AuthIntegrationTest {

    private static final String MOT_DE_PASSE = "Secret-de-test-2026";
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UtilisateurRepository utilisateurs;
    @Autowired
    private PasswordEncoder encodeur;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void connexion_renvoieUnJetonDAccesEtUnCookieDeRafraichissementProtege() throws Exception {
        String identifiant = compte(Role.GESTIONNAIRE);

        MvcResult reponse = mvc.perform(connexion(identifiant, MOT_DE_PASSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expireDans").value(600))
                .andExpect(jsonPath("$.utilisateur.identifiant").value(identifiant))
                .andExpect(jsonPath("$.utilisateur.role").value("GESTIONNAIRE"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth")))
                .andReturn();

        String jeton = lire(reponse).get("jetonAcces").asText();
        mvc.perform(get("/api/auth/moi").header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiant").value(identifiant))
                .andExpect(jsonPath("$.nom").value("Test"));
        assertThat(jdbc.queryForObject("select count(*) from journal_audit where action = 'CONNEXION' "
                + "and identifiant_utilisateur = ?", Integer.class, identifiant)).isEqualTo(1);
    }

    @Test
    void sansJetonOuAvecUnJetonInvalide_l_apiRepond401() throws Exception {
        mvc.perform(get("/api/imports")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/imports").header(HttpHeaders.AUTHORIZATION, "Bearer faux.jeton.signe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void p9_leRoleDuJetonDecideDesDroits() throws Exception {
        String gestionnaire = jetonAcces(compte(Role.GESTIONNAIRE));
        String consultant = jetonAcces(compte(Role.CONSULTATION));
        MockMultipartFile pdf = new MockMultipartFile("fichiers", "a.pdf", MediaType.APPLICATION_PDF_VALUE,
                "pas un pdf".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/imports").file(pdf).header(HttpHeaders.AUTHORIZATION, "Bearer " + consultant))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/imports").file(pdf).header(HttpHeaders.AUTHORIZATION, "Bearer " + gestionnaire))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultats[0].issue").value("FICHIER_INVALIDE"));
        mvc.perform(get("/api/imports").header(HttpHeaders.AUTHORIZATION, "Bearer " + consultant))
                .andExpect(status().isOk());
    }

    @Test
    void unMauvaisMotDePasseOuUnIdentifiantInconnu_memeReponse401() throws Exception {
        String identifiant = compte(Role.CONSULTATION);

        mvc.perform(connexion(identifiant, "mauvais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect."));
        mvc.perform(connexion("inconnu-" + identifiant, "mauvais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiant ou mot de passe incorrect."));
        assertThat(jdbc.queryForObject("select count(*) from journal_audit where action = 'ECHEC_CONNEXION' "
                + "and identifiant_utilisateur = ?", Integer.class, identifiant)).isEqualTo(1);
    }

    @Test
    void cinqEchecs_verrouillentLeCompteMemeAvecLeBonMotDePasse() throws Exception {
        String identifiant = compte(Role.GESTIONNAIRE);
        for (int i = 0; i < 4; i++) {
            mvc.perform(connexion(identifiant, "mauvais")).andExpect(status().isUnauthorized());
        }

        mvc.perform(connexion(identifiant, "mauvais")).andExpect(status().isUnauthorized());

        mvc.perform(connexion(identifiant, MOT_DE_PASSE))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(containsString("Réessayez après")));
        assertThat(utilisateurs.findByIdentifiant(identifiant).orElseThrow().getVerrouilleJusquA()).isNotNull();
    }

    @Test
    void unCompteDesactiveNePeutPasSeConnecter() throws Exception {
        String identifiant = compte(Role.GESTIONNAIRE);
        Utilisateur u = utilisateurs.findByIdentifiant(identifiant).orElseThrow();
        u.setActif(false);
        utilisateurs.save(u);

        mvc.perform(connexion(identifiant, MOT_DE_PASSE)).andExpect(status().isUnauthorized());
    }

    @Test
    void rafraichir_remplaceLeJetonDeRafraichissementQuiNeSertQuUneFois() throws Exception {
        Cookie premier = cookie(mvc.perform(connexion(compte(Role.CONSULTATION), MOT_DE_PASSE)).andReturn());

        MvcResult rafraichi = mvc.perform(post("/api/auth/rafraichir").cookie(premier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jetonAcces").isNotEmpty())
                .andReturn();
        Cookie second = cookie(rafraichi);

        assertThat(second.getValue()).isNotEqualTo(premier.getValue());
        mvc.perform(post("/api/auth/rafraichir").cookie(premier)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/rafraichir").cookie(second)).andExpect(status().isOk());
    }

    @Test
    void rafraichirSansCookie_repond401() throws Exception {
        mvc.perform(post("/api/auth/rafraichir")).andExpect(status().isUnauthorized());
    }

    @Test
    void deconnexion_revoqueLaSessionEtEffaceLeCookie() throws Exception {
        Cookie cookie = cookie(mvc.perform(connexion(compte(Role.CONSULTATION), MOT_DE_PASSE)).andReturn());

        mvc.perform(post("/api/auth/deconnexion").cookie(cookie))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        mvc.perform(post("/api/auth/rafraichir").cookie(cookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void lesComptesInitiauxSontCreesAuPremierDemarrage() throws Exception {
        for (String identifiant : new String[]{"admin", "gestionnaire", "consultant"}) {
            assertThat(utilisateurs.findByIdentifiant(identifiant)).as(identifiant).isPresent();
        }
        assertThat(utilisateurs.findByIdentifiant("admin").orElseThrow().getRole()).isEqualTo(Role.ADMINISTRATEUR);
    }

    // ------------------------------------------------------------------ outils

    private String compte(Role role) {
        String identifiant = "auth-" + role.name().toLowerCase() + "-" + COMPTEUR.incrementAndGet() + "-" + System.nanoTime() % 100000;
        utilisateurs.save(new Utilisateur(identifiant, "Test", "Auth", encodeur.encode(MOT_DE_PASSE), role));
        return identifiant;
    }

    private org.springframework.test.web.servlet.RequestBuilder connexion(String identifiant, String motDePasse)
            throws Exception {
        return post("/api/auth/connexion").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("identifiant", identifiant, "motDePasse", motDePasse)));
    }

    private String jetonAcces(String identifiant) throws Exception {
        return lire(mvc.perform(connexion(identifiant, MOT_DE_PASSE)).andExpect(status().isOk()).andReturn())
                .get("jetonAcces").asText();
    }

    private JsonNode lire(MvcResult reponse) throws Exception {
        return json.readTree(reponse.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static Cookie cookie(MvcResult reponse) {
        String entete = reponse.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(entete).startsWith(AuthController.COOKIE + "=");
        String valeur = entete.substring(AuthController.COOKIE.length() + 1, entete.indexOf(';'));
        return new Cookie(AuthController.COOKIE, valeur);
    }
}
