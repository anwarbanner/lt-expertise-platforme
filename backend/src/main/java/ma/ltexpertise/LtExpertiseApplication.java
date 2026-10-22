package ma.ltexpertise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LtExpertiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(LtExpertiseApplication.class, args);
    }
}
