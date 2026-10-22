package ma.ltexpertise;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({ConfigurationTestcontainers.class, ConfigurationMinioTest.class})
class LtExpertiseApplicationTests {

    @Test
    void contexteDemarreAvecPostgreSQLEtMinio() {
    }
}
