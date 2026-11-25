package ma.ltexpertise;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/** MinIO jetable, même image que docker-compose.yml (pgsty/silo, dérivé de MinIO). */
@TestConfiguration(proxyBeanMethods = false)
public class ConfigurationMinioTest {

    public static final DockerImageName IMAGE_MINIO = DockerImageName
            .parse("pgsty/silo:latest")
            .asCompatibleSubstituteFor("minio/minio");

    @Bean
    MinIOContainer minio() {
        return new MinIOContainer(IMAGE_MINIO);
    }

    @Bean
    DynamicPropertyRegistrar proprietesMinio(MinIOContainer minio) {
        return proprietes -> {
            proprietes.add("lt.stockage.url", minio::getS3URL);
            proprietes.add("lt.stockage.cle-acces", minio::getUserName);
            proprietes.add("lt.stockage.cle-secrete", minio::getPassword);
        };
    }
}
