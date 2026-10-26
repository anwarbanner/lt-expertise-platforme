package ma.ltexpertise.controle;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MontantEnLettresTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "deux mille quatre cents Dirhams | 2400.00",
            "deux mille trois cent vingt Dirhams | 2320.00",
            "sept mille cent soixante Dirhams | 7160.00",
            "six mille cinq cents Dirhams | 6500.00",
            "cent seize mille trois cents Dirhams | 116300.00",
            "cent neuf mille trois cent quatre-vingt treize Dirhams et quatorze centimes | 109393.14",
            "cinquante mille deux cent quatre-vingt sept Dirhams et quarante sept centimes | 50287.47",
            "cinquante mille deux cent seize Dirhams et quatre-vingt treize centimes | 50216.93",
            "quatre-vingts Dirhams | 80.00",
            "soixante-dix-neuf Dirhams | 79.00",
            "un million deux cent mille Dirhams | 1200000.00",
            "Dirhams | 0.00"
    })
    void convertitLeMontantEnChiffres(String lettres, String attendu) {
        assertThat(MontantEnLettres.versNombre(lettres)).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo(attendu));
    }

    @ParameterizedTest
    @ValueSource(strings = {"deux mille XXXX Dirhams", "mille Dirhams et cent cinquante centimes"})
    void refuseUnTexteIncomprehensible(String lettres) {
        assertThat(MontantEnLettres.versNombre(lettres)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void ignoreUnTexteAbsent(String lettres) {
        assertThat(MontantEnLettres.versNombre(lettres)).isEmpty();
    }
}
