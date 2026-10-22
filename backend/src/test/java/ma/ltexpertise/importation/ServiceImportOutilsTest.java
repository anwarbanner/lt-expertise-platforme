package ma.ltexpertise.importation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceImportOutilsTest {

    @ParameterizedTest
    @CsvSource({
            "rapport.pdf, rapport.pdf",
            "'C:\\Users\\poste\\Documents\\rapport 1.pdf', rapport 1.pdf",
            "/home/poste/rapport.pdf, rapport.pdf",
            "'  rapport.pdf  ', rapport.pdf"
    })
    void neGardeQueLeNomDuFichier(String depose, String attendu) {
        assertThat(ServiceImport.nettoyerNom(depose)).isEqualTo(attendu);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nommeUnFichierSansNom(String depose) {
        assertThat(ServiceImport.nettoyerNom(depose)).isEqualTo("sans-nom.pdf");
    }

    @Test
    void tronqueUnNomTropLong() {
        assertThat(ServiceImport.nettoyerNom("a".repeat(300) + ".pdf")).hasSize(255);
    }

    @Test
    void deriveLaCleObjetDeLEmpreinte() {
        String hash = "ab" + "0".repeat(62);

        assertThat(ServiceImport.cleObjet(hash)).isEqualTo("originaux/ab/" + hash + ".pdf");
    }
}
