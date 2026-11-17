package ma.ltexpertise.dossier;

import ma.ltexpertise.ConfigurationTestcontainers;
import ma.ltexpertise.commun.ConfigurationJpa;
import ma.ltexpertise.dossier.referentiel.StatutDossier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** V10 : la lettre J lue « Ã » est rétablie dans les dossiers déjà importés, sans toucher au journal d'audit. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConfigurationTestcontainers.class, ConfigurationJpa.class})
class MigrationLettreJTest {

    @Autowired
    private RapportExpertiseRepository rapports;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void retablitLaLettreJDansLesTextesDuDossier() throws IOException {
        RapportExpertise rapport = new RapportExpertise("268202-0J10", StatutDossier.IMPORTE);
        rapport.setCommentaire("DOSSIER ÃEAN");
        long id = rapports.saveAndFlush(rapport).getId();
        jdbc.update("insert into vehicule (rapport_id, numero_serie, immatriculation) values (?, ?, ?)",
                id, "TMAÃB81A0000000", "00000-Ã-00");

        jdbc.execute(new ClassPathResource("db/migration/V10__lettre_j_mal_decodee.sql").getContentAsString(StandardCharsets.UTF_8));

        assertThat(jdbc.queryForObject("select commentaire from rapport_expertise where id = ?", String.class, id))
                .isEqualTo("DOSSIER JEAN");
        assertThat(jdbc.queryForMap("select numero_serie, immatriculation from vehicule where rapport_id = ?", id))
                .containsEntry("numero_serie", "TMAJB81A0000000").containsEntry("immatriculation", "00000-J-00");
    }
}
