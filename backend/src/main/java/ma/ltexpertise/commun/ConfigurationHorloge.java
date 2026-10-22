package ma.ltexpertise.commun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Horloge injectée dans les services métier, remplaçable dans les tests. */
@Configuration(proxyBeanMethods = false)
public class ConfigurationHorloge {

    @Bean
    Clock horloge() {
        return Clock.systemDefaultZone();
    }
}
